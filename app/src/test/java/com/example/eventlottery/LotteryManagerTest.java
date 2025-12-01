package com.example.eventlottery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.util.Log;

import com.example.eventlottery.managers.LotteryManager;
import com.example.eventlottery.managers.NotificationManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LotteryManagerTest {

    @Mock private FirebaseFirestore mockDb;
    @Mock private NotificationManager mockNotificationManager;
    @Mock private CollectionReference mockCollectionRef;
    @Mock private DocumentReference mockDocRef;
    @Mock private DocumentSnapshot mockSnapshot;
    @Mock private WriteBatch mockBatch;
    @Mock private Transaction mockTransaction;
    @Mock private Task<DocumentSnapshot> mockDocTask;
    @Mock private Task<Void> mockVoidTask;
    @Mock private Task<String[]> mockTransactionTask; // For replacement result

    private MockedStatic<FirebaseFirestore> mockedFirestoreStatic;
    private MockedStatic<Log> mockedLog;
    private LotteryManager lotteryManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // 1. Mock Log to prevent "Method d not mocked" error
        mockedLog = mockStatic(Log.class);

        // 2. Mock static Firebase instance
        mockedFirestoreStatic = mockStatic(FirebaseFirestore.class);
        mockedFirestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        // 3. Setup standard Firestore mock chain
        when(mockDb.collection("events")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocRef);
        when(mockDb.batch()).thenReturn(mockBatch);

        // 4. Instantiate manager
        lotteryManager = new LotteryManager();

        // 5. Inject mock NotificationManager
        injectPrivateField(lotteryManager, "notificationManager", mockNotificationManager);
    }

    @After
    public void tearDown() {
        mockedFirestoreStatic.close();
        mockedLog.close();
    }

    /**
     * Test 1: Initialize Lottery (Happy Path)
     * Checks if winners are picked and database is updated.
     */
    @Test
    public void testInitializeLottery_Success() {
        // Arrange
        String eventId = "event_123";
        int sampleSize = 2;
        List<String> waitlist = Arrays.asList("user1", "user2", "user3", "user4", "user5");

        // Mock Getting Event
        when(mockDocRef.get()).thenReturn(mockDocTask);
        when(mockDocTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(mockSnapshot);
            return mockDocTask;
        });

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.get("waitlistUsers")).thenReturn(waitlist);
        when(mockSnapshot.getString("eventName")).thenReturn("Test Event");

        // Mock Batch Commit
        when(mockBatch.commit()).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        });

        LotteryManager.LotteryCallback callback = mock(LotteryManager.LotteryCallback.class);

        // Act
        lotteryManager.initializeLottery(eventId, sampleSize, callback);

        // Assert
        verify(mockBatch).update(eq(mockDocRef), eq("lotteryOrder"), any());
        verify(mockBatch).update(eq(mockDocRef), eq("currentDrawIndex"), eq(sampleSize));
        verify(mockNotificationManager, org.mockito.Mockito.times(2))
                .sendWinNotification(anyString(), eq(eventId), anyString());
        verify(callback).onSuccess(anyString());
    }

    /**
     * Test 2: Validation Error
     * Checks if it stops when requesting more winners than entrants.
     */
    @Test
    public void testInitializeLottery_InsufficientEntrants() {
        String eventId = "event_123";
        int sampleSize = 5; // More than list size
        List<String> waitlist = Arrays.asList("user1", "user2");

        when(mockDocRef.get()).thenReturn(mockDocTask);
        when(mockDocTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(mockSnapshot);
            return mockDocTask;
        });
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.get("waitlistUsers")).thenReturn(waitlist);

        LotteryManager.LotteryCallback callback = mock(LotteryManager.LotteryCallback.class);

        lotteryManager.initializeLottery(eventId, sampleSize, callback);

        verify(callback).onError(org.mockito.Mockito.contains("exceeds number of entrants"));
        verify(mockBatch, never()).commit();
    }

    /**
     * Test 3: Replacement Draw
     * Verifies logic to skip users who are NOT in the retry pool.
     */
    @Test
    public void testDrawReplacement_SkipsNonRetryUsers() throws Exception { // <--- Added throws Exception
        // Arrange
        String eventId = "event_replace";
        List<String> lotteryOrder = Arrays.asList("UserA", "UserB", "UserC");
        List<String> retryEntrants = Collections.singletonList("UserC"); // Only C wants retry
        long currentIndex = 1; // Start looking at UserB

        // Mock Transaction
        when(mockDb.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<?> func = invocation.getArgument(0);
            // Manually execute transaction logic
            func.apply(mockTransaction);
            return mockTransactionTask;
        });

        // Mock Snapshot Data inside Transaction
        // This line throws FirebaseFirestoreException, so the method must declare 'throws Exception'
        when(mockTransaction.get(mockDocRef)).thenReturn(mockSnapshot);

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.get("lotteryOrder")).thenReturn(lotteryOrder);
        when(mockSnapshot.get("retryEntrants")).thenReturn(retryEntrants);
        when(mockSnapshot.getLong("currentDrawIndex")).thenReturn(currentIndex);
        when(mockSnapshot.getString("eventName")).thenReturn("Replacement Event");

        // Mock Transaction Success
        when(mockTransactionTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<String[]> listener = invocation.getArgument(0);
            listener.onSuccess(new String[]{"UserC", "Replacement Event"});
            return mockTransactionTask;
        });

        LotteryManager.ReplacementCallback callback = mock(LotteryManager.ReplacementCallback.class);

        // Act
        lotteryManager.drawReplacement(eventId, callback);

        // Assert
        verify(mockTransaction).update(eq(mockDocRef), eq("currentDrawIndex"), eq(3));
        verify(mockNotificationManager).sendReplacementNotification(eq("UserC"), eq(eventId), anyString());
        verify(callback).onSuccess("UserC");
    }


    // Helper for Reflection
    private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
package com.example.eventlottery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.example.eventlottery.managers.LotteryManager;
import com.example.eventlottery.managers.NotificationManager;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class LotteryManagerTest {
    @Mock private FirebaseFirestore mockDb;
    @Mock private NotificationManager mockNotificationManager;
    @Mock private CollectionReference mockCollectionRef;
    @Mock private DocumentReference mockDocRef;
    @Mock private DocumentSnapshot mockSnapshot;
    @Mock private WriteBatch mockBatch;
    @Mock private Task<DocumentSnapshot> mockDocTask;
    @Mock private Task<Void> mockVoidTask;

    private MockedStatic<FirebaseFirestore> mockedFirestoreStatic;
    private MockedStatic<Log> mockedLog; // <--- ADD THIS VARIABLE
    private LotteryManager lotteryManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // 1. Mock Log to prevent "Method d not mocked" error
        mockedLog = mockStatic(Log.class); // <--- INITIALIZE MOCK HERE

        // 2. Mock the static getInstance() so the constructor doesn't crash
        mockedFirestoreStatic = mockStatic(FirebaseFirestore.class);
        mockedFirestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        // 3. Setup standard Firestore mock chain
        when(mockDb.collection("events")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocRef);
        when(mockDb.batch()).thenReturn(mockBatch);

        // 4. Instantiate the manager
        lotteryManager = new LotteryManager();

        // 5. Use Reflection to inject the mock NotificationManager
        injectPrivateField(lotteryManager, "notificationManager", mockNotificationManager);
    }

    @After
    public void tearDown() {
        // Always close static mocks to avoid memory leaks or interfering with other tests
        mockedFirestoreStatic.close();
        mockedLog.close(); // <--- CLOSE MOCK HERE
    }

    @Test
    public void testInitializeLottery_Success() {
        // Arrange
        String eventId = "event123";
        int sampleSize = 1;
        List<String> waitlist = Arrays.asList("user1", "user2");

        // Mock the event fetch
        when(mockDocRef.get()).thenReturn(mockDocTask);
        when(mockDocTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(mockSnapshot);
            return mockDocTask;
        });

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.get("waitlistUsers")).thenReturn(waitlist);
        when(mockSnapshot.getString("eventName")).thenReturn("Test Event");

        // Mock the commit
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
        verify(mockNotificationManager).sendWinNotification(anyString(), eq(eventId), anyString());
        verify(callback).onSuccess(anyString());
    }

    // Helper method for Reflection
    private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
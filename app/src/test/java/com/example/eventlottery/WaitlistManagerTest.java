package com.example.eventlottery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.example.eventlottery.managers.WaitlistManager;

import android.util.Log;

import com.example.eventlottery.WaitlistEntry;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class WaitlistManagerTest {

    @Mock private FirebaseFirestore mockDb;
    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseUser mockUser;

    // Events Collection Mocks
    @Mock private CollectionReference mockEventsCollection;
    @Mock private DocumentReference mockEventDocRef;
    @Mock private CollectionReference mockWaitlistCollection;
    @Mock private DocumentReference mockUserWaitlistDocRef;

    // Users Collection Mocks (ADDED THIS)
    @Mock private CollectionReference mockUsersCollection;
    @Mock private DocumentReference mockUserDocRef;

    @Mock private DocumentSnapshot mockEventSnapshot;
    @Mock private DocumentSnapshot mockUserSnapshot;
    @Mock private WriteBatch mockBatch;
    @Mock private Task<Void> mockVoidTask;
    @Mock private Task<DocumentSnapshot> mockEventDocTask;
    @Mock private Task<DocumentSnapshot> mockUserDocTask;

    private MockedStatic<FirebaseFirestore> mockedFirestoreStatic;
    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<Log> mockedLog;

    private WaitlistManager waitlistManager;
    private final String TEST_USER_ID = "testUser123";
    private final String TEST_EVENT_ID = "event123";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // 1. Reset the Singleton instance
        resetSingleton();

        // 2. Mock Log to prevent "Method d not mocked" error
        mockedLog = mockStatic(Log.class);

        // 3. Mock Static instances for Firestore and Auth
        mockedFirestoreStatic = mockStatic(FirebaseFirestore.class);
        mockedFirestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        mockedAuthStatic = mockStatic(FirebaseAuth.class);
        mockedAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        // 4. Setup Auth Mock
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn(TEST_USER_ID);

        // 5. Setup Firestore Chain Mocks
        when(mockDb.batch()).thenReturn(mockBatch);

        // --- MOCK "events" COLLECTION ---
        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(TEST_EVENT_ID)).thenReturn(mockEventDocRef);
        when(mockEventDocRef.collection("waitlist")).thenReturn(mockWaitlistCollection);
        when(mockWaitlistCollection.document(TEST_USER_ID)).thenReturn(mockUserWaitlistDocRef);

        // --- MOCK "users" COLLECTION (NEW FIX) ---
        // This prevents the NPE when performJoinWaitlist accesses db.collection("users")
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(TEST_USER_ID)).thenReturn(mockUserDocRef);

        // 6. Initialize the Manager
        waitlistManager = WaitlistManager.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        if (mockedFirestoreStatic != null) mockedFirestoreStatic.close();
        if (mockedAuthStatic != null) mockedAuthStatic.close();
        if (mockedLog != null) mockedLog.close();
        resetSingleton();
    }

    @Test
    public void testJoinWaitlist_Success() {
        WaitlistManager.WaitlistCallback callback = mock(WaitlistManager.WaitlistCallback.class);

        // 1. Mock "isUserOnWaitlist" check (Should return FALSE)
        when(mockUserWaitlistDocRef.get()).thenReturn(mockUserDocTask);
        when(mockUserDocTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            when(mockUserSnapshot.exists()).thenReturn(false);
            listener.onSuccess(mockUserSnapshot);
            return mockUserDocTask;
        });

        // 2. Mock "checkWaitlistCapacity" check (Should return TRUE)
        when(mockEventDocRef.get()).thenReturn(mockEventDocTask);
        when(mockEventDocTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            when(mockEventSnapshot.exists()).thenReturn(true);
            when(mockEventSnapshot.getLong("waitlistCapacity")).thenReturn(100L);
            when(mockEventSnapshot.getLong("waitlistCount")).thenReturn(10L);
            listener.onSuccess(mockEventSnapshot);
            return mockEventDocTask;
        });

        // 3. Mock the Batch Commit
        when(mockBatch.commit()).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        });

        // Act
        waitlistManager.joinWaitlist(TEST_EVENT_ID, null, null, callback);

        // Assert
        verify(callback).onSuccess();
        verify(mockBatch).set(eq(mockUserWaitlistDocRef), any(WaitlistEntry.class));
        verify(mockBatch).update(eq(mockEventDocRef), eq("waitlistCount"), any());
    }

    private void resetSingleton() throws Exception {
        Field instance = WaitlistManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }
}
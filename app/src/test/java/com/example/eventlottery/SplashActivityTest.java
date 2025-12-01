package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class SplashActivityTest {

    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private Query mockQuery;
    @Mock private Task<QuerySnapshot> mockQueryTask;
    @Mock private QuerySnapshot mockQuerySnapshot;
    @Mock private DocumentSnapshot mockDocumentSnapshot;
    @Mock private Task<AuthResult> mockAuthTask;

    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<FirebaseFirestore> mockedDbStatic;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 1. Mock Static Firebase Instances
        mockedAuthStatic = mockStatic(FirebaseAuth.class);
        mockedAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        mockedDbStatic = mockStatic(FirebaseFirestore.class);
        mockedDbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        // 2. Mock Common Firestore Chain
        when(mockDb.collection("users")).thenReturn(mockCollection);
        when(mockCollection.whereEqualTo(anyString(), anyString())).thenReturn(mockQuery);

        // 3. FIX: Set a fake Device ID in Robolectric so the app doesn't think ID is null
        Context context = ApplicationProvider.getApplicationContext();
        Settings.Secure.putString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID,
                "test_device_id_123"
        );
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedDbStatic.close();
    }

    @Test
    public void testNewUser_NavigatesToLanding() {
        // Setup: No current user
        when(mockAuth.getCurrentUser()).thenReturn(null);

        // Setup: Prepare Firestore query mocks (Stubbing with 'any()' first)
        when(mockQuery.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // ACT: Start Activity
        SplashActivity activity = Robolectric.buildActivity(SplashActivity.class).create().start().resume().get();

        // Run Handler delay (500ms)
        shadowOf(Looper.getMainLooper()).runToEndOfTasks();

        // CAPTURE: Now that the method was definitely called, capture the listener
        ArgumentCaptor<OnSuccessListener> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask).addOnSuccessListener(successCaptor.capture());

        // TRIGGER: Simulate "Empty Result" (Device not found)
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        successCaptor.getValue().onSuccess(mockQuerySnapshot);

        // ASSERT: Verify Navigation
        Intent expectedIntent = new Intent(activity, LandingActivity.class);
        Intent actualIntent = shadowOf(activity).getNextStartedActivity();

        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    public void testExistingUser_NavigatesToMain() {
        // Setup: No current user in Auth
        when(mockAuth.getCurrentUser()).thenReturn(null);

        // Setup: Prepare Firestore query mocks
        when(mockQuery.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // Setup: Prepare Anonymous Auth mocks
        when(mockAuth.signInAnonymously()).thenReturn(mockAuthTask);
        when(mockAuthTask.addOnCompleteListener(any())).thenReturn(mockAuthTask);
        when(mockAuthTask.isSuccessful()).thenReturn(true);

        // ACT: Start Activity
        SplashActivity activity = Robolectric.buildActivity(SplashActivity.class).create().start().resume().get();
        shadowOf(Looper.getMainLooper()).runToEndOfTasks();

        // CAPTURE 1: Firestore Listener
        ArgumentCaptor<OnSuccessListener> firestoreCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask).addOnSuccessListener(firestoreCaptor.capture());

        // TRIGGER 1: Device Found
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockDocumentSnapshot.getId()).thenReturn("existingUserId");
        when(mockDocumentSnapshot.getString("role")).thenReturn("entrant");
        when(mockQuerySnapshot.getDocuments()).thenReturn(List.of(mockDocumentSnapshot));

        firestoreCaptor.getValue().onSuccess(mockQuerySnapshot);

        // CAPTURE 2: Auth Listener (This runs after Firestore success)
        ArgumentCaptor<OnCompleteListener> authCaptor = ArgumentCaptor.forClass(OnCompleteListener.class);
        verify(mockAuthTask).addOnCompleteListener(authCaptor.capture());

        // TRIGGER 2: Sign In Success
        authCaptor.getValue().onComplete(mockAuthTask);

        // ASSERT: Verify Navigation
        Intent expectedIntent = new Intent(activity, MainActivity.class);
        Intent actualIntent = shadowOf(activity).getNextStartedActivity();

        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}
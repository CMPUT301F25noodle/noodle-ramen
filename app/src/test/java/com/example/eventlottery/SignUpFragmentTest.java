package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.test.core.app.ApplicationProvider;

import com.example.eventlottery.fragments.SignUpFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30) // Use a specific SDK version if needed
public class SignUpFragmentTest {

    private SignUpFragment fragment;
    private FragmentActivity activity;

    // Mocks for Firebase
    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseFirestore mockDb;
    @Mock private FirebaseUser mockUser;
    @Mock private Task<AuthResult> mockAuthTask;
    @Mock private Task<QuerySnapshot> mockQueryTask;
    @Mock private Task<Void> mockVoidTask;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDocument;
    @Mock private Query mockQuery;
    @Mock private QuerySnapshot mockQuerySnapshot;

    // Static mocks must be closed after tests
    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<FirebaseFirestore> mockedDbStatic;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock Static calls to getInstance()
        mockedAuthStatic = mockStatic(FirebaseAuth.class);
        mockedAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        mockedDbStatic = mockStatic(FirebaseFirestore.class);
        mockedDbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        // Setup common mock behaviors
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("testUserId");
        when(mockDb.collection("users")).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);

        // Initialize Fragment in Activity
        activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        fragment = new SignUpFragment();
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.add(fragment, "SignUpFragment");
        tx.commitNow();
    }

    @After
    public void tearDown() {
        // Close static mocks to prevent leaks
        mockedAuthStatic.close();
        mockedDbStatic.close();
    }

    @Test
    public void testEmptyNameValidation() {
        // Get Views
        EditText nameInput = fragment.getView().findViewById(R.id.et_name);
        Button signUpBtn = fragment.getView().findViewById(R.id.btn_sign_up);

        // Set Empty Name
        nameInput.setText("");
        signUpBtn.performClick();

        // Verify Validation Error
        assertEquals("Please enter your name", nameInput.getError());
    }

    @Test
    public void testInvalidEmailValidation() {
        // Get Views
        EditText nameInput = fragment.getView().findViewById(R.id.et_name);
        EditText emailInput = fragment.getView().findViewById(R.id.et_email);
        Button signUpBtn = fragment.getView().findViewById(R.id.btn_sign_up);

        // Set Valid Name, Invalid Email
        nameInput.setText("John Doe");
        emailInput.setText("invalid-email");
        signUpBtn.performClick();

        // Verify Validation Error
        assertEquals("Please enter a valid email address", emailInput.getError());
    }

    @Test
    public void testSignUpFlow_Successful() {
        // 1. Setup Inputs
        EditText nameInput = fragment.getView().findViewById(R.id.et_name);
        EditText emailInput = fragment.getView().findViewById(R.id.et_email);
        EditText phoneInput = fragment.getView().findViewById(R.id.et_phone);
        Button signUpBtn = fragment.getView().findViewById(R.id.btn_sign_up);

        nameInput.setText("Test User");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");

        // 2. Mock "Check Email Exists" logic
        when(mockCollection.whereEqualTo("email", "test@example.com")).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryTask);

        // Mock success listener for query
        ArgumentCaptor<OnSuccessListener> querySuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mockQueryTask.addOnSuccessListener(querySuccessCaptor.capture())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // 3. Mock "Sign In Anonymously"
        when(mockAuth.signInAnonymously()).thenReturn(mockAuthTask);
        ArgumentCaptor<OnCompleteListener> authCompleteCaptor = ArgumentCaptor.forClass(OnCompleteListener.class);
        when(mockAuthTask.addOnCompleteListener(authCompleteCaptor.capture())).thenReturn(mockAuthTask);
        when(mockAuthTask.isSuccessful()).thenReturn(true);
        when(mockAuthTask.getResult()).thenReturn(mock(AuthResult.class)); // or null, usually not used in code

        // 4. Mock "Create User Profile" (Firestore set)
        when(mockDocument.set(any(Map.class))).thenReturn(mockVoidTask);
        ArgumentCaptor<OnSuccessListener> saveSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mockVoidTask.addOnSuccessListener(saveSuccessCaptor.capture())).thenReturn(mockVoidTask);

        // --- PERFORM ACTION ---
        signUpBtn.performClick();

        // --- TRIGGER CALLBACKS MANUALLY ---

        // A. Trigger Email Check Success (Empty snapshot = email does not exist)
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        querySuccessCaptor.getValue().onSuccess(mockQuerySnapshot);

        // B. Trigger Auth Success
        authCompleteCaptor.getValue().onComplete(mockAuthTask);

        // C. Trigger Firestore Save Success
        saveSuccessCaptor.getValue().onSuccess(null);

        // --- VERIFY ---

        // Verify data saved to Firestore matches inputs
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockDocument).set(mapCaptor.capture());

        Map<String, Object> savedData = mapCaptor.getValue();
        assertEquals("Test User", savedData.get("name"));
        assertEquals("test@example.com", savedData.get("email"));
        assertEquals("entrant", savedData.get("role")); // Default role
    }
}
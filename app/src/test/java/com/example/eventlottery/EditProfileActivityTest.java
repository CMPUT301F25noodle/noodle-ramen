package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34) // Use a recent SDK version supported by Robolectric
public class EditProfileActivityTest {

    // 1. Mocks for Firebase
    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseUser mockUser;
    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollectionRef;
    @Mock private DocumentReference mockDocRef;
    @Mock private DocumentSnapshot mockSnapshot;

    // 2. Mocks for Tasks
    @Mock private Task<DocumentSnapshot> mockGetTask;
    @Mock private Task<Void> mockUpdateTask;

    // 3. Static Mocks
    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<FirebaseFirestore> mockedFirestoreStatic;

    private final String TEST_USER_ID = "test_user_123";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedAuthStatic = mockStatic(FirebaseAuth.class);
        mockedAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        mockedFirestoreStatic = mockStatic(FirebaseFirestore.class);
        mockedFirestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn(TEST_USER_ID);

        when(mockDb.collection("users")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocRef);
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedFirestoreStatic.close();
    }

    /**
     * Test 1: Verify that when the activity starts, it loads data from Firestore
     * and populates the EditText fields correctly.
     */
    @Test
    public void testOnCreate_LoadsProfileData() {
        when(mockDocRef.get()).thenReturn(mockGetTask);
        when(mockGetTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            when(mockSnapshot.exists()).thenReturn(true);
            when(mockSnapshot.getString("name")).thenReturn("Existing Name");
            when(mockSnapshot.getString("email")).thenReturn("old@example.com");
            when(mockSnapshot.getString("phone")).thenReturn("555-0000");

            listener.onSuccess(mockSnapshot);
            return mockGetTask;
        });
        when(mockGetTask.addOnFailureListener(any())).thenReturn(mockGetTask);

        try (ActivityScenario<EditProfileActivity> scenario = ActivityScenario.launch(EditProfileActivity.class)) {
            scenario.onActivity(activity -> {
                EditText nameEdit = activity.findViewById(R.id.userNameEditText);
                EditText emailEdit = activity.findViewById(R.id.emailEditText);

                assertEquals("Existing Name", nameEdit.getText().toString());
                assertEquals("old@example.com", emailEdit.getText().toString());
            });
        }
    }

    /**
     * Test 2: Verify that clicking "Done" saves the changes to Firestore
     * and closes the activity with RESULT_OK.
     */
    @Test
    public void testSaveAndClose_UpdatesFirestore() {

        when(mockDocRef.get()).thenReturn(mockGetTask);
        when(mockGetTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            when(mockSnapshot.exists()).thenReturn(true);
            listener.onSuccess(mockSnapshot);
            return mockGetTask;
        });
        when(mockGetTask.addOnFailureListener(any())).thenReturn(mockGetTask);

        when(mockDocRef.update(anyMap())).thenReturn(mockUpdateTask);
        when(mockUpdateTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null); // Trigger success immediately
            return mockUpdateTask;
        });
        when(mockUpdateTask.addOnFailureListener(any())).thenReturn(mockUpdateTask);

        // Act
        try (ActivityScenario<EditProfileActivity> scenario = ActivityScenario.launch(EditProfileActivity.class)) {
            scenario.onActivity(activity -> {
                EditText nameEdit = activity.findViewById(R.id.userNameEditText);
                EditText phoneEdit = activity.findViewById(R.id.phoneEditText);
                Button doneBtn = activity.findViewById(R.id.doneButton);

                nameEdit.setText("New Name");
                phoneEdit.setText("123-4567");

                doneBtn.performClick();


                ShadowActivity shadowActivity = Shadows.shadowOf(activity);
                assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
                assertTrue(activity.isFinishing());
            });

            ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockDocRef).update(mapCaptor.capture());

            Map<String, Object> updatedValues = mapCaptor.getValue();
            assertEquals("New Name", updatedValues.get("name"));
            assertEquals("123-4567", updatedValues.get("phone"));
        }
}}

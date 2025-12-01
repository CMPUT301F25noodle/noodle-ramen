package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.fragments.BrowseFragment;
import com.example.eventlottery.managers.ImageManager;
import com.example.eventlottery.managers.WaitlistManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class BrowseFragmentNavigationTest {

    // Firebase Mocks
    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseUser mockUser;
    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private Task<QuerySnapshot> mockQueryTask;
    @Mock private QuerySnapshot mockQuerySnapshot;
    @Mock private QueryDocumentSnapshot mockEventDoc;

    // Manager Mocks
    @Mock private ImageManager mockImageManager;
    @Mock private WaitlistManager mockWaitlistManager;

    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<FirebaseFirestore> mockedDbStatic;
    private MockedStatic<ImageManager> mockedImageManagerStatic;
    private MockedStatic<WaitlistManager> mockedWaitlistManagerStatic;

    private BrowseFragment fragment;
    private FragmentActivity activity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedAuthStatic = mockStatic(FirebaseAuth.class);
        mockedAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        mockedDbStatic = mockStatic(FirebaseFirestore.class);
        mockedDbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        mockedImageManagerStatic = mockStatic(ImageManager.class);
        mockedImageManagerStatic.when(ImageManager::getInstance).thenReturn(mockImageManager);

        mockedWaitlistManagerStatic = mockStatic(WaitlistManager.class);
        mockedWaitlistManagerStatic.when(WaitlistManager::getInstance).thenReturn(mockWaitlistManager);

        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("test_user_id");

        when(mockDb.collection("events")).thenReturn(mockCollection);
        when(mockCollection.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedDbStatic.close();
        mockedImageManagerStatic.close();
        mockedWaitlistManagerStatic.close();
    }

    @Test
    public void testClickingEventNavigatesToDetails() {

        String testEventId = "event_123_unique";
        when(mockEventDoc.getId()).thenReturn(testEventId);
        when(mockEventDoc.getString("eventName")).thenReturn("Navigation Test Event");
        when(mockEventDoc.getString("price")).thenReturn("20.0");
        when(mockEventDoc.get("waitlistUsers")).thenReturn(new ArrayList<>());
        when(mockEventDoc.getString("organizerName")).thenReturn("Test Org");
        when(mockEventDoc.getString("location")).thenReturn("Test Loc");
        when(mockEventDoc.getString("startDate")).thenReturn("2025-01-01");
        when(mockEventDoc.getString("endDate")).thenReturn("2025-01-01");
        when(mockEventDoc.getString("waitlistLimit")).thenReturn("10");
        when(mockEventDoc.getString("entrantMaxCapacity")).thenReturn("10");
        when(mockEventDoc.getLong("waitlistCount")).thenReturn(0L);
        when(mockEventDoc.getBoolean("geolocationRequired")).thenReturn(false);

        List<QueryDocumentSnapshot> mockDocs = Collections.singletonList(mockEventDoc);

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.iterator()).thenReturn(mockDocs.iterator());
        doAnswer(invocation -> {
            Consumer<QueryDocumentSnapshot> consumer = invocation.getArgument(0);
            mockDocs.forEach(consumer);
            return null;
        }).when(mockQuerySnapshot).forEach(any());


        fragment = new BrowseFragment();
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.add(android.R.id.content, fragment);
        tx.commitNow();

        ArgumentCaptor<OnSuccessListener> captor = ArgumentCaptor.forClass(OnSuccessListener.class);
        org.mockito.Mockito.verify(mockQueryTask, org.mockito.Mockito.atLeast(1)).addOnSuccessListener(captor.capture());
        captor.getValue().onSuccess(mockQuerySnapshot);
        shadowOf(Looper.getMainLooper()).runToEndOfTasks();

        RecyclerView recyclerView = fragment.getView().findViewById(R.id.events_recycler_view);

        recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST));
        recyclerView.layout(0, 0, 1000, 1000);

        View item = recyclerView.getChildAt(0);
        assertNotNull("RecyclerView item should exist", item);

        View goToButton = item.findViewById(R.id.go_to_event_button);
        assertNotNull("Go To Event button should exist", goToButton);

        goToButton.performClick();


        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertNotNull("An intent should have been started", startedIntent);

        assertEquals(EventDetailActivity.class.getName(), startedIntent.getComponent().getClassName());

        assertEquals(testEventId, startedIntent.getStringExtra("eventId"));
    }
}

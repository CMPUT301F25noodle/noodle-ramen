package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.event_classes.EventAdapter;
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
public class BrowseEventsTest {

    // Firebase Mocks
    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseUser mockUser;
    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private Task<QuerySnapshot> mockQueryTask;
    @Mock private QuerySnapshot mockQuerySnapshot;
    @Mock private QueryDocumentSnapshot mockEventDoc1;
    @Mock private QueryDocumentSnapshot mockEventDoc2;

    // Manager Mocks
    @Mock private ImageManager mockImageManager;
    @Mock private WaitlistManager mockWaitlistManager;

    // Static Mock Controllers
    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<FirebaseFirestore> mockedDbStatic;
    private MockedStatic<ImageManager> mockedImageManagerStatic;
    private MockedStatic<WaitlistManager> mockedWaitlistManagerStatic;

    private BrowseFragment fragment;
    private FragmentActivity activity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 1. Mock Static Instances
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
    public void testEventsAreDisplayedInRecyclerView() {

        // Create Mock Event 1 Data
        when(mockEventDoc1.getId()).thenReturn("event_1");
        when(mockEventDoc1.getString("eventName")).thenReturn("Ramen Tasting");
        when(mockEventDoc1.getString("organizerName")).thenReturn("Noodle Shop");
        when(mockEventDoc1.getString("location")).thenReturn("Edmonton");
        when(mockEventDoc1.getString("startDate")).thenReturn("2025-12-01");
        when(mockEventDoc1.getString("endDate")).thenReturn("2025-12-01");
        when(mockEventDoc1.getString("price")).thenReturn("15.0");
        when(mockEventDoc1.getString("waitlistLimit")).thenReturn("10");
        when(mockEventDoc1.getString("entrantMaxCapacity")).thenReturn("5");
        when(mockEventDoc1.getString("description")).thenReturn("Yummy ramen");
        when(mockEventDoc1.getString("eligibility")).thenReturn("All");
        when(mockEventDoc1.getString("category")).thenReturn("Food");
        when(mockEventDoc1.getLong("waitlistCount")).thenReturn(2L);
        when(mockEventDoc1.getBoolean("geolocationRequired")).thenReturn(false);
        when(mockEventDoc1.get("waitlistUsers")).thenReturn(new ArrayList<>());

        // Create Mock Event 2 Data
        when(mockEventDoc2.getId()).thenReturn("event_2");
        when(mockEventDoc2.getString("eventName")).thenReturn("Coding Workshop");
        when(mockEventDoc2.getString("organizerName")).thenReturn("Tech Club");
        when(mockEventDoc2.getString("location")).thenReturn("Online");
        when(mockEventDoc2.getString("startDate")).thenReturn("2025-12-05");
        when(mockEventDoc2.getString("endDate")).thenReturn("2025-12-05");
        when(mockEventDoc2.getString("price")).thenReturn("0");
        when(mockEventDoc2.getString("waitlistLimit")).thenReturn("50");
        when(mockEventDoc2.getString("entrantMaxCapacity")).thenReturn("20");
        when(mockEventDoc2.getString("description")).thenReturn("Learn Java");
        when(mockEventDoc2.getString("eligibility")).thenReturn("Students");
        when(mockEventDoc2.getString("category")).thenReturn("Education");
        when(mockEventDoc2.getLong("waitlistCount")).thenReturn(10L);
        when(mockEventDoc2.getBoolean("geolocationRequired")).thenReturn(true);
        when(mockEventDoc2.get("waitlistUsers")).thenReturn(Collections.singletonList("test_user_id"));

        // Put docs in a list
        List<QueryDocumentSnapshot> mockDocs = new ArrayList<>();
        mockDocs.add(mockEventDoc1);
        mockDocs.add(mockEventDoc2);

        // Mock Iterator
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.iterator()).thenReturn(mockDocs.iterator());

        doAnswer(invocation -> {
            Consumer<QueryDocumentSnapshot> consumer = invocation.getArgument(0);
            for (QueryDocumentSnapshot doc : mockDocs) {
                consumer.accept(doc);
            }
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



        View view = fragment.getView();
        assertNotNull(view);

        RecyclerView recyclerView = view.findViewById(R.id.events_recycler_view);
        assertNotNull(recyclerView);

        recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST));
        recyclerView.layout(0, 0, 1000, 1000);

        EventAdapter adapter = (EventAdapter) recyclerView.getAdapter();
        assertNotNull(adapter);
        assertEquals("RecyclerView should have 2 items", 2, adapter.getItemCount());

        View item1 = recyclerView.getChildAt(0);
        assertNotNull("Item 1 should be visible", item1);
        TextView title1 = item1.findViewById(R.id.event_title);
        TextView price1 = item1.findViewById(R.id.price_text);

        assertEquals("Ramen Tasting", title1.getText().toString());

        assertEquals("$15", price1.getText().toString());

        // Check Item 2
        View item2 = recyclerView.getChildAt(1);
        assertNotNull("Item 2 should be visible", item2);
        TextView title2 = item2.findViewById(R.id.event_title);

        assertEquals("Coding Workshop", title2.getText().toString());
    }
}
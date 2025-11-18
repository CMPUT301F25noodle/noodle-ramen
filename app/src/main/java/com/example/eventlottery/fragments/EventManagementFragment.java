package com.example.eventlottery.fragments;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.example.eventlottery.managers.LotteryManager;
import com.example.eventlottery.utils.CSVDownloadHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class EventManagementFragment extends Fragment implements OnMapReadyCallback{
    private static final String TAG = "EventManagement";
    private static final String ARG_EVENT_ID = "eventId";

    // UI Components
    private TextView tvEventName, tvWaitlistCount, tvPoolSize, tvLotteryStatus;
    private LinearLayout waitlistPreviewContainer, postDrawActionsContainer;
    private Button btnViewAllWaitlist, btnDrawLottery, btnBack;
    private Button btnViewAccepted, btnDownloadAccepted;
    private Button btnViewDeclined, btnDownloadDeclined;
    private Button btnViewRetry, btnDownloadRetry;
    private Button btnDownloadAllWaitlist;
    private CardView mapCard;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private LotteryManager lotteryManager;

    // Data
    private String eventId;
    private String eventName;
    private int poolSize;
    private String lotteryStatus;
    private GoogleMap googleMap;

    // Firestore field names for different entrant categories
    private static final String FIELD_WAITLIST = "waitlistUsers";
    private static final String FIELD_SELECTED = "selectedEntrants";
    private static final String FIELD_ACCEPTED = "acceptedEntrants";
    private static final String FIELD_DECLINED = "declinedEntrants";
    private static final String FIELD_RETRY = "retryEntrants";
    private static final String FIELD_LOST = "lostEntrants";

    public static EventManagementFragment newInstance(String eventId) {
        EventManagementFragment fragment = new EventManagementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }


}

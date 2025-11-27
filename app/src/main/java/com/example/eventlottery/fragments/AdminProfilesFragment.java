package com.example.eventlottery.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.google.firebase.firestore.*;

import java.util.*;

public class AdminProfilesFragment extends Fragment {

    private TextView profilesCount;
    private EditText searchProfiles;
    private LinearLayout profilesList;
    private ProgressBar loadingSpinner;
    private TextView emptyMessage;

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private final List<ProfileData> allProfiles = new ArrayList<>();
    private final List<ProfileData> filteredProfiles = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_profiles, container, false);

        db = FirebaseFirestore.getInstance();

        profilesCount = view.findViewById(R.id.profilesCount);
        searchProfiles = view.findViewById(R.id.searchProfiles);
        profilesList = view.findViewById(R.id.profilesList);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        emptyMessage = view.findViewById(R.id.emptyMessage);

        setupSearch();
        loadProfiles();

        return view;
    }

    private void setupSearch() {
        searchProfiles.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                filterProfiles(s.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    private void loadProfiles() {
        showLoading(true);

        listener = db.collection("users")
                .addSnapshotListener((value, err) -> {
                    if (err != null) {
                        showLoading(false);
                        showError(err.getMessage());
                        return;
                    }

                    allProfiles.clear();

                    for (QueryDocumentSnapshot d : value) {
                        String id = d.getId();
                        String name = d.getString("name");
                        String email = d.getString("email");
                        String date = d.getString("joinedDate");

                        allProfiles.add(new ProfileData(
                                id,
                                name != null ? name : "Unknown Name",
                                email != null ? email : "Unknown Email",
                                date != null ? date : "Unknown Date"
                        ));
                    }

                    profilesCount.setText(String.valueOf(allProfiles.size()));
                    filterProfiles(searchProfiles.getText().toString());

                    showLoading(false);
                });
    }

    private void filterProfiles(String q) {
        filteredProfiles.clear();

        if (q.isEmpty()) {
            filteredProfiles.addAll(allProfiles);
        } else {
            String low = q.toLowerCase();
            for (ProfileData p : allProfiles) {
                if (p.name.toLowerCase().contains(low) ||
                        p.email.toLowerCase().contains(low)) {
                    filteredProfiles.add(p);
                }
            }
        }

        showProfiles();
    }

    private void showProfiles() {
        profilesList.removeAllViews();

        if (filteredProfiles.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        emptyMessage.setVisibility(View.GONE);

        for (ProfileData p : filteredProfiles) addProfileCard(p);
    }

    @SuppressLint("SetTextI18n")
    private void addProfileCard(ProfileData p) {

        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_profile_card, profilesList, false);

        TextView name = card.findViewById(R.id.profileName);
        TextView email = card.findViewById(R.id.profileEmail);
        TextView date = card.findViewById(R.id.joinedDate);
        Button viewBtn = card.findViewById(R.id.viewProfileBtn);
        Button deleteBtn = card.findViewById(R.id.deleteProfileBtn);

        name.setText(p.name);
        email.setText(p.email);
        date.setText("Joined: " + p.joinedDate);

        viewBtn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Open Profile Viewer (not built)", Toast.LENGTH_SHORT).show()
        );

        deleteBtn.setOnClickListener(v -> deleteProfile(p.id));

        profilesList.addView(card);
    }

    private void deleteProfile(String id) {
        db.collection("users").document(id)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Profile deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showLoading(boolean show) {
        loadingSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        profilesList.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }

    private static class ProfileData {
        String id, name, email, joinedDate;

        ProfileData(String i, String n, String e, String d) {
            id = i;
            name = n;
            email = e;
            joinedDate = d;
        }
    }
}

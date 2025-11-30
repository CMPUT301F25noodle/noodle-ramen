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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
/**
 * AdminProfilesFragment provides an interface for administrators to manage user profiles.
 * It allows viewing a list of all registered users, searching/filtering them by name or email,
 * and deleting user profiles from the database.
 */
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
    /**
     * Initializes the fragment's UI components and triggers the data loading process.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
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
    /**
     * Sets up a TextWatcher on the search bar to filter the profile list as the user types.
     */
    private void setupSearch() {
        searchProfiles.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                filterProfiles(s.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });
    }
    /**
     * Connects to Firestore to listen for real-time updates to the "users" collection.
     * Fetches user details like name, email, and join date.
     */
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
                        Long createdAt = d.getLong("createdAt");

                        String formattedDate = "Unknown Date";
                        if (createdAt != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            formattedDate = sdf.format(new Date(createdAt));
                        }

                        allProfiles.add(new ProfileData(
                                id,
                                name != null ? name : "Unknown Name",
                                email != null ? email : "Unknown Email",
                                formattedDate
                        ));
                    }

                    profilesCount.setText(String.valueOf(allProfiles.size()));
                    filterProfiles(searchProfiles.getText().toString());

                    showLoading(false);
                });
    }
    /**
     * Filters the list of profiles based on the search query.
     * Matches against the user's name or email address.
     *
     * @param q The search string entered by the user.
     */
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
    /**
     * Renders the list of filtered profiles into the LinearLayout container.
     * Displays an empty message if no profiles are found.
     */
    private void showProfiles() {
        profilesList.removeAllViews();

        if (filteredProfiles.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        emptyMessage.setVisibility(View.GONE);

        for (ProfileData p : filteredProfiles) addProfileCard(p);
    }
    /**
     * Inflates and populates a single profile card view with data, then adds it to the list.
     * Sets up listeners for the "View Profile" and "Delete Profile" buttons.
     *
     * @param p The ProfileData object containing details to display.
     */
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
    /**
     * Deletes the specified user document from the Firestore "users" collection.
     *
     * @param id The unique document ID of the user to delete.
     */
    private void deleteProfile(String id) {
        db.collection("users").document(id)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Profile deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }
    /**
     * Toggles the visibility of the loading spinner and the profile list.
     *
     * @param show True to show the loading spinner, false to show the list.
     */
    private void showLoading(boolean show) {
        loadingSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        profilesList.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    /**
     * Displays a toast message with an error description.
     *
     * @param msg The error message to display.
     */
    private void showError(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }
    /**
     * Cleans up resources when the fragment view is destroyed, removing the Firestore listener.
     */
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

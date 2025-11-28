package com.example.eventlottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * SplashActivity - Entry point for the app
 * Checks if device ID exists in Firestore
 * If exists: Sign in anonymously and navigate to MainActivity/AdminMainActivity
 * If not: Navigate to SignUpActivity
 * Created for device-based authentication
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Add a small delay to ensure Firebase initializes
        new Handler().postDelayed(() -> {
            // Check if user is already signed in
            if (mAuth.getCurrentUser() != null) {
                // User is already authenticated, fetch their role and navigate
                String userId = mAuth.getCurrentUser().getUid();
                fetchUserRoleAndNavigate(userId);
            } else {
                // Not authenticated, check if device exists in Firestore
                checkDeviceAndNavigate();
            }
        }, 500); // 500ms delay
    }

    /**
     * Gets device ID and checks if it exists in Firestore
     */
    private void checkDeviceAndNavigate() {
        String deviceId = getAndroidDeviceId();

        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Unable to get device ID");
            Toast.makeText(this, "Error: Unable to get device ID", Toast.LENGTH_LONG).show();
            navigateToSignUp();
            return;
        }

        // Query Firestore for this device ID
        db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Device exists - sign in anonymously and navigate to main app
                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String role = queryDocumentSnapshots.getDocuments().get(0).getString("role");
                        signInAndNavigate(userId, role);
                    } else {
                        // New device - navigate to sign up
                        navigateToSignUp();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking device ID: " + e.getMessage(), e);
                    // On error, navigate to sign up to allow user to proceed
                    navigateToSignUp();
                });
    }

    /**
     * Gets the unique Android device ID
     */
    private String getAndroidDeviceId() {
        return android.provider.Settings.Secure.getString(
                getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );
    }

    /**
     * Signs in anonymously and navigates to appropriate main activity
     */
    private void signInAndNavigate(String userId, String role) {
        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save role to SharedPreferences for later use
                        saveUserRole(role);

                        // Navigate based on role
                        if ("admin".equals(role)) {
                            Intent intent = new Intent(SplashActivity.this, AdminMainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        Log.e(TAG, "Anonymous sign-in failed: " + task.getException());
                        Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_LONG).show();
                        // On failure, navigate to sign up
                        navigateToSignUp();
                    }
                });
    }

    /**
     * Saves user role to SharedPreferences
     */
    private void saveUserRole(String role) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userRole", role != null ? role : "entrant");
        editor.apply();
    }

    /**
     * Fetches user role from Firestore and navigates to appropriate activity
     */
    private void fetchUserRoleAndNavigate(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        saveUserRole(role);

                        // Navigate based on role
                        if ("admin".equals(role)) {
                            Intent intent = new Intent(SplashActivity.this, AdminMainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        // User document doesn't exist, treat as new user
                        navigateToSignUp();
                    }
                })
                .addOnFailureListener(e -> {
                    // On error, try to navigate to sign up
                    navigateToSignUp();
                });
    }

    /**
     * Navigates to SignUpActivity for new users
     */
    private void navigateToSignUp() {
        Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

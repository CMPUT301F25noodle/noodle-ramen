package com.example.eventlottery;

import android.content.Intent;
import android.widget.Button;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for LandingActivity (the first page users see)
 * What we're testing:
 * - Does the landing page open without crashing?
 * - Do the buttons work and take you to the right pages?
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LandingActivityTest {

    private LandingActivity activity;

    /**
     * This runs before all tests
     */
    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(LandingActivity.class)
                .create()
                .start()
                .resume()
                .get();
    }

    /**
     * TEST 1: Does the landing page open properly?
     * What we're checking:
     * - The page opens without errors
     * - The login button is on the screen
     * - The signup button is on the screen
     */
    @Test
    public void testActivityLaunches() {
        // Check that the landing page exists
        assertNotNull("Landing page should open", activity);

        // Look for the login button on the screen
        Button loginButton = activity.findViewById(R.id.login_button);
        // Look for the signup button on the screen
        Button signupButton = activity.findViewById(R.id.signup_button);

        // Make sure we found both buttons
        assertNotNull("Login button should be on the screen", loginButton);
        assertNotNull("Signup button should be on the screen", signupButton);
    }

    /**
     * TEST 2: Does the login button take you to the login page?
     * What we're checking:
     * - When I click "Login", does it open the LoginActivity page?
     */
    @Test
    public void testLoginButtonNavigatesToLoginActivity() {
        //  Find the login button on the screen
        Button loginButton = activity.findViewById(R.id.login_button);

        //  Pretend the user clicked the button
        loginButton.performClick();

        // Check which page opened after the click
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        // Make sure the correct page (LoginActivity) opened
        assertNotNull("Clicking login should open a new page", startedIntent);
        String expectedPage = LoginActivity.class.getName();
        String actualPage = startedIntent.getComponent().getClassName();
        assertEquals("Login button should take you to the login page", expectedPage, actualPage);
    }

    /**
     * TEST 3: Does the signup button take you to the signup page?
     * What we're checking:
     * - When I click "Sign Up", does it open the SignUpActivity page?
     */
    @Test
    public void testSignupButtonNavigatesToSignUpActivity() {
        // Find the signup button on the screen
        Button signupButton = activity.findViewById(R.id.signup_button);

        //  Pretend the user clicked the button
        signupButton.performClick();

        //  Check which page opened after the click
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        //  Make sure the correct page opened
        assertNotNull("Clicking signup should open a new page", startedIntent);
        String expectedPage = SignUpActivity.class.getName();
        String actualPage = startedIntent.getComponent().getClassName();
        assertEquals("Signup button should take you to the signup page", expectedPage, actualPage);
    }
}
package com.example.eventlottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo; // Import this
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BrowseFragmentUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // Grant permissions to prevent the system dialog from blocking the test
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION);

    @Test
    public void testSearchInteraction() {
        onView(withId(R.id.search_edit_text))
                .perform(typeText("Ramen Event"), closeSoftKeyboard());

        onView(withId(R.id.search_edit_text))
                .check(matches(withText("Ramen Event")));
    }

    @Test
    public void testFilterInteraction() {
        // 1. Open Filter Dialog
        onView(withId(R.id.fitler_button))
                .perform(click());

        // 2. Select "Sports"
        onView(withId(R.id.cb_sports))
                .check(matches(isDisplayed()))
                .perform(click());

        // 3. Click "Apply Filters"
        // FIX: Use scrollTo() because the button is at the bottom of the ScrollView
        onView(withId(R.id.btn_apply_filters))
                .perform(scrollTo(), click());
    }
}
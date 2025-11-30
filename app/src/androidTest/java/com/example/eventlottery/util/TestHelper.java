package com.example.eventlottery.util;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Utility class for Android UI tests.
 *
 * Provides:
 * - Custom IdlingResource for Firebase/async operations
 * - Custom ViewActions for RecyclerView interactions
 * - Custom Matchers for RecyclerView assertions
 * - Common wait/delay utilities
 */
public class TestHelper {

    // ==================== DELAY CONSTANTS ====================

    /**
     * Standard delay for Firebase operations to complete (2 seconds)
     */
    public static final long FIREBASE_DELAY_MS = 2000;

    /**
     * Standard delay for navigation transitions (1 second)
     */
    public static final long NAVIGATION_DELAY_MS = 1000;

    /**
     * Standard delay for search debouncing (500ms includes 300ms debounce + buffer)
     */
    public static final long SEARCH_DELAY_MS = 500;

    /**
     * Short delay for UI updates (500ms)
     */
    public static final long SHORT_DELAY_MS = 500;

    // ==================== WAIT UTILITIES ====================

    /**
     * Wait for Firebase operations to complete
     */
    public static void waitForFirebase() {
        try {
            Thread.sleep(FIREBASE_DELAY_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for navigation transition to complete
     */
    public static void waitForNavigation() {
        try {
            Thread.sleep(NAVIGATION_DELAY_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for search debouncing and results
     */
    public static void waitForSearch() {
        try {
            Thread.sleep(SEARCH_DELAY_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for short UI updates
     */
    public static void waitShort() {
        try {
            Thread.sleep(SHORT_DELAY_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for custom duration
     *
     * @param milliseconds Duration to wait
     */
    public static void wait(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ==================== RECYCLERVIEW CUSTOM ACTIONS ====================

    /**
     * Custom ViewAction to click a child view within a RecyclerView item
     *
     * @param id The resource ID of the child view to click
     * @return ViewAction that performs the click
     */
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id: " + id;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View childView = view.findViewById(id);
                if (childView != null) {
                    childView.performClick();
                } else {
                    throw new RuntimeException("Child view with id " + id + " not found");
                }
            }
        };
    }

    /**
     * Custom ViewAction to get text from a child view within a RecyclerView item
     *
     * @param id The resource ID of the child view
     * @return ViewAction that can be used to get text
     */
    public static ViewAction getTextFromChildView(final int id, final StringBuilder result) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Get text from child view with id: " + id;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View childView = view.findViewById(id);
                if (childView instanceof android.widget.TextView) {
                    result.append(((android.widget.TextView) childView).getText().toString());
                }
            }
        };
    }

    // ==================== RECYCLERVIEW CUSTOM MATCHERS ====================

    /**
     * Matcher to verify RecyclerView has a specific item count
     *
     * @param expectedCount Expected number of items
     * @return Matcher for RecyclerView item count
     */
    public static Matcher<View> withItemCount(final int expectedCount) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            protected boolean matchesSafely(RecyclerView recyclerView) {
                RecyclerView.Adapter adapter = recyclerView.getAdapter();
                return adapter != null && adapter.getItemCount() == expectedCount;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with item count: " + expectedCount);
            }
        };
    }

    /**
     * Matcher to verify RecyclerView has at least a minimum number of items
     *
     * @param minCount Minimum expected number of items
     * @return Matcher for RecyclerView minimum item count
     */
    public static Matcher<View> withMinimumItemCount(final int minCount) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            protected boolean matchesSafely(RecyclerView recyclerView) {
                RecyclerView.Adapter adapter = recyclerView.getAdapter();
                return adapter != null && adapter.getItemCount() >= minCount;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with at least " + minCount + " items");
            }
        };
    }

    /**
     * Matcher to verify RecyclerView is empty
     *
     * @return Matcher for empty RecyclerView
     */
    public static Matcher<View> isEmpty() {
        return withItemCount(0);
    }

    /**
     * Matcher to verify RecyclerView is not empty
     *
     * @return Matcher for non-empty RecyclerView
     */
    public static Matcher<View> isNotEmpty() {
        return withMinimumItemCount(1);
    }

    /**
     * Matcher to check if a RecyclerView has a specific item at position with text
     *
     * @param position The position in the RecyclerView
     * @param targetViewId The ID of the view to check
     * @param expectedText The expected text
     * @return Matcher for RecyclerView item text
     */
    public static Matcher<View> atPositionWithText(final int position, final int targetViewId, final String expectedText) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            protected boolean matchesSafely(RecyclerView recyclerView) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                if (viewHolder == null) {
                    return false;
                }
                View targetView = viewHolder.itemView.findViewById(targetViewId);
                if (targetView instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) targetView).getText().toString();
                    return text.equals(expectedText);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with item at position " + position + " having text: " + expectedText);
            }
        };
    }

    // ==================== CUSTOM IDLING RESOURCE ====================

    /**
     * Simple IdlingResource for delayed operations
     * Use this to wait for Firebase or other async operations in a more reliable way
     */
    public static class SimpleIdlingResource implements IdlingResource {
        private volatile ResourceCallback callback;
        private boolean isIdle = true;

        @Override
        public String getName() {
            return SimpleIdlingResource.class.getName();
        }

        @Override
        public boolean isIdleNow() {
            return isIdle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            this.callback = callback;
        }

        /**
         * Set the resource to busy state
         */
        public void setIdleState(boolean isIdleNow) {
            isIdle = isIdleNow;
            if (isIdleNow && callback != null) {
                callback.onTransitionToIdle();
            }
        }
    }

    // ==================== TEXT MATCHERS ====================

    /**
     * Matcher to check if text contains a substring (case-insensitive)
     *
     * @param substring The substring to search for
     * @return Matcher for text containing substring
     */
    public static Matcher<String> containsIgnoreCase(final String substring) {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return item != null && item.toLowerCase().contains(substring.toLowerCase());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("String containing (case-insensitive): " + substring);
            }
        };
    }

    // ==================== FIREBASE IDLING RESOURCE ====================

    /**
     * IdlingResource for Firebase operations
     * Note: This is a basic implementation. For production, you should integrate
     * with your actual Firebase callbacks to set idle state properly.
     */
    public static class FirebaseIdlingResource implements IdlingResource {
        private volatile ResourceCallback callback;
        private boolean isIdle = false;

        @Override
        public String getName() {
            return FirebaseIdlingResource.class.getName();
        }

        @Override
        public boolean isIdleNow() {
            return isIdle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            this.callback = callback;
        }

        /**
         * Call this when Firebase operation starts
         */
        public void incrementOperationCount() {
            isIdle = false;
        }

        /**
         * Call this when Firebase operation completes
         */
        public void decrementOperationCount() {
            isIdle = true;
            if (callback != null) {
                callback.onTransitionToIdle();
            }
        }
    }
}

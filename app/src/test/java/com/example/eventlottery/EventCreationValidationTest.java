package com.example.eventlottery;

import com.example.eventlottery.event_classes.Event;
import com.example.eventlottery.event_classes.EventDates;
import com.example.eventlottery.event_classes.EventStatus;
import com.example.eventlottery.event_classes.Location;
import com.example.eventlottery.event_classes.Money;
import com.example.eventlottery.event_classes.Waitlist;

import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for Event Creation validation logic.
 *
 * Tests the business logic and validation rules for creating events,
 * including:
 * - Required field validation
 * - Date validation and formatting
 * - Price validation
 * - Capacity/waitlist validation
 * - Firebase data structure mapping
 * - Category selection
 * - Geolocation requirement handling
 *
 * Note: This test focuses on the business logic that would be used
 * by CreateEventFragment, testing it independently of Android UI.
 */
public class EventCreationValidationTest {

    private EventCreationValidator validator;
    private SimpleDateFormat dateFormat;

    @Before
    public void setUp() {
        validator = new EventCreationValidator();
        dateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
    }


    /**
     * TEST 1: Valid event creation with all required fields
     */
    @Test
    public void testValidEventCreation() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",           // eventName
                "Music Organization",       // organizerName
                "Central Park, NYC",        // location
                "Amazing summer concert",   // description
                "Open to all",              // eligibility
                "15/6/2025",                // startDate
                "15/6/2025",                // endDate
                "25.00",                    // price
                "100",                      // waitlistLimit
                "50",                       // poolSize
                "Music",                    // category
                true                        // geolocationRequired
        );

        assertTrue("Valid event should pass validation", result.isValid());
        assertEquals("Should have no error messages", 0, result.getErrors().size());
    }

    /**
     * TEST 2: Empty event name should fail validation
     */
    @Test
    public void testEmptyEventNameFails() {
        ValidationResult result = validator.validateEventInput(
                "",                         // eventName - EMPTY
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertFalse("Empty event name should fail", result.isValid());
        assertTrue("Should contain event name error",
                result.getErrors().contains("Event name is required"));
    }

    /**
     * TEST 3: Null event name should fail validation
     */
    @Test
    public void testNullEventNameFails() {
        ValidationResult result = validator.validateEventInput(
                null,                       // eventName - NULL
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertFalse("Null event name should fail", result.isValid());
        assertTrue("Should contain event name error",
                result.getErrors().contains("Event name is required"));
    }

    /**
     * TEST 4: Empty organizer name should fail validation
     */
    @Test
    public void testEmptyOrganizerNameFails() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "",                         // organizerName - EMPTY
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertFalse("Empty organizer name should fail", result.isValid());
        assertTrue("Should contain organizer error",
                result.getErrors().contains("Organizer name is required"));
    }

    /**
     * TEST 5: Empty location should fail validation
     */
    @Test
    public void testEmptyLocationFails() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "",                         // location - EMPTY
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertFalse("Empty location should fail", result.isValid());
        assertTrue("Should contain location error",
                result.getErrors().contains("Location is required"));
    }


    /**
     * TEST 6: Empty start date should fail validation
     */
    @Test
    public void testEmptyStartDateFails() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "",                         // startDate - EMPTY
                "15/6/2025",
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertFalse("Empty start date should fail", result.isValid());
        assertTrue("Should contain start date error",
                result.getErrors().contains("Start date is required"));
    }

    /**
     * TEST 7: Empty end date should fail validation
     */
    @Test
    public void testEmptyEndDateFails() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "",                         // endDate - EMPTY
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertFalse("Empty end date should fail", result.isValid());
        assertTrue("Should contain end date error",
                result.getErrors().contains("End date is required"));
    }

    /**
     * TEST 8: End date before start date should fail validation
     */
    @Test
    public void testEndDateBeforeStartDateFails() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "20/6/2025",                // startDate
                "15/6/2025",                // endDate - BEFORE start
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertFalse("End date before start date should fail", result.isValid());
        assertTrue("Should contain date order error",
                result.getErrors().contains("End date must be after or equal to start date"));
    }

    /**
     * TEST 9: Same start and end date should be valid
     */
    @Test
    public void testSameStartAndEndDateValid() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",                // startDate
                "15/6/2025",                // endDate - SAME
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertTrue("Same start and end date should be valid", result.isValid());
    }


    /**
     * TEST 10: Negative price should fail validation
     */
    @Test
    public void testNegativePriceFails() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "-25.00",                   // price - NEGATIVE
                "100",
                "50",
                "Music",
                false
        );

        assertFalse("Negative price should fail", result.isValid());
        assertTrue("Should contain price error",
                result.getErrors().contains("Price cannot be negative"));
    }

    /**
     * TEST 11: Free event (price = 0) should be valid
     */
    @Test
    public void testFreeEventValid() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "0",                        // price - FREE
                "100",
                "50",
                "Music",
                false
        );

        assertTrue("Free event should be valid", result.isValid());
    }

    /**
     * TEST 12: Empty price should default to 0 (free)
     */
    @Test
    public void testEmptyPriceDefaultsToFree() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "",                         // price - EMPTY
                "100",
                "50",
                "Music",
                false
        );

        assertTrue("Empty price should be valid (defaults to free)", result.isValid());
    }


    /**
     * TEST 13: Negative waitlist limit should fail validation
     */
    @Test
    public void testNegativeWaitlistLimitFails() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "-100",                     // waitlistLimit - NEGATIVE
                "50",
                "Music",
                false
        );

        assertFalse("Negative waitlist limit should fail", result.isValid());
        assertTrue("Should contain waitlist error",
                result.getErrors().contains("Waitlist limit cannot be negative"));
    }

    /**
     * TEST 14: Negative pool size should fail validation
     */
    @Test
    public void testNegativePoolSizeFails() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "100",
                "-50",                      // poolSize - NEGATIVE
                "Music",
                false
        );

        assertFalse("Negative pool size should fail", result.isValid());
        assertTrue("Should contain pool size error",
                result.getErrors().contains("Pool size cannot be negative"));
    }

    /**
     * TEST 15: Zero waitlist limit should be valid (unlimited)
     */
    @Test
    public void testUnlimitedWaitlistValid() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "0",                        // waitlistLimit - UNLIMITED
                "50",
                "Music",
                false
        );

        assertTrue("Unlimited waitlist should be valid", result.isValid());
    }


    /**
     * TEST 16: Firebase data structure mapping is correct
     */
    @Test
    public void testFirebaseDataMapping() {
        Map<String, Object> eventData = validator.createFirebaseEventData(
                "Summer Concert",
                "user123",                  // organizerId
                "Music Organization",
                "Central Park, NYC",
                "Amazing summer concert",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "100",
                "50",
                "Music",
                true
        );

        assertNotNull("Event data should not be null", eventData);
        assertEquals("Event name should match", "Summer Concert", eventData.get("eventName"));
        assertEquals("Organizer should match", "user123", eventData.get("organizer"));
        assertEquals("Organizer name should match", "Music Organization", eventData.get("organizerName"));
        assertEquals("Location should match", "Central Park, NYC", eventData.get("location"));
        assertEquals("Description should match", "Amazing summer concert", eventData.get("description"));
        assertEquals("Eligibility should match", "Open to all", eventData.get("eligibility"));
        assertEquals("Start date should match", "15/6/2025", eventData.get("startDate"));
        assertEquals("End date should match", "15/6/2025", eventData.get("endDate"));
        assertEquals("Price should match", "25.00", eventData.get("price"));
        assertEquals("Waitlist limit should match", "100", eventData.get("waitlistLimit"));
        assertEquals("Pool size should match", "50", eventData.get("entrantMaxCapacity"));
        assertEquals("Category should match", "Music", eventData.get("category"));
        assertEquals("Geolocation should match", true, eventData.get("geolocationRequired"));
        assertTrue("Should have createdAt timestamp", eventData.containsKey("createdAt"));
    }

    /**
     * TEST 17: Empty price defaults to "0" in Firebase
     */
    @Test
    public void testEmptyPriceFirebaseMapping() {
        Map<String, Object> eventData = validator.createFirebaseEventData(
                "Summer Concert",
                "user123",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "",                         // price - EMPTY
                "100",
                "50",
                "Music",
                false
        );

        assertEquals("Empty price should map to '0'", "0", eventData.get("price"));
    }

    /**
     * TEST 18: Empty waitlist limit defaults to "0" in Firebase (unlimited)
     */
    @Test
    public void testEmptyWaitlistLimitFirebaseMapping() {
        Map<String, Object> eventData = validator.createFirebaseEventData(
                "Summer Concert",
                "user123",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "",                         // waitlistLimit - EMPTY
                "50",
                "Music",
                false
        );

        assertEquals("Empty waitlist limit should map to '0'", "0", eventData.get("waitlistLimit"));
    }


    /**
     * TEST 19: Valid categories should be accepted
     */
    @Test
    public void testValidCategories() {
        String[] validCategories = {"Sports", "Music", "Arts", "Educational", "Workshops", "Other"};

        for (String category : validCategories) {
            ValidationResult result = validator.validateEventInput(
                    "Test Event",
                    "Test Organizer",
                    "Test Location",
                    "Description",
                    "Eligibility",
                    "15/6/2025",
                    "15/6/2025",
                    "0",
                    "0",
                    "0",
                    category,
                    false
            );

            assertTrue("Category '" + category + "' should be valid", result.isValid());
        }
    }


    /**
     * TEST 20: Empty description should be allowed (optional field)
     */
    @Test
    public void testEmptyDescriptionAllowed() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "",                         // description - EMPTY (optional)
                "Open to all",
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertTrue("Empty description should be allowed", result.isValid());
    }

    /**
     * TEST 21: Empty eligibility should be allowed (optional field)
     */
    @Test
    public void testEmptyEligibilityAllowed() {
        ValidationResult result = validator.validateEventInput(
                "Summer Concert",
                "Music Organization",
                "Central Park, NYC",
                "Description",
                "",                         // eligibility - EMPTY (optional)
                "15/6/2025",
                "15/6/2025",
                "25.00",
                "100",
                "50",
                "Music",
                false
        );

        assertTrue("Empty eligibility should be allowed", result.isValid());
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Inner class for event creation validation logic
     * This simulates the validation that would happen in CreateEventFragment
     */
    private static class EventCreationValidator {

        public ValidationResult validateEventInput(
                String eventName, String organizerName, String location,
                String description, String eligibility,
                String startDate, String endDate,
                String price, String waitlistLimit, String poolSize,
                String category, boolean geolocationRequired) {

            ValidationResult result = new ValidationResult();

            // Required field validation
            if (eventName == null || eventName.trim().isEmpty()) {
                result.addError("Event name is required");
            }

            if (organizerName == null || organizerName.trim().isEmpty()) {
                result.addError("Organizer name is required");
            }

            if (location == null || location.trim().isEmpty()) {
                result.addError("Location is required");
            }

            if (startDate == null || startDate.trim().isEmpty()) {
                result.addError("Start date is required");
            }

            if (endDate == null || endDate.trim().isEmpty()) {
                result.addError("End date is required");
            }

            // Date validation - end date must be >= start date
            if (startDate != null && !startDate.trim().isEmpty() &&
                    endDate != null && !endDate.trim().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                    Date start = sdf.parse(startDate);
                    Date end = sdf.parse(endDate);

                    if (end.before(start)) {
                        result.addError("End date must be after or equal to start date");
                    }
                } catch (ParseException e) {
                    result.addError("Invalid date format");
                }
            }

            // Price validation
            if (price != null && !price.trim().isEmpty()) {
                try {
                    double priceValue = Double.parseDouble(price);
                    if (priceValue < 0) {
                        result.addError("Price cannot be negative");
                    }
                } catch (NumberFormatException e) {
                    result.addError("Invalid price format");
                }
            }

            // Waitlist limit validation
            if (waitlistLimit != null && !waitlistLimit.trim().isEmpty()) {
                try {
                    int waitlistValue = Integer.parseInt(waitlistLimit);
                    if (waitlistValue < 0) {
                        result.addError("Waitlist limit cannot be negative");
                    }
                } catch (NumberFormatException e) {
                    result.addError("Invalid waitlist limit format");
                }
            }

            // Pool size validation
            if (poolSize != null && !poolSize.trim().isEmpty()) {
                try {
                    int poolValue = Integer.parseInt(poolSize);
                    if (poolValue < 0) {
                        result.addError("Pool size cannot be negative");
                    }
                } catch (NumberFormatException e) {
                    result.addError("Invalid pool size format");
                }
            }

            return result;
        }

        public Map<String, Object> createFirebaseEventData(
                String eventName, String organizerId, String organizerName,
                String location, String description, String eligibility,
                String startDate, String endDate, String price,
                String waitlistLimit, String poolSize,
                String category, boolean geolocationRequired) {

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventName", eventName);
            eventData.put("organizer", organizerId);
            eventData.put("organizerName", organizerName);
            eventData.put("location", location);
            eventData.put("description", description);
            eventData.put("eligibility", eligibility);
            eventData.put("startDate", startDate);
            eventData.put("endDate", endDate);
            eventData.put("price", price.isEmpty() ? "0" : price);
            eventData.put("waitlistLimit", waitlistLimit.isEmpty() ? "0" : waitlistLimit);
            eventData.put("entrantMaxCapacity", poolSize.isEmpty() ? "0" : poolSize);
            eventData.put("geolocationRequired", geolocationRequired);
            eventData.put("category", category);
            eventData.put("createdAt", System.currentTimeMillis());

            return eventData;
        }
    }

    /**
     * Validation result holder
     */
    private static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return errors;
        }
    }
}
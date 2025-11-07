package com.example.eventlottery;

import com.example.eventlottery.event_classes.EventDates;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for EventDates class
 * Tests creating and formatting event date ranges
 */
public class EventDatesTest {
    // TEST 1: Create event dates with valid dates
    @Test
    public void testCreateValidEventDates() {
        EventDates dates = new EventDates("10/15/2025", "10/21/2025");

        assertEquals("Start date should be 10/15/2025", "10/15/2025", dates.getStartDate());
        assertEquals("End date should be 10/21/2025", "10/21/2025", dates.getEndDate());
    }
    // TEST 2: Should format dates as range string
    @Test
    public void testToRangeString() {
        EventDates dates = new EventDates("10/15/2025", "10/21/2025");

        assertEquals("Should show date range",
                "10/15/2025 - 10/21/2025",
                dates.toRangeString());
    }
    // TEST 3: Should not allow null start date
    @Test(expected = IllegalArgumentException.class)
    public void testNullStartDateThrowsError() {
        new EventDates(null, "10/21/2025");
    }
    // TEST 4: Should not allow empty start date
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyStartDateThrowsError() {
        new EventDates("", "10/21/2025");
    }
    // TEST 5: Should not allow null end date
    @Test(expected = IllegalArgumentException.class)
    public void testNullEndDateThrowsError() {
        new EventDates("10/15/2025", null);
    }
    // TEST 6: Should not allow empty end date
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyEndDateThrowsError() {
        new EventDates("10/15/2025", "");
    }
    // TEST 7: Two event dates with same dates should be equal
    @Test
    public void testEventDatesEquality() {
        EventDates dates1 = new EventDates("10/15/2025", "10/21/2025");
        EventDates dates2 = new EventDates("10/15/2025", "10/21/2025");

        assertEquals("Event dates with same dates should be equal", dates1, dates2);
    }
    // TEST 8: toString should return range string
    @Test
    public void testToString() {
        EventDates dates = new EventDates("10/15/2025", "10/21/2025");

        assertEquals("toString should return range",
                "10/15/2025 - 10/21/2025",
                dates.toString());
    }
    // TEST 9: Single day event
    @Test
    public void testSingleDayEvent() {
        EventDates dates = new EventDates("10/15/2025", "10/15/2025");

        assertEquals("Should allow same start and end date",
                "10/15/2025 - 10/15/2025",
                dates.toRangeString());
    }
    // TEST 10: Different date formats should work
    @Test
    public void testDifferentDateFormats() {
        EventDates dates = new EventDates("2025-10-15", "2025-10-21");

        assertEquals("Should accept different date format",
                "2025-10-15 - 2025-10-21",
                dates.toRangeString());
    }
}
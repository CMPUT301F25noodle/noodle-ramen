package com.example.eventlottery;

import com.example.eventlottery.event_classes.EventStatus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for EventStatus class
 * Tests event status values and parsing
 */
public class EventStatusTest {
    // TEST 1: Create custom status
    @Test
    public void testCreateCustomStatus() {
        EventStatus status = new EventStatus("Cancelled");

        assertEquals("Display text should be Cancelled", "Cancelled", status.getDisplayText());
    }
    // TEST 2: Should not allow null display text
    @Test(expected = IllegalArgumentException.class)
    public void testNullDisplayTextThrowsError() {
        new EventStatus(null);
    }
    // TEST 3: Should not allow empty display text
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyDisplayTextThrowsError() {
        new EventStatus("");
    }
    // TEST 4: Parse "open" string to OPEN status
    @Test
    public void testFromStringOpen() {
        EventStatus status = EventStatus.fromString("open");

        assertEquals("Should return OPEN status", EventStatus.OPEN, status);
    }
    // TEST 5: Parse "closed" string to CLOSED status
    @Test
    public void testFromStringClosed() {
        EventStatus status = EventStatus.fromString("closed");

        assertEquals("Should return CLOSED status", EventStatus.CLOSED, status);
    }
    // TEST 6: Parse null string to OPEN status
    @Test
    public void testFromStringNullReturnsOpen() {
        EventStatus status = EventStatus.fromString(null);

        assertEquals("Null should default to OPEN", EventStatus.OPEN, status);
    }
    // TEST 7: Two statuses with same text should be equal
    @Test
    public void testStatusEquality() {
        EventStatus status1 = new EventStatus("Test");
        EventStatus status2 = new EventStatus("Test");

        assertEquals("Statuses with same text should be equal", status1, status2);
    }
    // TEST 8: toString should return display text
    @Test
    public void testToString() {
        EventStatus status = new EventStatus("Open");

        assertEquals("toString should return display text", "Open", status.toString());
    }
}
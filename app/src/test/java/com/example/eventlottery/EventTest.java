package com.example.eventlottery;

import com.example.eventlottery.event_classes.Event;
import com.example.eventlottery.event_classes.EventDates;
import com.example.eventlottery.event_classes.EventStatus;
import com.example.eventlottery.event_classes.Location;
import com.example.eventlottery.event_classes.Money;
import com.example.eventlottery.event_classes.Waitlist;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Event class
 * Tests creating events and checking availability
 */
public class EventTest {

    private Location location;
    private EventDates dates;
    private Waitlist waitlist;
    private Money price;

    // Set up common objects before each test
    @Before
    public void setUp() {
        location = new Location("123 Main Street");
        dates = new EventDates("10/15/2025", "10/21/2025");
        waitlist = new Waitlist(50, 100, 30);
        price = new Money(25.0);
    }
    // TEST 1: Create event with all valid information
    @Test
    public void testCreateValidEvent() {
        Event event = new Event("event123", "Concert", "Music Org",
                "A great music concert", "Open to all", location, dates,
                "http://image.url", waitlist, price, EventStatus.OPEN, false);

        assertEquals("ID should be event123", "event123", event.getId());
        assertEquals("Title should be Concert", "Concert", event.getTitle());
        assertEquals("Organization should be Music Org", "Music Org", event.getOrganizationName());
    }
    // TEST 2: Event should not allow null ID
    @Test(expected = IllegalArgumentException.class)
    public void testNullIdThrowsError() {
        new Event(null, "Concert", "Music Org", "Description", "Eligibility",
                location, dates, "url", waitlist, price, EventStatus.OPEN, false);
    }
    // TEST 3: Event should not allow empty title
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyTitleThrowsError() {
        new Event("event123", "", "Music Org", "Description", "Eligibility",
                location, dates, "url", waitlist, price, EventStatus.OPEN, false);
    }
    // TEST 4: Event should not allow null location
    @Test(expected = IllegalArgumentException.class)
    public void testNullLocationThrowsError() {
        new Event("event123", "Concert", "Music Org", "Description", "Eligibility",
                null, dates, "url", waitlist, price, EventStatus.OPEN, false);
    }
    // TEST 5: Open event with spots should be available
    @Test
    public void testEventIsAvailable() {
        Waitlist availableWaitlist = new Waitlist(50, 100, 30);
        Event event = new Event("event123", "Concert", "Music Org", "Description", "Eligibility",
                location, dates, "url", availableWaitlist, price, EventStatus.OPEN, false);

        assertTrue("Open event with spots should be available", event.isAvailable());
    }
    // TEST 6: Closed event should not be available
    @Test
    public void testClosedEventNotAvailable() {
        Event event = new Event("event123", "Concert", "Music Org", "Description", "Eligibility",
                location, dates, "url", waitlist, price, EventStatus.CLOSED, false);

        assertFalse("Closed event should not be available", event.isAvailable());
    }
    // TEST 7: Should detect full waitlist
    @Test
    public void testWaitlistFull() {
        Waitlist fullWaitlist = new Waitlist(100, 100, 30);
        Event event = new Event("event123", "Concert", "Music Org", "Description", "Eligibility",
                location, dates, "url", fullWaitlist, price, EventStatus.OPEN, false);

        assertTrue("Should detect full waitlist", event.isWaitlistFull());
    }
    // TEST 8: Should detect not full waitlist
    @Test
    public void testWaitlistNotFull() {
        Event event = new Event("event123", "Concert", "Music Org", "Description", "Eligibility",
                location, dates, "url", waitlist, price, EventStatus.OPEN, false);

        assertFalse("Should detect not full waitlist", event.isWaitlistFull());
    }
    // TEST 9: Two events with same ID should be equal
    @Test
    public void testEventEquality() {
        Event event1 = new Event("event123", "Concert", "Music Org", "Description", "Eligibility",
                location, dates, "url", waitlist, price, EventStatus.OPEN, false);
        Event event2 = new Event("event123", "Different Title", "Different Org", "Other desc", "Other elig",
                location, dates, "url2", waitlist, price, EventStatus.CLOSED, true);

        assertEquals("Events with same ID should be equal", event1, event2);
    }
}
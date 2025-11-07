package com.example.eventlottery;

import com.example.eventlottery.event_classes.Waitlist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Waitlist class
 * Tests adding people to waitlist and checking if it's full
 */
public class WaitlistTest {
    // TEST 1: Create a waitlist with valid numbers
    @Test
    public void testCreateValidWaitlist() {
        Waitlist waitlist = new Waitlist(50, 100, 30);

        assertEquals("Current count should be 50", 50, waitlist.getCurrentCount());
        assertEquals("Capacity should be 100", 100, waitlist.getCapacity());
        assertEquals("Available spots should be 30", 30, waitlist.getAvailableSpots());
    }
    // TEST 2: Waitlist should detect when it's full
    @Test
    public void testWaitlistIsFull() {
        Waitlist waitlist = new Waitlist(100, 100, 30);

        assertTrue("Waitlist with 100/100 should be full", waitlist.isFull());
    }
    // TEST 3: Waitlist should detect when it's not full
    @Test
    public void testWaitlistIsNotFull() {
        Waitlist waitlist = new Waitlist(50, 100, 30);

        assertFalse("Waitlist with 50/100 should not be full", waitlist.isFull());
    }
    // TEST 4: Should detect when event has available spots
    @Test
    public void testHasAvailableSpots() {
        Waitlist waitlist = new Waitlist(50, 100, 30);

        assertTrue("Should have available spots", waitlist.hasAvailableSpots());
    }
    // TEST 5: Should detect when event has no available spots
    @Test
    public void testNoAvailableSpots() {
        Waitlist waitlist = new Waitlist(50, 100, 0);

        assertFalse("Should have no available spots", waitlist.hasAvailableSpots());
    }
    // TEST 6: Should format waitlist info correctly
    @Test
    public void testWaitlistInfoText() {
        Waitlist waitlist = new Waitlist(50, 100, 30);

        assertEquals("Should show 50/100 on the waitlist",
                "50/100 on the waitlist",
                waitlist.getWaitlistInfoText());
    }
    // TEST 7: Should format available spots correctly
    @Test
    public void testAvailableSpotsText() {
        Waitlist waitlist = new Waitlist(50, 100, 30);

        assertEquals("Should show 30 spots",
                "30 spots",
                waitlist.getAvailableSpotsText());
    }
    // TEST 8: Should not allow negative waitlist count
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeCurrentCountThrowsError() {
        new Waitlist(-1, 100, 30);
    }
    // TEST 9: Should not allow negative capacity
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeCapacityThrowsError() {
        new Waitlist(50, -1, 30);
    }
    // TEST 10: Should not allow negative available spots
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAvailableSpotsThrowsError() {
        new Waitlist(50, 100, -1);
    }
    // TEST 11: Empty waitlist should not be full
    @Test
    public void testEmptyWaitlistNotFull() {
        Waitlist waitlist = new Waitlist(0, 100, 30);

        assertFalse("Empty waitlist should not be full", waitlist.isFull());
    }
    // TEST 12: Two waitlists with same values should be equal
    @Test
    public void testWaitlistEquality() {
        Waitlist waitlist1 = new Waitlist(50, 100, 30);
        Waitlist waitlist2 = new Waitlist(50, 100, 30);

        assertEquals("Waitlists with same values should be equal", waitlist1, waitlist2);
    }
}
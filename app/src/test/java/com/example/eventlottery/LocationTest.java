package com.example.eventlottery;

import com.example.eventlottery.event_classes.Location;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Location class
 * Tests creating and validating event locations
 */
public class LocationTest {
    // TEST 1: Create location with valid address
    @Test
    public void testCreateValidLocation() {
        Location location = new Location("123 Main Street");

        assertEquals("Address should be '123 Main Street'",
                "123 Main Street",
                location.getAddress());
    }
    // TEST 2: Should nit allow null address
    @Test(expected = IllegalArgumentException.class)
    public void testNullAddressThrowsError() {
        new Location(null);
    }
    // TEST 3: Should not allow empty address
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyAddressThrowsError() {
        new Location("");
    }
    // TEST 4: Two locations with same address should be equal
    @Test
    public void testLocationEquality() {
        Location location1 = new Location("123 Main Street");
        Location location2 = new Location("123 Main Street");

        assertEquals("Locations with same address should be equal", location1, location2);
    }
    // TEST 5: toString should return the address
    @Test
    public void testToString() {
        Location location = new Location("456 Park Avenue");

        assertEquals("toString should return address",
                "456 Park Avenue",
                location.toString());
    }
    // TEST 6: Location with long address should work
    @Test
    public void testLongAddress() {
        Location location = new Location("1234 Very Long Street Name, City, Province, Country, Postal Code");

        assertEquals("Should handle long addresses",
                "1234 Very Long Street Name, City, Province, Country, Postal Code",
                location.getAddress());
    }
    // TEST 7: Location with special characters should work
    @Test
    public void testAddressWithSpecialCharacters() {
        Location location = new Location("123 Main St. #456, Apt. 7B");

        assertEquals("Should handle special characters",
                "123 Main St. #456, Apt. 7B",
                location.getAddress());
    }
}
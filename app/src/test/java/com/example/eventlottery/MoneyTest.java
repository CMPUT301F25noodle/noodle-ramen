package com.example.eventlottery;

import com.example.eventlottery.event_classes.Money;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Money class
 * Tests money amounts and formatting
 */
public class MoneyTest {
    // TEST 1: Create money with valid amount
    @Test
    public void testCreateValidMoney() {
        Money money = new Money(50.0);

        assertEquals("Amount should be 50", 50.0, money.getAmount(), 0.01);
    }
    // TEST 2: Free event should show "Free"
    @Test
    public void testFreeEventDisplay() {
        Money money = new Money(0);

        assertEquals("Should show 'Free'", "Free", money.toDisplayString());
    }
    // TEST 3: Paid event should show dollar amount
    @Test
    public void testPaidEventDisplay() {
        Money money = new Money(50.0);

        assertEquals("Should show '$50'", "$50", money.toDisplayString());
    }
    // TEST 4: Should detect free events
    @Test
    public void testIsFree() {
        Money money = new Money(0);

        assertTrue("Money with 0 should be free", money.isFree());
    }
    // TEST 5: Should detect paid events
    @Test
    public void testIsNotFree() {
        Money money = new Money(50.0);

        assertFalse("Money with $50 should not be free", money.isFree());
    }
    // TEST 6: Should NOT allow negative amounts
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAmountThrowsError() {
        new Money(-10.0);
    }
    // TEST 7: Two money objects with same amount should be equal
    @Test
    public void testMoneyEquality() {
        Money money1 = new Money(50.0);
        Money money2 = new Money(50.0);

        assertEquals("Money with same amount should be equal", money1, money2);
    }
    // TEST 8: Large amount should display correctly
    @Test
    public void testLargeAmount() {
        Money money = new Money(1000.0);

        assertEquals("Should show '$1000'", "$1000", money.toDisplayString());
    }
    // TEST 9: Small amount should display correctly
    @Test
    public void testSmallAmount() {
        Money money = new Money(5.0);

        assertEquals("Should show '$5'", "$5", money.toDisplayString());
    }
    // TEST 10: Decimal amounts should work (even if display shows as int)
    @Test
    public void testDecimalAmount() {
        Money money = new Money(25.99);

        assertEquals("Amount should be 25.99", 25.99, money.getAmount(), 0.01);
        assertEquals("Should show '$25'", "$25", money.toDisplayString());
    }
}
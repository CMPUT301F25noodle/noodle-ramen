package com.example.eventlottery.event_classes;

/**
 * Represents a monetary amount with formatting.
 * Immutable value object for type-safe money handling.
 */
public class Money {
    private final double amount;

    /**
     * Creates a Money object.
     * @param amount the monetary value (must be non-negative)
     * @throws IllegalArgumentException if amount is negative
     */
    public Money(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.amount = amount;
    }

    public double getAmount() { return amount; }

    public boolean isFree() { return amount == 0; }

    /**
     * Formats amount for display.
     * @return "Free" if zero, otherwise "$X"
     */
    public String toDisplayString() {
        if (amount == 0) {
            return "Free";
        }
        return "$" + (int) amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Double.compare(money.amount, amount) == 0;
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}

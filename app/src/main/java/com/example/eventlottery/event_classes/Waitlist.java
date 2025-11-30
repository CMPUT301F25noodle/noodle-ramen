package com.example.eventlottery.event_classes;

/**
 * Represents waitlist information for an event.
 * Immutable value object with current count, capacity, and available spots.
 */
public class Waitlist {
    private final int currentCount;
    private final int capacity;
    private final int availableSpots;

    /**
     * Creates a Waitlist.
     * @param currentCount number of people on waitlist
     * @param capacity maximum waitlist capacity
     * @param availableSpots number of event spots available
     * @throws IllegalArgumentException if any value is negative
     */
    public Waitlist(int currentCount, int capacity, int availableSpots) {
        if (currentCount < 0) {
            throw new IllegalArgumentException("Waitlist count cannot be negative");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Waitlist capacity cannot be negative");
        }
        if (availableSpots < 0) {
            throw new IllegalArgumentException("Available spots cannot be negative");
        }
        this.currentCount = currentCount;
        this.capacity = capacity;
        this.availableSpots = availableSpots;
    }

    /** @return current waitlist count */
    public int getCurrentCount() { return currentCount; }

    /** @return waitlist capacity */
    public int getCapacity() { return capacity; }

    /** @return available event spots */
    public int getAvailableSpots() { return availableSpots; }

    /**
     * Checks if waitlist is at capacity.
     * @return true if full (always false for unlimited capacity)
     */
    public boolean isFull() {
        if (capacity == 0) {
            // Unlimited capacity - never full
            return false;
        }
        return currentCount >= capacity;
    }

    /**
     * Checks if event has available spots.
     * @return true if spots available
     */
    public boolean hasAvailableSpots() {
        return availableSpots > 0;
    }

    /**
     * Formats waitlist info for display.
     * @return formatted string (e.g., "50/100 on the waitlist" or "50 on the waitlist" if unlimited)
     */
    public String getWaitlistInfoText() {
        if (capacity == 0) {
            // Unlimited capacity - just show count
            return currentCount + " on the waitlist";
        }
        return currentCount + "/" + capacity + " on the waitlist";
    }

    /**
     * Formats available spots for display.
     * @return formatted string (e.g., "30 spots")
     */
    public String getAvailableSpotsText() {
        return availableSpots + " spots";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Waitlist waitlist = (Waitlist) o;
        return currentCount == waitlist.currentCount &&
               capacity == waitlist.capacity &&
               availableSpots == waitlist.availableSpots;
    }


    @Override
    public String toString() {
        return getWaitlistInfoText();
    }
}

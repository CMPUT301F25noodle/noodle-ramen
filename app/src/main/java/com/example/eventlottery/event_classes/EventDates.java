package com.example.eventlottery.event_classes;

/**
 * Represents an event's date range.
 * Immutable value object storing start and end dates.
 */
public class EventDates {
    private final String startDate;
    private final String endDate;

    /**
     * Creates an EventDates object.
     * @param startDate event start date
     * @param endDate event end date
     * @throws IllegalArgumentException if either date is null or empty
     */
    public EventDates(String startDate, String endDate) {
        if (startDate == null || startDate.isEmpty()) {
            throw new IllegalArgumentException("Start date cannot be null or empty");
        }
        if (endDate == null || endDate.isEmpty()) {
            throw new IllegalArgumentException("End date cannot be null or empty");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** @return start date */
    public String getStartDate() { return startDate; }

    /** @return end date */
    public String getEndDate() { return endDate; }

    /**
     * Formats dates as a range string.
     * @return formatted range (e.g., "10/15/2025 - 10/21/2025")
     */
    public String toRangeString() {
        return startDate + " - " + endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventDates that = (EventDates) o;
        return startDate.equals(that.startDate) && endDate.equals(that.endDate);
    }

    @Override
    public String toString() {
        return toRangeString();
    }
}

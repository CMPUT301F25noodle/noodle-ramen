package com.example.eventlottery.event_classes;

/**
 * Represents the status of an event.
 * Immutable value object with common status constants.
 */
public class EventStatus {
    public static final EventStatus OPEN = new EventStatus("Open");
    public static final EventStatus CLOSED = new EventStatus("Closed");
    public static final EventStatus ENDING_SOON = new EventStatus("2 days left");
    public static final EventStatus FULL = new EventStatus("Full");

    private final String displayText;

    /**
     * Creates an EventStatus.
     * @param displayText text to display to users
     * @throws IllegalArgumentException if displayText is null or empty
     */
    public EventStatus(String displayText) {
        if (displayText == null || displayText.isEmpty()) {
            throw new IllegalArgumentException("Status display text cannot be null or empty");
        }
        this.displayText = displayText;
    }

    /** @return the display text */
    public String getDisplayText() { return displayText; }

    /**
     * Creates EventStatus from a string. Matches common statuses or creates new one.
     * @param status string to parse
     * @return EventStatus object (defaults to OPEN if null/empty)
     */
    public static EventStatus fromString(String status) {
        if (status == null || status.isEmpty()) {
            return OPEN;
        }

        String statusLower = status.toLowerCase();
        if (statusLower.contains("open")) {
            return OPEN;
        } else if (statusLower.contains("closed")) {
            return CLOSED;
        } else if (statusLower.contains("day")) {
            return ENDING_SOON;
        } else if (statusLower.contains("full")) {
            return FULL;
        }

        return new EventStatus(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventStatus that = (EventStatus) o;
        return displayText.equals(that.displayText);
    }

    @Override
    public int hashCode() {
        return displayText.hashCode();
    }

    @Override
    public String toString() {
        return displayText;
    }
}

package com.example.eventlottery.event_classes;

/**
 * ViewModel for presenting Event data in the UI.
 * Wraps an Event and adds presentation logic and user-specific state.
 */
public class EventViewModel {
    /** The wrapped Event object */
    private final Event event;

    /** Whether the current user is on this event's waitlist */
    private final boolean isUserOnWaitlist;

    /** The specific status of the user for this event (WON, LOST, PENDING, REGISTERED) */
    private final String status;

    /**
     * Main Constructor.
     *
     * @param event            the Event to wrap
     * @param isUserOnWaitlist whether user is on waitlist
     * @param status           the user's specific status for this event
     * @throws IllegalArgumentException if event is null
     */
    public EventViewModel(Event event, boolean isUserOnWaitlist, String status) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        this.event = event;
        this.isUserOnWaitlist = isUserOnWaitlist;
        this.status = status;
    }

    /**
     * Secondary constructor for simple cases (defaults to "NONE" status and not on waitlist).
     *
     * @param event the Event to wrap
     */
    public EventViewModel(Event event) {
        // You must provide a default status here if you use this constructor
        this(event, false, "NONE");
    }

    /** @return the user's specific status for this event */
    public String getStatus() { return status; }

    /**
     * Creates a new ViewModel with updated waitlist status, keeping the same event status.
     */
    public EventViewModel withWaitlistStatus(boolean newStatus) {
        return new EventViewModel(this.event, newStatus, this.status);
    }

    // --- DELEGATED METHODS (Pass-through to the Event object) ---

    public Event getEvent() { return event; }
    public boolean isUserOnWaitlist() { return isUserOnWaitlist; }
    public String getId() { return event.getId(); }
    public String getTitle() { return event.getTitle(); }
    public String getOrganizationName() { return event.getOrganizationName(); }
    public String getImageUrl() { return event.getImageUrl(); }

    public String getFormattedOrganization() {
        return "by " + event.getOrganizationName();
    }

    public String getLocationText() {
        // Ensure event.getLocation() isn't null before calling getAddress() to avoid crashes
        if (event.getLocation() == null) return "Unknown Location";
        return event.getLocation().getAddress();
    }

    public String getDateRange() {
        return event.getDates().toRangeString();
    }

    public String getFormattedPrice() {
        return event.getPrice().toDisplayString();
    }

    // NOTE: This might conflict with your new 'status' field if you aren't careful.
    // Consider renaming this to 'getEventGlobalStatusText()' if it's different from the user's status.
    public String getStatusText() {
        return event.getStatus().getDisplayText();
    }

    public String getWaitlistInfo() {
        return event.getWaitlist().getWaitlistInfoText();
    }

    public String getSpotsText() {
        return event.getWaitlist().getAvailableSpotsText();
    }

    public String getJoinButtonText() {
        return isUserOnWaitlist ? "On Waitlist" : "Join Waitlist";
    }

    public boolean isJoinButtonEnabled() {
        return !isUserOnWaitlist && event.isAvailable();
    }

    public boolean shouldShowFullBadge() {
        return event.isWaitlistFull();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventViewModel that = (EventViewModel) o;
        // Also check if the status matches
        return isUserOnWaitlist == that.isUserOnWaitlist &&
                event.equals(that.event) &&
                (status == null ? that.status == null : status.equals(that.status));
    }
    // Temporary constructor for testing UI without a real database
    public EventViewModel(String title, String location, String status) {
        // You'll need to create a dummy Event object here if Event is final
        // Or just store these values temporarily if you are just testing UI
        this.event = new Event(title, location, null, null, null, null, null, null); // Simplified for example
        this.status = status;
        this.isUserOnWaitlist = "PENDING".equals(status);
    }
}
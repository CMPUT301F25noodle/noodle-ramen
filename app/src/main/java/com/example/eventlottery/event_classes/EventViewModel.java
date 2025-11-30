package com.example.eventlottery.event_classes;

/**
 * ViewModel for presenting Event data in the UI.
 * Wraps an Event and adds presentation logic and user-specific state.
 * Handles formatting and UI state without modifying the domain Event.
 */
public class EventViewModel {
    /** The wrapped Event object */
    private final Event event;

    /** Whether the current user is on this event's waitlist */
    private final boolean isUserOnWaitlist;

    /** Base64 encoded image data for this event (null if no image) */
    private final String imageData;

    /**
     * Creates an EventViewModel with user-specific waitlist status and image data.
     *
     * @param event the Event to wrap
     * @param isUserOnWaitlist whether user is on waitlist
     * @param imageData Base64 encoded image data (null if no image)
     * @throws IllegalArgumentException if event is null
     */
    public EventViewModel(Event event, boolean isUserOnWaitlist, String imageData) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        this.event = event;
        this.isUserOnWaitlist = isUserOnWaitlist;
        this.imageData = imageData;
    }

    /**
     * Creates an EventViewModel with user-specific waitlist status.
     *
     * @param event the Event to wrap
     * @param isUserOnWaitlist whether user is on waitlist
     * @throws IllegalArgumentException if event is null
     */
    public EventViewModel(Event event, boolean isUserOnWaitlist) {
        this(event, isUserOnWaitlist, null);
    }

    /**
     * Creates an EventViewModel with user not on waitlist.
     *
     * @param event the Event to wrap
     * @throws IllegalArgumentException if event is null
     */
    public EventViewModel(Event event) {
        this(event, false, null);
    }

    /** @return the underlying Event */
    public Event getEvent() { return event; }

    /** @return true if user is on waitlist */
    public boolean isUserOnWaitlist() { return isUserOnWaitlist; }

    /** @return Base64 encoded image data (null if no image) */
    public String getImageData() { return imageData; }

    /** @return event ID */
    public String getId() { return event.getId(); }

    /** @return event title */
    public String getTitle() { return event.getTitle(); }

    /** @return organization name */
    public String getOrganizationName() { return event.getOrganizationName(); }

    /** @return description */
    public String getDescription() { return event.getDescription(); }

    /** @return eligibility criteria */
    public String getEligibility() { return event.getEligibility(); }

    /** @return image URL */
    public String getImageUrl() { return event.getImageUrl(); }

    /** @return organization name with "by" prefix */
    public String getFormattedOrganization() {
        return "by " + event.getOrganizationName();
    }

    /** @return location address */
    public String getLocationText() {
        return event.getLocation().getAddress();
    }

    /** @return formatted date range */
    public String getDateRange() {
        return event.getDates().toRangeString();
    }

    /** @return formatted price ("$25" or "Free") */
    public String getFormattedPrice() {
        return event.getPrice().toDisplayString();
    }

    /** @return raw price value for filtering */
    public double getPrice() {
        return event.getPrice().getAmount();
    }

    /** @return category for filtering */
    public String getCategory() {
        return event.getCategory();
    }

    /** @return start date for filtering */
    public String getStartDate() {
        return event.getDates().getStartDate();
    }

    /** @return end date for filtering */
    public String getEndDate() {
        return event.getDates().getEndDate();
    }

    /** @return status text */
    public String getStatusText() {
        return event.getStatus().getDisplayText();
    }

    /** @return formatted waitlist info */
    public String getWaitlistInfo() {
        return event.getWaitlist().getWaitlistInfoText();
    }

    /** @return formatted available spots */
    public String getSpotsText() {
        return event.getWaitlist().getAvailableSpotsText();
    }

    /**
     * Gets button text based on user's waitlist status.
     * @return "On Waitlist" if user is on list, "Join Waitlist" otherwise
     */
    public String getJoinButtonText() {
        if (isUserOnWaitlist) {
            return "On Waitlist";
        }
        return "Join Waitlist";
    }

    /**
     * Determines if join button should be enabled.
     * @return true if user can join (not on list and event available)
     */
    public boolean isJoinButtonEnabled() {
        return !isUserOnWaitlist && event.isAvailable();
    }

    /**
     * Checks if full badge should be shown.
     * @return true if waitlist is full
     */
    public boolean shouldShowFullBadge() {
        return event.isWaitlistFull();
    }

    /**
     * Checks if geolocation is required for this event.
     * @return true if geolocation is required
     */
    public boolean isGeolocationRequired() {
        return event.isGeolocationRequired();
    }

    /**
     * Creates a new ViewModel with updated waitlist status.
     * @param newStatus new waitlist membership status
     * @return new EventViewModel with updated status
     */
    public EventViewModel withWaitlistStatus(boolean newStatus) {
        return new EventViewModel(event, newStatus, imageData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventViewModel that = (EventViewModel) o;
        return isUserOnWaitlist == that.isUserOnWaitlist && event.equals(that.event);
    }

}

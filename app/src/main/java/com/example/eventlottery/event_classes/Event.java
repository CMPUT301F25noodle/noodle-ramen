package com.example.eventlottery.event_classes;

/**
 * Represents an event in the lottery system.
 * Immutable domain model with business logic, no UI concerns.
 * Uses value objects for type safety.
 */
public class Event {
    private final String id;
    private final String title;
    private final String organizationName;
    private final String description;
    private final String eligibility;
    private final Location location;
    private final EventDates dates;
    private final String imageUrl;
    private final Waitlist waitlist;
    private final Money price;
    private final EventStatus status;
    private final boolean geolocationRequired;
    private final String category; // Sports, Music, Arts, Educational, Workshops, Other

    /**
     * Creates an Event with all required information.
     *
     * @param id unique event identifier
     * @param title event title
     * @param organizationName name of organizing entity
     * @param description event description
     * @param eligibility eligibility criteria for the event
     * @param location event location
     * @param dates event date range
     * @param imageUrl URL to event image
     * @param waitlist waitlist information
     * @param price event price
     * @param status event status
     * @param geolocationRequired whether geolocation is required for this event
     * @param category event category (Sports, Music, Arts, Educational, Workshops, Other)
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public Event(String id, String title, String organizationName, String description,
                 String eligibility, Location location, EventDates dates, String imageUrl,
                 Waitlist waitlist, Money price, EventStatus status, boolean geolocationRequired,
                 String category) {
        // Validate all inputs
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Event title cannot be null or empty");
        }
        if (organizationName == null || organizationName.isEmpty()) {
            throw new IllegalArgumentException("Organization name cannot be null or empty");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        if (eligibility == null) {
            throw new IllegalArgumentException("Eligibility cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (dates == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        if (waitlist == null) {
            throw new IllegalArgumentException("Waitlist cannot be null");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        // Assign to final fields (immutable)
        this.id = id;
        this.title = title;
        this.organizationName = organizationName;
        this.description = description;
        this.eligibility = eligibility;
        this.location = location;
        this.dates = dates;
        this.imageUrl = imageUrl;
        this.waitlist = waitlist;
        this.price = price;
        this.status = status;
        this.geolocationRequired = geolocationRequired;
        this.category = category != null ? category : "Other"; // Default to "Other" if not specified
    }

    public String getId() { return id; }

    public String getTitle() { return title; }

    public String getOrganizationName() { return organizationName; }

    public String getDescription() { return description; }

    public String getEligibility() { return eligibility; }

    public Location getLocation() { return location; }

    public EventDates getDates() { return dates; }

    public String getImageUrl() { return imageUrl; }

    public Waitlist getWaitlist() { return waitlist; }

    public Money getPrice() { return price; }

    public EventStatus getStatus() { return status; }

    public boolean isGeolocationRequired() { return geolocationRequired; }

    public String getCategory() { return category; }

    /**
     * Checks if event is accepting new signups.
     * @return true if status is OPEN and spots are available
     */
    public boolean isAvailable() {
        return status == EventStatus.OPEN && waitlist.hasAvailableSpots();
    }

    /**
     * Checks if waitlist is full.
     * @return true if at capacity
     */
    public boolean isWaitlistFull() {
        return waitlist.isFull();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id.equals(event.id);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                '}';
    }
}
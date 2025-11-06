package com.example.eventlottery.event_classes;

/**
 * Represents an event location.
 * Immutable value object for type-safe location handling.
 */
public class  Location {
    private final String address;

    /**
     * Creates a Location.
     * @param address the location address
     * @throws IllegalArgumentException if address is null or empty
     */
    public Location(String address) {
        if (address == null || address.isEmpty()) {
            throw new IllegalArgumentException("Location address cannot be null or empty");
        }
        this.address = address;
    }

    /** @return the address */
    public String getAddress() { return address; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return address.equals(location.address);
    }

    @Override
    public String toString() {
        return address;
    }
}

package com.example.eventlottery;

/**
 * Data class to hold filter criteria for events
 */
public class EventFilter {
    private String activityType;  // Sports, Music, Arts, Educational, Workshops, Other
    private String startDate;     // Filter events starting after this date
    private String endDate;       // Filter events ending before this date
    private Double minPrice;      // Minimum price
    private Double maxPrice;      // Maximum price
    private String location;      // Location filter

    public EventFilter() {
        // Default constructor - no filters applied
        this.activityType = null;
        this.startDate = null;
        this.endDate = null;
        this.minPrice = null;
        this.maxPrice = null;
        this.location = null;
    }

    // Getters
    public String getActivityType() { return activityType; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Double getMinPrice() { return minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public String getLocation() { return location; }

    // Setters
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    public void setLocation(String location) { this.location = location; }

    /**
     * Check if any filters are applied
     */
    public boolean hasFilters() {
        return activityType != null ||
               startDate != null ||
               endDate != null ||
               minPrice != null ||
               maxPrice != null ||
               (location != null && !location.trim().isEmpty());
    }

    /**
     * Clear all filters
     */
    public void clear() {
        this.activityType = null;
        this.startDate = null;
        this.endDate = null;
        this.minPrice = null;
        this.maxPrice = null;
        this.location = null;
    }
}

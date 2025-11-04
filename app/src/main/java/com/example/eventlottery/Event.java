package com.example.eventlottery;

public class Event {
    private String id; // Id for easy querying/reference
    private String title;
    private String organizationName;
    private String location;
    private String startDate;
    private String endDate;
    private String imageUrl;
    private int waitlistCount;
    private int waitlistCapacity;
    private int availableSpots;
    private double price;
    private String status; // "Open", "Closed", "2 days left", etc.
    private boolean isUserOnWaitlist;

    // Constructor
    public Event(String id, String title, String organizationName, String location,
                 String startDate, String endDate, String imageUrl,
                 int waitlistCount, int waitlistCapacity, int availableSpots,
                 double price, String status, boolean isUserOnWaitlist) {
        this.id = id;
        this.title = title;
        this.organizationName = organizationName;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.imageUrl = imageUrl;
        this.waitlistCount = waitlistCount;
        this.waitlistCapacity = waitlistCapacity;
        this.availableSpots = availableSpots;
        this.price = price;
        this.status = status;
        this.isUserOnWaitlist = isUserOnWaitlist;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getOrganizationName() { return organizationName; }
    public String getLocation() { return location; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getImageUrl() { return imageUrl; }
    public int getWaitlistCount() { return waitlistCount; }
    public int getWaitlistCapacity() { return waitlistCapacity; }
    public int getAvailableSpots() { return availableSpots; }
    public double getPrice() { return price; }
    public String getStatus() { return status; }
    public boolean isUserOnWaitlist() { return isUserOnWaitlist; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public void setLocation(String location) { this.location = location; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setWaitlistCount(int waitlistCount) { this.waitlistCount = waitlistCount; }
    public void setWaitlistCapacity(int waitlistCapacity) { this.waitlistCapacity = waitlistCapacity; }
    public void setAvailableSpots(int availableSpots) { this.availableSpots = availableSpots; }
    public void setPrice(double price) { this.price = price; }
    public void setStatus(String status) { this.status = status; }
    public void setUserOnWaitlist(boolean userOnWaitlist) { isUserOnWaitlist = userOnWaitlist; }

    // Helper method to get formatted waitlist info
    public String getWaitlistInfo() {
        return waitlistCount + "/" + waitlistCapacity + " on the waitlist";
    }

    // Helper method to get formatted date range
    public String getDateRange() {
        return startDate + " - " + endDate;
    }

    // Helper method to get formatted price
    public String getFormattedPrice() {
        if (price == 0) {
            return "Free";
        }
        return "$" + (int) price;
    }
}
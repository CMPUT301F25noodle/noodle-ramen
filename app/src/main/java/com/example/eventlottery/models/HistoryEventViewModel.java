package com.example.eventlottery.models;

/**
 * Lightweight view model for Event History display
 * Avoids the complexity of full Event/EventViewModel classes
 */
public class HistoryEventViewModel {
    private String eventId;
    private String eventName;
    private String location;
    private String startDate;
    private String endDate;
    private String price;

    public HistoryEventViewModel() {
    }

    public HistoryEventViewModel(String eventId, String eventName, String location,
                                String startDate, String endDate, String price) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.price = price;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    /**
     * Returns formatted price string for display
     * Shows "Free" for null, empty, or "0" prices
     */
    public String getFormattedPrice() {
        if (price == null || price.isEmpty() || price.equals("0")) {
            return "Free";
        }
        return "$" + price;
    }

    /**
     * Returns formatted date range for display
     * Shows single date if start equals end, or "Date TBD" if null
     */
    public String getDateRange() {
        if (startDate != null && endDate != null) {
            if (startDate.equals(endDate)) {
                return startDate;
            }
            return startDate + " - " + endDate;
        }
        return "Date TBD";
    }
}

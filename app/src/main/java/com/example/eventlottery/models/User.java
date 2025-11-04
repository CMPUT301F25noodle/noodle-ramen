package com.example.eventlottery.models;
import java.util.HashMap;
import java.util.Map;


/**
 * user class represents the user
 *
 */

public class User {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean notificationsEnabled;
    private String deviceId;
    private long createdAt;
    private Map <String,Object> waitingLists;
    private Map <String, Object> eventHistory;
    private Map<String, Object> eventsCreated;

    /**
     * Default constructor.
     * Initializes empty HashMaps for waiting lists, event history, and created events.
     */
    public User() {
        this.waitingLists = new HashMap<>();
        this.eventHistory = new HashMap<>();
        this.eventsCreated = new HashMap<>();

    }

    /**
     * Constructs a new User with specified details.
     * Also initializes empty maps for event-related data.
     *
     * @param userId               The unique identifier for the user.
     * @param name                 The user's full name.
     * @param email                The user's email address.
     * @param phone                The user's phone number.
     * @param role                 The user's role (e.g., "entrant", "organizer").
     * @param notificationsEnabled True if notifications are enabled, false otherwise.
     * @param deviceId             The unique ID of the user's device (for notifications).
     * @param createdAt            The timestamp (long) when the user account was created.
     */
    public User (String userId, String name, String email, String phone, String role,
                 boolean notificationsEnabled, String deviceId, long createdAt ) {

        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.notificationsEnabled = notificationsEnabled;
        this.deviceId = deviceId;
        this.createdAt = createdAt;
        this.waitingLists = new HashMap<>();
        this.eventHistory = new HashMap<>();
        this.eventsCreated = new HashMap<>();
    }

    /**
     * getters for all the params
     */
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public String getDeviceId() { return deviceId; }
    public long getCreatedAt() { return createdAt; }
    public Map<String, Object> getWaitingLists() { return waitingLists; }
    public Map<String, Object> getEventHistory() { return eventHistory; }
    public Map<String, Object> getEventsCreated() { return eventsCreated; }

    /**
     * setters for all the params
     */
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(String role) { this.role = role; }
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setWaitingLists(Map<String, Object> waitingLists) {
        this.waitingLists = waitingLists;
    }
    public void setEventHistory(Map<String, Object> eventHistory) {
        this.eventHistory = eventHistory;
    }
    public void setEventsCreated(Map<String, Object> eventsCreated) {
        this.eventsCreated = eventsCreated;
    }


    public boolean isOnWaitlist(String eventId) {
        return waitingLists != null && waitingLists.containsKey(eventId);
    }

    public boolean isEntrant() {
        return "entrant".equals(role);
    }

    public boolean isOrganizer() {
        return "organizer".equals(role);
    }


}
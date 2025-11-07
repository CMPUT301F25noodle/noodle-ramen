package com.example.eventlottery;

/**
 * Represents an entry in an event's waitlist.
 * Stores user information and waitlist metadata.
 */
public class WaitlistEntry {
    private String userId;
    private long joinedAt;
    private String status; // "waiting", "selected", "declined", "cancelled"
    private String deviceId;
    private Double latitude;
    private Double longitude;



    /**
     * Empty constructor r
     *
     */
    public WaitlistEntry() {
        // Empty constructor - required by Firebase
    }

    /**
     * Parameterized constructor for creating waitlist entries.
     *
     * @param userId
     * @param joinedAt
     * @param status
     * @param deviceId
     */
    public WaitlistEntry(String userId, long joinedAt, String status, String deviceId) {
        this.userId = userId;
        this.joinedAt = joinedAt;
        this.status = status;
        this.deviceId = deviceId;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
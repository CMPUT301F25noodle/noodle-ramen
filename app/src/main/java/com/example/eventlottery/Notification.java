package com.example.eventlottery;

import com.example.eventlottery.managers.NotificationManager;

/**
 * represents a single notifcation that is sent to a user
 * models the data strcuture of the notication document that is stored in db
 */
public class Notification {
    private String id;
    private String type;
    private String eventId;
    private String eventName;
    private String message;
    private Long timestamp;
    private Boolean read;
    private Boolean responded;

    public Notification() {
    }

    public Notification(String id, String type, String eventId, String eventName,
                        String message, Long timestamp, Boolean read, Boolean responded) {
        this.id = id;
        this.type = type;
        this.eventId = eventId;
        this.eventName = eventName;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
        this.responded = responded;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getRead() {
        return read != null ? read : false;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Boolean getResponded() {
        return responded != null ? responded : false;
    }

    public void setResponded(Boolean responded) {
        this.responded = responded;
    }

    // Helper methods

    public boolean isRead() {
        return read != null && read;
    }

    public boolean isResponded() {
        return responded != null && responded;
    }

    public boolean isWin() {
        return NotificationManager.TYPE_WIN.equals(type);
    }

    public boolean isLoss() {
        return NotificationManager.TYPE_LOSS.equals(type);
    }

    public boolean isReplacement() {
        return NotificationManager.TYPE_REPLACEMENT.equals(type);
    }

    /**
     * Check if this notification requires a response (Accept/Decline buttons)
     */
    public boolean requiresResponse() {
        return (isWin() || isReplacement() || isLoss()) && !isResponded();
    }


}

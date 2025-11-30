package com.example.eventlottery.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing an image uploaded for an event.
 * Images are compressed and stored directly in Firestore as encoded strings.
 */
public class Image {
    private String imageId;
    private String eventId;
    private String organizerId;
    private String organizerName;
    private String imageData;  // Compressed and encoded image data
    private long uploadedAt;
    private String fileName;

    // Default constructor required for Firestore
    public Image() {
    }

    public Image(String imageId, String eventId, String organizerId, String organizerName,
                 String imageData, long uploadedAt, String fileName) {
        this.imageId = imageId;
        this.eventId = eventId;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.imageData = imageData;
        this.uploadedAt = uploadedAt;
        this.fileName = fileName;
    }

    // Getters
    public String getImageId() {
        return imageId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public String getImageData() {
        return imageData;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public String getFileName() {
        return fileName;
    }

    // Setters
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public void setUploadedAt(long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Converts Image object to a Map for Firestore storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("imageId", imageId);
        map.put("eventId", eventId);
        map.put("organizerId", organizerId);
        map.put("organizerName", organizerName);
        map.put("imageData", imageData);
        map.put("uploadedAt", uploadedAt);
        map.put("fileName", fileName);
        return map;
    }

    /**
     * Creates an Image object from a Firestore document
     */
    public static Image fromMap(String documentId, Map<String, Object> map) {
        return new Image(
                documentId,
                (String) map.get("eventId"),
                (String) map.get("organizerId"),
                (String) map.get("organizerName"),
                (String) map.get("imageData"),
                map.get("uploadedAt") != null ? (Long) map.get("uploadedAt") : System.currentTimeMillis(),
                (String) map.get("fileName")
        );
    }
}

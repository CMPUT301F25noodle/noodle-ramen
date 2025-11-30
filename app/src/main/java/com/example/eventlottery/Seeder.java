/**
 * Seeder Class to fill the DB with Test Events
 */

package com.example.eventlottery;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeder for populating the database with test events for search functionality testing.
 * Creates diverse events across different categories, price ranges, locations, and dates.
 */
public class Seeder {

    private final FirebaseFirestore db;
    private final String organizerId;

    /**
     * Creates a new Seeder instance.
     * @param organizerId The user ID to use as the organizer for seeded events
     */
    public Seeder(String organizerId) {
        this.db = FirebaseFirestore.getInstance();
        this.organizerId = organizerId != null ? organizerId : "test-organizer-id";
    }

    /**
     * Seeds the database with test events across all categories.
     * Creates 20 diverse events for comprehensive search testing.
     */
    public void seedDatabase(SeedCallback callback) {
        List<Map<String, Object>> events = generateTestEvents();

        int[] successCount = {0};
        int[] failureCount = {0};
        int totalEvents = events.size();

        for (Map<String, Object> eventData : events) {
            db.collection("events")
                    .add(eventData)
                    .addOnSuccessListener(documentReference -> {
                        successCount[0]++;
                        if (successCount[0] + failureCount[0] == totalEvents) {
                            callback.onComplete(successCount[0], failureCount[0]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        failureCount[0]++;
                        if (successCount[0] + failureCount[0] == totalEvents) {
                            callback.onComplete(successCount[0], failureCount[0]);
                        }
                    });
        }
    }

    /**
     * Generates a list of diverse test events.
     */
    private List<Map<String, Object>> generateTestEvents() {
        List<Map<String, Object>> events = new ArrayList<>();

        // Sports Events
        events.add(createEvent(
                "City Marathon 2025",
                "Edmonton Sports Association",
                "Commonwealth Stadium, Edmonton",
                "Join us for the annual city marathon with routes for all skill levels",
                "Open to all ages, registration required",
                "01/15/2026", "01/15/2026",
                "25", "500", "100",
                false, "Sports"
        ));

        events.add(createEvent(
                "Basketball Tournament",
                "University of Alberta",
                "Van Vliet Centre, Edmonton",
                "Competitive basketball tournament for local teams",
                "Teams must register in advance",
                "02/20/2026", "02/22/2026",
                "0", "200", "50",
                false, "Sports"
        ));

        events.add(createEvent(
                "Yoga in the Park",
                "Wellness Edmonton",
                "Hawrelak Park, Edmonton",
                "Free outdoor yoga sessions every Saturday morning",
                "All skill levels welcome",
                "05/01/2026", "08/30/2026",
                "0", "0", "30",
                true, "Sports"
        ));

        // Music Events
        events.add(createEvent(
                "Summer Music Festival",
                "Edmonton Arts Council",
                "Churchill Square, Edmonton",
                "Three-day music festival featuring local and international artists",
                "All ages event, ID required for alcohol purchase",
                "07/15/2026", "07/17/2026",
                "75", "10000", "2000",
                false, "Music"
        ));

        events.add(createEvent(
                "Jazz Night at the Winspear",
                "Winspear Centre",
                "4 Sir Winston Churchill Square, Edmonton",
                "An intimate evening of live jazz music",
                "18+ only",
                "03/10/2026", "03/10/2026",
                "50", "500", "100",
                false, "Music"
        ));

        events.add(createEvent(
                "Open Mic Night",
                "The Needle Coffee Bar",
                "124 Street, Edmonton",
                "Showcase your musical talent at our weekly open mic",
                "Performers must sign up in advance",
                "Every Friday", "Every Friday",
                "0", "0", "20",
                false, "Music"
        ));

        // Arts Events
        events.add(createEvent(
                "Art Gallery Opening",
                "Art Gallery of Alberta",
                "2 Sir Winston Churchill Square, Edmonton",
                "Opening reception for our new contemporary art exhibition",
                "Free admission, all welcome",
                "04/05/2026", "04/05/2026",
                "0", "300", "80",
                false, "Arts"
        ));

        events.add(createEvent(
                "Photography Workshop",
                "Edmonton Camera Club",
                "Old Strathcona, Edmonton",
                "Learn advanced photography techniques from professionals",
                "Participants must bring their own camera",
                "06/12/2026", "06/13/2026",
                "150", "30", "10",
                false, "Arts"
        ));

        events.add(createEvent(
                "Pottery Class Series",
                "Clay Works Studio",
                "Whyte Avenue, Edmonton",
                "8-week pottery course for beginners",
                "All materials included",
                "09/01/2026", "10/24/2026",
                "200", "20", "5",
                false, "Arts"
        ));

        // Educational Events
        events.add(createEvent(
                "Science Fair",
                "Edmonton Public Schools",
                "Telus World of Science, Edmonton",
                "Annual student science fair showcasing innovative projects",
                "Open to all students grades 1-12",
                "03/20/2026", "03/21/2026",
                "0", "0", "200",
                false, "Educational"
        ));

        events.add(createEvent(
                "Tech Conference 2026",
                "Alberta Tech Alliance",
                "Edmonton Convention Centre",
                "Two-day technology conference with industry leaders",
                "Registration required, student discounts available",
                "11/05/2026", "11/06/2026",
                "300", "1000", "200",
                false, "Educational"
        ));

        events.add(createEvent(
                "Book Club: Classic Literature",
                "Edmonton Public Library",
                "Stanley A. Milner Library, Edmonton",
                "Monthly book discussion group focusing on classic literature",
                "Free for library members",
                "Every Month", "Every Month",
                "0", "25", "8",
                false, "Educational"
        ));

        // Workshops
        events.add(createEvent(
                "Cooking Class: Italian Cuisine",
                "Culinary Institute Edmonton",
                "Downtown Edmonton",
                "Learn to make authentic Italian pasta and sauces",
                "Must be 16+ years old",
                "05/15/2026", "05/15/2026",
                "85", "15", "5",
                false, "Workshops"
        ));

        events.add(createEvent(
                "DIY Home Repair Workshop",
                "Home Depot Edmonton",
                "Various Locations, Edmonton",
                "Learn basic home repair skills from experts",
                "Free, registration required",
                "04/18/2026", "04/18/2026",
                "0", "50", "15",
                false, "Workshops"
        ));

        events.add(createEvent(
                "Financial Planning Seminar",
                "Edmonton Investment Group",
                "Commerce Place, Edmonton",
                "Free seminar on retirement planning and investment strategies",
                "Open to all adults",
                "06/30/2026", "06/30/2026",
                "0", "100", "30",
                false, "Workshops"
        ));

        // Other Category
        events.add(createEvent(
                "Community Garage Sale",
                "Riverbend Community League",
                "Riverbend, Edmonton",
                "Annual community-wide garage sale event",
                "Vendors must register tables in advance",
                "08/15/2026", "08/15/2026",
                "10", "0", "50",
                false, "Other"
        ));

        events.add(createEvent(
                "Charity Fundraiser Gala",
                "Edmonton Food Bank",
                "Fairmont Hotel Macdonald, Edmonton",
                "Elegant evening gala supporting local food security",
                "Formal attire required, 19+ only",
                "10/10/2026", "10/10/2026",
                "250", "400", "100",
                false, "Other"
        ));

        events.add(createEvent(
                "Dog Park Meetup",
                "Edmonton Dog Owners Association",
                "Terwillegar Park, Edmonton",
                "Monthly social gathering for dog owners",
                "Dogs must be vaccinated and friendly",
                "Every Month", "Every Month",
                "0", "0", "40",
                true, "Other"
        ));

        events.add(createEvent(
                "Halloween Costume Contest",
                "West Edmonton Mall",
                "West Edmonton Mall, Edmonton",
                "Spooky costume contest with prizes for all age categories",
                "Pre-registration encouraged",
                "10/31/2025", "10/31/2025",
                "5", "500", "150",
                false, "Other"
        ));

        events.add(createEvent(
                "New Year's Eve Celebration",
                "City of Edmonton",
                "Churchill Square, Edmonton",
                "Ring in the new year with fireworks and live entertainment",
                "Free event, all ages welcome",
                "12/31/2025", "01/01/2026",
                "0", "0", "5000",
                false, "Other"
        ));

        return events;
    }

    /**
     * Helper method to create an event data map.
     */
    private Map<String, Object> createEvent(
            String eventName,
            String organizerName,
            String location,
            String description,
            String eligibility,
            String startDate,
            String endDate,
            String price,
            String waitlistLimit,
            String entrantMaxCapacity,
            boolean geolocationRequired,
            String category) {

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventName", eventName);
        eventData.put("organizer", organizerId);
        eventData.put("organizerName", organizerName);
        eventData.put("location", location);
        eventData.put("description", description);
        eventData.put("eligibility", eligibility);
        eventData.put("startDate", startDate);
        eventData.put("endDate", endDate);
        eventData.put("price", price);
        eventData.put("waitlistLimit", waitlistLimit);
        eventData.put("entrantMaxCapacity", entrantMaxCapacity);
        eventData.put("geolocationRequired", geolocationRequired);
        eventData.put("category", category);
        eventData.put("createdAt", System.currentTimeMillis());

        return eventData;
    }

    /**
     * Callback interface for seed operation completion.
     */
    public interface SeedCallback {
        void onComplete(int successCount, int failureCount);
    }
}
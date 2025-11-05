package com.example.eventlottery.managers;
import android.app.NotificationManager;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lottery manager - handles lottery logic for events
 * Runs the inital lottery draw
 * Manages taking entrants from waitlsit to accepted or decliend
 * draws replacements when entrants decline
 */
public class LotteryManager {
    private static final String TAG = "LotteryManager";
    private final FirebaseFirestore db;
    private final NotificationManager notificationManager;

    /**
     * constructor
     */
    public LotteryManager() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationManager= new NotificationManager();

    }

    /**
     * initialize the lottery and select the winners
     * get all entrants from waitling list
     * random shuffle
     * select the first x number of entrants as specificied by organizer when makig the event
     * gives winners to selected and give them pending status until they accept
     * send notifications to all wialtist participants, based on if they have selected or not selected status the message will be different
     *
     * @param eventId the event to run the lottery for
     * @param sampleSize the number of winners that need to be selected provided by orgnaizers
     *
     */

    public void initializeLottery(String eventId, int sampleSize, LotteryCallback callback) {

    }




}

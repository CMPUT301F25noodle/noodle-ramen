package com.example.eventlottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnEventClickListener listener;

    // Interface for click listeners
    public interface OnEventClickListener {
        void onJoinWaitlistClick(Event event);
        void onEventPageClick(Event event);
    }

    // Constructor
    public EventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList != null ? eventList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    // Update the event list
    public void updateEvents(List<Event> newEvents) {
        this.eventList = newEvents;
        notifyDataSetChanged();
    }

    // ViewHolder class
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView statusBadge;
        private TextView priceText;
        private ImageView eventImage;
        private TextView eventTitle;
        private TextView organizationName;
        private TextView locationText;
        private TextView dateRangeText;
        private TextView waitlistInfo;
        private TextView spotsText;
        private Button joinWaitlistButton;
        private Button goToEventButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize all views
            statusBadge = itemView.findViewById(R.id.statusBadge);
            priceText = itemView.findViewById(R.id.priceText);
            eventImage = itemView.findViewById(R.id.eventImage);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            organizationName = itemView.findViewById(R.id.organizationName);
            locationText = itemView.findViewById(R.id.locationText);
            dateRangeText = itemView.findViewById(R.id.dateRangeText);
            waitlistInfo = itemView.findViewById(R.id.waitlistInfo);
            spotsText = itemView.findViewById(R.id.spotsText);
            joinWaitlistButton = itemView.findViewById(R.id.joinWaitlistButton);
            goToEventButton = itemView.findViewById(R.id.goToEventButton);
        }

        public void bind(Event event, OnEventClickListener listener) {
            // Set event details
            statusBadge.setText(event.getStatus());
            priceText.setText(event.getFormattedPrice());
            eventTitle.setText(event.getTitle());
            organizationName.setText("by " + event.getOrganizationName());
            locationText.setText(event.getLocation());
            dateRangeText.setText(event.getDateRange());
            waitlistInfo.setText(event.getWaitlistInfo());
            spotsText.setText(event.getAvailableSpots() + " spots");

            // Update button text based on waitlist status
            if (event.isUserOnWaitlist()) {
                joinWaitlistButton.setText("On Waitlist");
                joinWaitlistButton.setEnabled(false);
            } else {
                joinWaitlistButton.setText("Join Waitlist");
                joinWaitlistButton.setEnabled(true);
            }

            // Set click listeners
            joinWaitlistButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onJoinWaitlistClick(event);
                }
            });

            goToEventButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventPageClick(event);
                }
            });

            // TODO: Load image from URL using Glide or Picasso
            // For now, image placeholder is shown
        }
    }
}
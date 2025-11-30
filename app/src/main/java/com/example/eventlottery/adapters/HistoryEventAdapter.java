package com.example.eventlottery.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.R;
import com.example.eventlottery.models.HistoryEventViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple adapter for Event History
 */
public class HistoryEventAdapter extends RecyclerView.Adapter<HistoryEventAdapter.ViewHolder> {
    private List<HistoryEventViewModel> events;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(HistoryEventViewModel event);
    }

    public HistoryEventAdapter(List<HistoryEventViewModel> events, OnEventClickListener listener) {
        this.events = events != null ? events : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEventViewModel event = events.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView eventName;
        private TextView eventLocation;
        private TextView eventDates;
        private TextView eventPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.history_event_name);
            eventLocation = itemView.findViewById(R.id.history_event_location);
            eventDates = itemView.findViewById(R.id.history_event_dates);
            eventPrice = itemView.findViewById(R.id.history_event_price);
        }

        public void bind(HistoryEventViewModel event, OnEventClickListener listener) {
            eventName.setText(event.getEventName() != null ? event.getEventName() : "Untitled Event");
            eventLocation.setText(event.getLocation() != null ? event.getLocation() : "TBD");
            eventDates.setText(event.getDateRange());
            eventPrice.setText(event.getFormattedPrice());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }
    }
}

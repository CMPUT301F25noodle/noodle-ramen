package com.example.eventlottery.event_classes;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlottery.R;
import java.util.ArrayList;
import java.util.List;

/**
 * EventAdapter - Handles event history page
 * Created by: Jana
 * Handles the tabs that show events that the user registered, won, lost, and is pending in.
 */

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<EventViewModel> eventViewModels;
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventPageClick(EventViewModel viewModel);
        void onPrimaryActionClick(EventViewModel viewModel); // Combined action (Join, Accept, Opt-in)
        void onSecondaryActionClick(EventViewModel viewModel); // Secondary action (Decline, Leave)
    }

    public EventAdapter(List<EventViewModel> eventViewModels, OnEventClickListener listener) {
        this.eventViewModels = eventViewModels != null ? eventViewModels : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(eventViewModels.get(position), listener);
    }

    @Override
    public int getItemCount() { return eventViewModels.size(); }

    public void updateEvents(List<EventViewModel> newEventViewModels) {
        this.eventViewModels = newEventViewModels != null ? newEventViewModels : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView statusBadge, priceText, eventTitle, organizationName, locationText, dateRangeText, waitlistInfo, spotsText;
        private final Button primaryButton, secondaryButton, goToEventButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            priceText = itemView.findViewById(R.id.priceText);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            organizationName = itemView.findViewById(R.id.organizationName);
            locationText = itemView.findViewById(R.id.locationText);
            dateRangeText = itemView.findViewById(R.id.dateRangeText);
            waitlistInfo = itemView.findViewById(R.id.waitlistInfo);
            spotsText = itemView.findViewById(R.id.spotsText);
            primaryButton = itemView.findViewById(R.id.primaryButton);
            secondaryButton = itemView.findViewById(R.id.secondaryButton);
            goToEventButton = itemView.findViewById(R.id.goToEventButton);
        }

        public void bind(EventViewModel vm, OnEventClickListener listener) {
            // 1. Bind Data to Views
            // (Ensure your EventViewModel has these getter methods!)
            eventTitle.setText(vm.getTitle());
            locationText.setText(vm.getLocationText());
            organizationName.setText(vm.getFormattedOrganization());
            dateRangeText.setText(vm.getDateRange());
            priceText.setText(vm.getFormattedPrice());
            waitlistInfo.setText(vm.getWaitlistInfo());
            spotsText.setText(vm.getSpotsText());

            // navigation Button Always Active
            goToEventButton.setOnClickListener(v -> listener.onEventPageClick(vm));

            //reset Button States for Recycling
            primaryButton.setVisibility(View.VISIBLE);
            primaryButton.setEnabled(true);
            // default purple look
            primaryButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CE93D8")));
            primaryButton.setTextColor(Color.WHITE);

            secondaryButton.setVisibility(View.GONE);
            secondaryButton.setOnClickListener(null);

            // configure Status
            String status = vm.getStatus() != null ? vm.getStatus() : "NONE";
            statusBadge.setText(status);

            switch (status) {
                case "WON":
                    primaryButton.setText("Accept");
                    primaryButton.setOnClickListener(v -> listener.onPrimaryActionClick(vm));
                    secondaryButton.setVisibility(View.VISIBLE);
                    secondaryButton.setText("Decline");
                    secondaryButton.setOnClickListener(v -> listener.onSecondaryActionClick(vm));
                    break;

                case "LOST":
                    primaryButton.setText("Opt-in for Retry");
                    primaryButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#90A4AE")));
                    primaryButton.setOnClickListener(v -> listener.onPrimaryActionClick(vm));
                    break;

                case "REGISTERED":
                    primaryButton.setText("Attending");
                    primaryButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#81C784")));
                    primaryButton.setEnabled(false); // Acts as a badge
                    break;

                case "PENDING":
                    primaryButton.setText("Waitlisted");
                    primaryButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB74D")));
                    primaryButton.setEnabled(false); // Acts as a badge
                    secondaryButton.setVisibility(View.VISIBLE);
                    secondaryButton.setText("Leave");
                    secondaryButton.setOnClickListener(v -> listener.onSecondaryActionClick(vm));
                    break;

                default: // Browse mode
                    primaryButton.setText("Join Waitlist");
                    primaryButton.setOnClickListener(v -> listener.onPrimaryActionClick(vm));
                    break;
            }
        }
    }
}
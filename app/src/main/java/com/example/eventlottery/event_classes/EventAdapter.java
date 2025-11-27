package com.example.eventlottery.event_classes;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlottery.managers.ImageManager;
import com.example.eventlottery.models.Image;
import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.Glide;
import com.example.eventlottery.R;
import com.example.eventlottery.utils.ImageCompressionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying a list of events in a RecyclerView.
 * Connects event data to the RecyclerView by creating and binding ViewHolders.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<EventViewModel> eventViewModels; // Models
    private OnEventClickListener listener; // Listeners

    /**
     * Interface for handling user interactions with event cards.
     */
    public interface OnEventClickListener {
        /**
         * Called when user clicks the "Join Waitlist" button.
         *
         * @param eventViewModel the event that was clicked
         */
        void onJoinWaitlistClick(EventViewModel eventViewModel);

        /**
         * Called when user clicks the "Go to Event Page" button.
         *
         * @param eventViewModel the event that was clicked
         */
        void onEventPageClick(EventViewModel eventViewModel);
    }

    /**
     * Creates an EventAdapter with event data and click listener.
     *
     * @param eventViewModels list of events to display
     * @param listener        callback for button clicks
     */
    public EventAdapter(List<EventViewModel> eventViewModels, OnEventClickListener listener) {
        this.eventViewModels = eventViewModels != null ? eventViewModels : new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Creates a new ViewHolder by inflating the item layout.
     * Called by RecyclerView when it needs a new view.
     *
     * @param parent   the RecyclerView
     * @param viewType the view type (not used, we have only one type)
     * @return new EventViewHolder wrapping the inflated view
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds event data to a ViewHolder at the specified position.
     * Called by RecyclerView when an item becomes visible.
     *
     * @param holder   the ViewHolder to bind data to
     * @param position the position in the data list
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventViewModel viewModel = eventViewModels.get(position);
        holder.bind(viewModel, listener);
    }

    /**
     * Returns the total number of items in the list.
     *
     * @return size of the event list
     */
    @Override
    public int getItemCount() {
        return eventViewModels.size();
    }

    /**
     * Updates the adapter with a new list of events and refreshes the RecyclerView.
     *
     * @param newEventViewModels new list of events to display
     */
    public void updateEvents(List<EventViewModel> newEventViewModels) {
        this.eventViewModels = newEventViewModels != null ? newEventViewModels : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for one event card. Caches view references for performance.
     * Made static to avoid holding reference to outer Adapter class.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        // UI element references
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
        private View imageContainer;


        /**
         * Creates a ViewHolder and caches references to all child views.
         *
         * @param itemView the layout for one event card
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find and cache all view references
            statusBadge = itemView.findViewById(R.id.status_badge);
            priceText = itemView.findViewById(R.id.price_text);
            eventImage = itemView.findViewById(R.id.eventImage);
            eventTitle = itemView.findViewById(R.id.event_title);
            organizationName = itemView.findViewById(R.id.organization_name);
            locationText = itemView.findViewById(R.id.location_text);
            dateRangeText = itemView.findViewById(R.id.date_range_text);
            waitlistInfo = itemView.findViewById(R.id.waitlist_info);
            spotsText = itemView.findViewById(R.id.spots_text);
            joinWaitlistButton = itemView.findViewById(R.id.join_waitlist_button);
            goToEventButton = itemView.findViewById(R.id.go_to_event_button);
            imageContainer = itemView.findViewById(R.id.image_container);
        }

        /**
         * Binds event data from ViewModel to the UI elements.
         *
         * @param viewModel the event data to display
         * @param listener  callback for button clicks
         */
        public void bind(EventViewModel viewModel, OnEventClickListener listener) {
            // Text Binding
            statusBadge.setText(viewModel.getStatusText());
            priceText.setText(viewModel.getFormattedPrice());
            eventTitle.setText(viewModel.getTitle());
            organizationName.setText(viewModel.getFormattedOrganization());
            locationText.setText(viewModel.getLocationText());
            dateRangeText.setText(viewModel.getDateRange());
            waitlistInfo.setText(viewModel.getWaitlistInfo());
            spotsText.setText(viewModel.getSpotsText());
            eventImage.setVisibility(View.GONE);

            // Image Binding
            loadImage(viewModel.getId());

            // Button Binding
            setupButtons(viewModel, listener);

        }

        private void setupButtons(EventViewModel vm, OnEventClickListener listener) {
            if (vm.isUserOnWaitlist()) {
                joinWaitlistButton.setText("Leave Waitlist");
                joinWaitlistButton.setEnabled(true);
            } else {
                joinWaitlistButton.setText(vm.getJoinButtonText());
                joinWaitlistButton.setEnabled(vm.isJoinButtonEnabled());
            }

            joinWaitlistButton.setOnClickListener(v -> {
                if (listener != null) listener.onJoinWaitlistClick(vm);
            });

            goToEventButton.setOnClickListener(v -> {
                if (listener != null) listener.onEventPageClick(vm);
            });

        }

        private void loadImage(String eventId) {
            // 1. Set a placeholder immediately so reused views don't show old images
            if (eventImage != null) {
                eventImage.setImageResource(R.drawable.ic_launcher_foreground);

                ImageManager.getInstance().getImagesForEvent(eventId, new ImageManager.ImageListCallback() {
                    @Override
                    public void onSuccess(List<Image> images) {
                        // Always update UI on Main Thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!images.isEmpty() && images.get(0).getImageData() != null) {

                                // Decode string
                                Bitmap bitmap = ImageCompressionHelper.decodeFromBase64(images.get(0).getImageData());

                                if (bitmap != null && eventImage != null) {
                                    // Turn visibility ON so we can see the event image
                                    // This alone took me like 3 hours to find bceuase I set this
                                    // Boilerplate code like a week ago
                                    eventImage.setVisibility(View.VISIBLE);

                                    Glide.with(itemView.getContext())
                                            .load(bitmap)
                                            .centerCrop()
                                            .into(eventImage);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        // Maybe set placeholder here??
                    }
                });
            }
        }
    }
}
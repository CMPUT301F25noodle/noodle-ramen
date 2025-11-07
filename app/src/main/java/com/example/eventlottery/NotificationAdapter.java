package com.example.eventlottery;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.managers.NotificationManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    /**
     * interrface for handling notificaint actions
     * */

    public interface OnNotificationClickListener {
        void onAcceptClicked(Notification notification);
        void onDeclineClicked(Notification notification);
    }

    /**
     * construcotr
     */
    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * vierwholder for notification items
     */

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private TextView titleTextView;
        private TextView messageTextView;
        private TextView timestampTextView;
        private Button acceptButton;
        private Button declineButton;
        private TextView respondedTextView;



        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.notification_card);
            titleTextView = itemView.findViewById(R.id.notification_title);
            messageTextView = itemView.findViewById(R.id.notification_message);
            timestampTextView = itemView.findViewById(R.id.notification_timestamp);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
            respondedTextView = itemView.findViewById(R.id.responded_text);
        }
        public void bind(Notification notification, OnNotificationClickListener listener) {
            // Set title based on type
            String title = getTitleForType(notification.getType());
            titleTextView.setText(title);

            // Set message
            messageTextView.setText(notification.getMessage());

            // Set timestamp
            String timeAgo = getTimeAgo(notification.getTimestamp());
            timestampTextView.setText(timeAgo);

            // Show/hide buttons based on whether notification requires response
            if (notification.requiresResponse()) {
                // Show Accept/Decline buttons
                acceptButton.setVisibility(View.VISIBLE);
                declineButton.setVisibility(View.VISIBLE);
                respondedTextView.setVisibility(View.GONE);

                // Set click listeners
                acceptButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAcceptClicked(notification);
                    }
                });

                declineButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeclineClicked(notification);
                    }
                });
            } else {

                acceptButton.setVisibility(View.GONE);
                declineButton.setVisibility(View.GONE);

                // show responded status
                if (notification.isResponded()) {
                    respondedTextView.setVisibility(View.VISIBLE);
                    respondedTextView.setText("✓ Responded");
                } else {
                    respondedTextView.setVisibility(View.GONE);
                }
            }

            // indicator for unread ntoifaaitons
            if (!notification.isRead()) {
                cardView.setCardBackgroundColor(
                        itemView.getContext().getResources().getColor(android.R.color.holo_blue_light));
            } else {
                cardView.setCardBackgroundColor(
                        itemView.getContext().getResources().getColor(android.R.color.white));
            }
        }

        /**
         * Get title based on notification type
         */
        private String getTitleForType(String type) {
            switch (type) {
                case NotificationManager.TYPE_WIN:
                    return " You Won!";
                case NotificationManager.TYPE_REPLACEMENT:
                    return " You Won!";
                case NotificationManager.TYPE_LOSS:
                    return " Lottery Results";
                default:
                    return "Notification";
            }
        }

        /**
         * Convert timestamp to "time ago" format
         */
        private String getTimeAgo(Long timestamp) {
            if (timestamp == null) {
                return "Just now";
            }

            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (days < 7) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else {
                // For older notifications, show actual date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}















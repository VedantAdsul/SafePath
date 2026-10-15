package com.vedant.safepath.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vedant.safepath.Model.Notification;
import com.vedant.safepath.R;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notificationList;
    private final Runnable emptyStateCallback;

    public NotificationAdapter(List<Notification> notificationList, Runnable emptyStateCallback) {
        this.notificationList = notificationList;
        this.emptyStateCallback = emptyStateCallback;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.title.setText(notification.getTitle());
        holder.description.setText(notification.getDescription());
        holder.timestamp.setText(notification.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void clear() {
        int size = notificationList.size();
        if (size > 0) {
            notificationList.clear();
            notifyItemRangeRemoved(0, size);
            if (emptyStateCallback != null) {
                emptyStateCallback.run();
            }
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView description;
        TextView timestamp;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            description = itemView.findViewById(R.id.notification_description);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
        }
    }
}

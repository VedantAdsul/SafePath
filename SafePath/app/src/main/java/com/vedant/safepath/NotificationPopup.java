package com.vedant.safepath;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vedant.safepath.Adapter.NotificationAdapter;
import com.vedant.safepath.Model.Notification;
import com.vedant.safepath.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationPopup {

    public static void show(Context context, View anchorView) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View popupView = inflater.inflate(R.layout.popup_notifications, null);

        final PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        RecyclerView recyclerView = popupView.findViewById(R.id.notifications_recycler_view);
        TextView emptyView = popupView.findViewById(R.id.empty_view);
        TextView clearAllButton = popupView.findViewById(R.id.clear_all_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        List<Notification> notifications = getDummyNotifications();

        Runnable emptyStateCallback = () -> {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        };

        NotificationAdapter adapter = new NotificationAdapter(notifications, emptyStateCallback);
        recyclerView.setAdapter(adapter);

        if (notifications.isEmpty()) {
            emptyStateCallback.run();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        clearAllButton.setOnClickListener(v -> adapter.clear());

        popupWindow.setElevation(24);
        popupWindow.setBackgroundDrawable(new ColorDrawable());
        popupWindow.setOutsideTouchable(true);

        // --- FIX START ---
        // Use showAtLocation for precise screen positioning.
        // This is more reliable than calculating offsets with showAsDropDown.

        // Get the height of the ActionBar to use as a top margin.
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }

        // Set a small margin from the right edge of the screen (in pixels).
        int rightMarginPx = (int) (12 * context.getResources().getDisplayMetrics().density);

        // Show the popup at the top-right of the screen with our calculated margins.
        popupWindow.showAtLocation(anchorView.getRootView(), Gravity.TOP | Gravity.END, rightMarginPx, actionBarHeight);
        // --- FIX END ---
    }

    private static List<Notification> getDummyNotifications() {
        List<Notification> list = new ArrayList<>();
        list.add(new Notification("Welcome to SafePath!", "We're glad to have you here. Explore safety tips.", "2m ago"));
        list.add(new Notification("Profile Update", "Complete your user profile for a better experience.", "1h ago"));
        list.add(new Notification("New Guide Added", "Check out 'Staying Safe in Public Transport'.", "1d ago"));
        list.add(new Notification("Contact Verified", "Your emergency contact has been verified.", "2d ago"));
        return list;
    }
}

package com.vedant.safepath.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vedant.safepath.R;
import com.vedant.safepath.Model.VideoItem;
import com.vedant.safepath.VideoPlayerActivity;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private Context context;
    private List<VideoItem> videoList;

    public VideoAdapter(Context context, List<VideoItem> videoList) {
        this.context = context;
        this.videoList = videoList;
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;

        public VideoViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            title = itemView.findViewById(R.id.videoTitle);
        }
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_card, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videoList.get(position);
        VideoItem videoItem = videoList.get(position);


        holder.title.setText(video.getTitle());
        Glide.with(context).load(video.getThumbnailUrl()).into(holder.thumbnail);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.VIDEO_ID_KEY, videoItem.getVideoId());

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }
}


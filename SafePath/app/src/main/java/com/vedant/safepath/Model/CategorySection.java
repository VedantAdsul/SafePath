package com.vedant.safepath.Model;

import java.util.List;

public class CategorySection {
    private String categoryTitle;
    private List<VideoItem> videoList;

    public CategorySection(String categoryTitle, List<VideoItem> videoList) {
        this.categoryTitle = categoryTitle;
        this.videoList = videoList;
    }

    public String getCategoryTitle() {
        return categoryTitle;
    }

    public List<VideoItem> getVideoList() {
        return videoList;
    }
}


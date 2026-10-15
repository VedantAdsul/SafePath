package com.vedant.safepath.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vedant.safepath.BuildConfig;
import com.vedant.safepath.Adapter.CategoryAdapter;
import com.vedant.safepath.Model.CategorySection;
import com.vedant.safepath.R;
import com.vedant.safepath.Model.VideoItem;
import com.vedant.safepath.YouTubeFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuideFragment extends Fragment {

    private RecyclerView guideRecyclerView;
    private CategoryAdapter adapter;
    private final List<CategorySection> fullCategoryList = new ArrayList<>();
    private final List<CategorySection> filteredCategoryList = new ArrayList<>();
    private SearchView searchView;
    private ImageView btnMic;
    private ActivityResultLauncher<Intent> voiceSearchLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeVoiceSearchLauncher();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide, container, false);

        guideRecyclerView = view.findViewById(R.id.guide_recycler_view);
        searchView = view.findViewById(R.id.search_view);
        btnMic = view.findViewById(R.id.mic_button);

        btnMic.setOnClickListener(v -> startVoiceSearch());

        setupRecyclerView();
        setupSearch();

        if (fullCategoryList.isEmpty()) {
            loadCategories();
        }

        return view;
    }

    private void setupRecyclerView() {
        guideRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CategoryAdapter(getContext(), filteredCategoryList);
        guideRecyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCategories(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCategories(newText);
                return true;
            }
        });
    }

    private void filterCategories(String query) {
        filteredCategoryList.clear();
        if (query.isEmpty()) {
            filteredCategoryList.addAll(fullCategoryList);
        } else {
            for (CategorySection section : fullCategoryList) {
                if (section.getCategoryTitle().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                    filteredCategoryList.add(section);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadCategories() {
        String apiKey = BuildConfig.YOUTUBE_API_KEY;

        String[] searchQueries = {
                "How to perform CPR", "First aid for bleeding", "First aid for burns",
                "How to help choking victim", "First aid for heart attack", "Emergency child care"
        };

        String[] categoryTitles = {
                "CPR", "Bleeding Control", "Burn Treatment",
                "Choking Rescue", "Heart Attack Response", "Child Emergency Aid"
        };

        for (int i = 0; i < searchQueries.length; i++) {
            final int index = i;
            final String query = searchQueries[i];
            final String categoryTitle = categoryTitles[i];

            fullCategoryList.add(new CategorySection(categoryTitle, null));

            YouTubeFetcher.fetchYouTubeVideos(getContext(), query, apiKey, new YouTubeFetcher.YouTubeCallback() {
                @Override
                public void onVideosFetched(List<VideoItem> videoItems) {
                    if (isAdded() && index < fullCategoryList.size()) {
                        fullCategoryList.set(index, new CategorySection(categoryTitle, videoItems));
                        filterCategories(searchView.getQuery().toString());
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("GuideFragment", "Failed to fetch videos for " + categoryTitle + ": " + error);
                }
            });
        }
        filterCategories("");
    }

    private void initializeVoiceSearchLauncher() {
        voiceSearchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> spokenResult = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (spokenResult != null && !spokenResult.isEmpty()) {
                            String spokenText = spokenResult.get(0);
                            searchView.setQuery(spokenText, false);
                        }
                    }
                }
        );
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            voiceSearchLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Voice search not available", Toast.LENGTH_SHORT).show();
        }
    }
}

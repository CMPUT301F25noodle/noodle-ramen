package com.example.eventlottery.utils;
import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
public class PlacesCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private final List<String> resultList = new ArrayList<>();
    private final List<AutocompletePrediction> resultPredictions = new ArrayList<>();
    private final PlacesClient placesClient;
    private AutocompleteSessionToken token;

    public PlacesCompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.placesClient = placesClient;
        this.token = AutocompleteSessionToken.newInstance();
    }

    public String getPlaceId(int position) {
        if (position < resultPredictions.size()) {
            return resultPredictions.get(position).getPlaceId();
        }
        return null;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public String getItem(int position) {
        return resultList.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // Create a new token for each search session if needed, or maintain one
                    // token = AutocompleteSessionToken.newInstance();

                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setSessionToken(token)
                            .setQuery(constraint.toString())
                            .build();

                    try {
                        // We must wait for the results on this worker thread
                        FindAutocompletePredictionsResponse response =
                                Tasks.await(placesClient.findAutocompletePredictions(request), 60, TimeUnit.SECONDS);

                        if (response != null) {
                            resultList.clear();
                            resultPredictions.clear();
                            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                resultList.add(prediction.getFullText(null).toString());
                                resultPredictions.add(prediction);
                            }
                            filterResults.values = resultList;
                            filterResults.count = resultList.size();
                        }
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        Log.e("PlacesAdapter", "Error fetching suggestions", e);
                    }
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

}

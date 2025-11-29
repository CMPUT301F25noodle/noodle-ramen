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
/**
 * PlacesCompleteAdapter is a custom adapter that provides autocomplete suggestions for places.
 * It connects to the Google Places API to fetch predictions based on the user's input text
 * and displays them in a dropdown list.
 */
public class PlacesCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private final List<String> resultList = new ArrayList<>();
    private final List<AutocompletePrediction> resultPredictions = new ArrayList<>();
    private final PlacesClient placesClient;
    private AutocompleteSessionToken token;
    /**
     * Initializes the adapter with the context and the Places API client.
     *
     * @param context      The application context.
     * @param placesClient The initialized Google Places client used to fetch predictions.
     */
    public PlacesCompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.placesClient = placesClient;
        this.token = AutocompleteSessionToken.newInstance();
    }
    /**
     * Retrieves the unique Place ID for the item at the specified position.
     * This ID can be used to fetch more details about the selected place.
     *
     * @param position The index of the item in the list.
     * @return The Place ID string, or null if the position is invalid.
     */
    public String getPlaceId(int position) {
        if (position < resultPredictions.size()) {
            return resultPredictions.get(position).getPlaceId();
        }
        return null;
    }
    /**
     * Returns the total number of suggestion results currently stored in the adapter.
     *
     * @return The count of results.
     */
    @Override
    public int getCount() {
        return resultList.size();
    }
    /**
     * Returns the suggestion text (address or place name) at the specified position.
     *
     * @param position The index of the item.
     * @return The full text description of the place.
     */
    @Override
    public String getItem(int position) {
        return resultList.get(position);
    }
    /**
     * Returns a filter that performs the actual search query to the Google Places API.
     * It runs asynchronously to fetch predictions as the user types.
     *
     * @return A Filter object that handles the API request and result publishing.
     */
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

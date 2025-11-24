package com.example.eventlottery;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

/**
 * Dialog fragment for filtering events
 */
public class FilterDialogFragment extends DialogFragment {

    // UI components
    private CheckBox cbSports, cbMusic, cbArts, cbTech, cbEducational, cbWorkshops, cbOther;
    private EditText editStartDate, editEndDate;
    private EditText editMinPrice, editMaxPrice;
    private EditText editLocation;
    private Button btnApplyFilters, btnClearFilters;

    // Current filter
    private EventFilter currentFilter;

    // Callback interface
    public interface FilterAppliedListener {
        void onFiltersApplied(EventFilter filter);
    }

    private FilterAppliedListener listener;

    public static FilterDialogFragment newInstance(EventFilter currentFilter) {
        FilterDialogFragment fragment = new FilterDialogFragment();
        Bundle args = new Bundle();
        // You could pass current filter here if needed
        fragment.setArguments(args);
        fragment.currentFilter = currentFilter != null ? currentFilter : new EventFilter();
        return fragment;
    }

    public void setFilterAppliedListener(FilterAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        populateCurrentFilters();
        setupListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make dialog wider
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void initializeViews(View view) {
        // Category checkboxes
        cbSports = view.findViewById(R.id.cb_sports);
        cbMusic = view.findViewById(R.id.cb_music);
        cbArts = view.findViewById(R.id.cb_arts);
        cbTech = view.findViewById(R.id.cb_tech);
        cbEducational = view.findViewById(R.id.cb_educational);
        cbWorkshops = view.findViewById(R.id.cb_workshops);
        cbOther = view.findViewById(R.id.cb_other);

        // Date fields
        editStartDate = view.findViewById(R.id.edit_start_date);
        editEndDate = view.findViewById(R.id.edit_end_date);

        // Price fields
        editMinPrice = view.findViewById(R.id.edit_min_price);
        editMaxPrice = view.findViewById(R.id.edit_max_price);

        // Location field
        editLocation = view.findViewById(R.id.edit_location);

        // Buttons
        btnApplyFilters = view.findViewById(R.id.btn_apply_filters);
        btnClearFilters = view.findViewById(R.id.btn_clear_filters);
    }

    private void populateCurrentFilters() {
        if (currentFilter == null) return;

        // Set activity type - check the appropriate checkbox
        if (currentFilter.getActivityType() != null) {
            CheckBox checkboxToSelect = null;
            switch (currentFilter.getActivityType()) {
                case "Sports": checkboxToSelect = cbSports; break;
                case "Music": checkboxToSelect = cbMusic; break;
                case "Arts": checkboxToSelect = cbArts; break;
                case "Tech": checkboxToSelect = cbTech; break;
                case "Educational": checkboxToSelect = cbEducational; break;
                case "Workshops": checkboxToSelect = cbWorkshops; break;
                case "Other": checkboxToSelect = cbOther; break;
            }
            if (checkboxToSelect != null) {
                checkboxToSelect.setChecked(true);
            }
        }

        // Set dates
        if (currentFilter.getStartDate() != null) {
            editStartDate.setText(currentFilter.getStartDate());
        }
        if (currentFilter.getEndDate() != null) {
            editEndDate.setText(currentFilter.getEndDate());
        }

        // Set prices
        if (currentFilter.getMinPrice() != null) {
            editMinPrice.setText(String.valueOf(currentFilter.getMinPrice()));
        }
        if (currentFilter.getMaxPrice() != null) {
            editMaxPrice.setText(String.valueOf(currentFilter.getMaxPrice()));
        }

        // Set location
        if (currentFilter.getLocation() != null) {
            editLocation.setText(currentFilter.getLocation());
        }
    }

    private void setupListeners() {
        // Setup category checkbox listeners to ensure only one is selected at a time
        CheckBox.OnCheckedChangeListener categoryCheckListener = (buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck all other checkboxes
                if (buttonView != cbSports) cbSports.setChecked(false);
                if (buttonView != cbMusic) cbMusic.setChecked(false);
                if (buttonView != cbArts) cbArts.setChecked(false);
                if (buttonView != cbTech) cbTech.setChecked(false);
                if (buttonView != cbEducational) cbEducational.setChecked(false);
                if (buttonView != cbWorkshops) cbWorkshops.setChecked(false);
                if (buttonView != cbOther) cbOther.setChecked(false);
            }
        };

        cbSports.setOnCheckedChangeListener(categoryCheckListener);
        cbMusic.setOnCheckedChangeListener(categoryCheckListener);
        cbArts.setOnCheckedChangeListener(categoryCheckListener);
        cbTech.setOnCheckedChangeListener(categoryCheckListener);
        cbEducational.setOnCheckedChangeListener(categoryCheckListener);
        cbWorkshops.setOnCheckedChangeListener(categoryCheckListener);
        cbOther.setOnCheckedChangeListener(categoryCheckListener);

        // Date pickers
        editStartDate.setOnClickListener(v -> showDatePicker(editStartDate));
        editEndDate.setOnClickListener(v -> showDatePicker(editEndDate));

        // Apply button
        btnApplyFilters.setOnClickListener(v -> {
            applyFilters();
            dismiss();
        });

        // Clear button
        btnClearFilters.setOnClickListener(v -> {
            clearFilters();
            if (listener != null) {
                listener.onFiltersApplied(new EventFilter()); // Empty filter
            }
            dismiss();
        });
    }

    private void showDatePicker(EditText dateField) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, selectedYear, selectedMonth, selectedDay) -> {
                // Format: YYYY-MM-DD
                String date = String.format("%04d-%02d-%02d",
                    selectedYear, selectedMonth + 1, selectedDay);
                dateField.setText(date);
            },
            year, month, day
        );

        datePickerDialog.show();
    }

    private void applyFilters() {
        EventFilter filter = new EventFilter();

        // Get activity type from selected checkbox
        if (cbSports.isChecked()) {
            filter.setActivityType("Sports");
        } else if (cbMusic.isChecked()) {
            filter.setActivityType("Music");
        } else if (cbArts.isChecked()) {
            filter.setActivityType("Arts");
        } else if (cbTech.isChecked()) {
            filter.setActivityType("Tech");
        } else if (cbEducational.isChecked()) {
            filter.setActivityType("Educational");
        } else if (cbWorkshops.isChecked()) {
            filter.setActivityType("Workshops");
        } else if (cbOther.isChecked()) {
            filter.setActivityType("Other");
        }
        // If no checkbox is checked, don't set a category filter (shows all categories)

        // Get dates
        String startDate = editStartDate.getText().toString().trim();
        if (!startDate.isEmpty()) {
            filter.setStartDate(startDate);
        }

        String endDate = editEndDate.getText().toString().trim();
        if (!endDate.isEmpty()) {
            filter.setEndDate(endDate);
        }

        // Get prices
        String minPriceStr = editMinPrice.getText().toString().trim();
        if (!minPriceStr.isEmpty()) {
            try {
                filter.setMinPrice(Double.parseDouble(minPriceStr));
            } catch (NumberFormatException e) {
                // Invalid number, ignore
            }
        }

        String maxPriceStr = editMaxPrice.getText().toString().trim();
        if (!maxPriceStr.isEmpty()) {
            try {
                filter.setMaxPrice(Double.parseDouble(maxPriceStr));
            } catch (NumberFormatException e) {
                // Invalid number, ignore
            }
        }

        // Get location
        String location = editLocation.getText().toString().trim();
        if (!location.isEmpty()) {
            filter.setLocation(location);
        }

        // Notify listener
        if (listener != null) {
            listener.onFiltersApplied(filter);
        }
    }

    private void clearFilters() {
        // Uncheck all category checkboxes
        cbSports.setChecked(false);
        cbMusic.setChecked(false);
        cbArts.setChecked(false);
        cbTech.setChecked(false);
        cbEducational.setChecked(false);
        cbWorkshops.setChecked(false);
        cbOther.setChecked(false);

        editStartDate.setText("");
        editEndDate.setText("");
        editMinPrice.setText("");
        editMaxPrice.setText("");
        editLocation.setText("");
    }
}

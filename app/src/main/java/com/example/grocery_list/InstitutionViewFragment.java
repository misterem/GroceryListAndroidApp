package com.example.grocery_list;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.Context;

public class InstitutionViewFragment extends Fragment {

    private Spinner institutionSpinner;
    private ImageButton addInstitutionButton;
    private Button scanButton;
    private RecyclerView groceryRecyclerView;
    private GroceryItemAdapter groceryItemAdapter;
    private ArrayAdapter<String> spinnerAdapter;

    private MainActivity mainActivity;
    private List<DisplayGroceryItem> currentDisplayItemsInFragment = new ArrayList<>();

    public InstitutionViewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_institution_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        institutionSpinner = view.findViewById(R.id.institution_spinner_fragment);
        addInstitutionButton = view.findViewById(R.id.add_institution_button_fragment);
        scanButton = view.findViewById(R.id.scan_button_fragment);
        groceryRecyclerView = view.findViewById(R.id.item_list_fragment);

        if (mainActivity == null) {
            Toast.makeText(getContext(), "Error: Fragment not properly attached to MainActivity.", Toast.LENGTH_LONG).show();
            return;
        }

        // Setup Spinner
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, mainActivity.institutions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        institutionSpinner.setAdapter(spinnerAdapter);
        institutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mainActivity.institutions.isEmpty()) {
                    String selectedInstitution = mainActivity.institutions.get(position);
                    updateRecyclerViewForInstitution(selectedInstitution);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentDisplayItemsInFragment.clear();
                if (groceryItemAdapter != null) groceryItemAdapter.updateGroceryItems(currentDisplayItemsInFragment);
            }
        });

        // Setup RecyclerView
        groceryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        groceryItemAdapter = new GroceryItemAdapter(currentDisplayItemsInFragment, (item, position) -> {
            String selectedInstitution = getSelectedInstitution();
            if (selectedInstitution != null && item.getGroceryItem() != null) {
                mainActivity.showEditQuantityDialog(item, selectedInstitution);
            }
        });
        groceryRecyclerView.setAdapter(groceryItemAdapter);

        // Setup Button Listeners
        addInstitutionButton.setOnClickListener(v -> mainActivity.showAddInstitutionDialog());
        scanButton.setOnClickListener(v -> {
            if (getSelectedInstitution() == null && mainActivity.institutions.isEmpty()){
                Toast.makeText(getContext(), "Please add an institution first.", Toast.LENGTH_LONG).show();
                return;
            }
            if (getSelectedInstitution() == null){
                 Toast.makeText(getContext(), "Please select an institution.", Toast.LENGTH_LONG).show();
                 return;
            }
            mainActivity.initiateBarcodeScan("ADD_ITEM"); // Use constant from MainActivity if available
        });

        // Initial load for spinner and recycler view
        if (!mainActivity.institutions.isEmpty()) {
            institutionSpinner.setSelection(0);
            updateRecyclerViewForInstitution(mainActivity.institutions.get(0));
        } else {
            updateRecyclerViewForInstitution(null);
        }
    }

    public String getSelectedInstitution() {
        if (institutionSpinner != null && institutionSpinner.getSelectedItem() != null) {
            return (String) institutionSpinner.getSelectedItem();
        } else if (!mainActivity.institutions.isEmpty()){
            // Fallback if spinner not fully initialized but institutions exist (e.g. during very initial load)
            return mainActivity.institutions.get(0); 
        }
        return null;
    }

    public void updateRecyclerViewForInstitution(String institutionName) {
        if (mainActivity != null) {
            currentDisplayItemsInFragment.clear();
            currentDisplayItemsInFragment.addAll(mainActivity.getCurrentDisplayItemsForInstitution(institutionName));
            if (groceryItemAdapter != null) {
                groceryItemAdapter.updateGroceryItems(currentDisplayItemsInFragment);
            }
        }
    }
    
    // Called by MainActivity after a new institution is added
    public void updateSpinner(String newlyAddedInstitution) {
        if (spinnerAdapter != null) {
            spinnerAdapter.notifyDataSetChanged(); // Refreshes the spinner data
            if (newlyAddedInstitution != null && mainActivity.institutions.contains(newlyAddedInstitution)){
                 institutionSpinner.setSelection(mainActivity.institutions.indexOf(newlyAddedInstitution));
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null; // Avoid memory leaks
    }
}

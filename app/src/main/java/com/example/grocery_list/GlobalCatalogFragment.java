package com.example.grocery_list;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GlobalCatalogFragment extends Fragment implements GlobalItemAdapter.OnGlobalItemClickListener {

    private static final String TAG = "GlobalCatalogFragment";

    private RecyclerView globalItemRecyclerView;
    private GlobalItemAdapter globalItemAdapter;
    private EditText searchEditText;
    private ImageButton scanSearchButton;

    private MainActivity mainActivity;
    private List<GroceryItem> allGlobalItems = new ArrayList<>();
    private List<GroceryItem> filteredGlobalItems = new ArrayList<>();

    public GlobalCatalogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Log.e(TAG, "Fragment must be attached to MainActivity");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_global_catalog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        globalItemRecyclerView = view.findViewById(R.id.global_item_list_recycler_view);
        searchEditText = view.findViewById(R.id.search_edit_text_global);
        scanSearchButton = view.findViewById(R.id.scan_search_button_global);

        if (mainActivity == null) {
            Toast.makeText(getContext(), "Error initializing fragment. MainActivity not found.", Toast.LENGTH_LONG).show();
            return;
        }

        setupRecyclerView();
        loadAndDisplayGlobalItems();
        setupSearchListeners();
    }

    private void setupRecyclerView() {
        globalItemRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        globalItemAdapter = new GlobalItemAdapter(filteredGlobalItems, this);
        globalItemRecyclerView.setAdapter(globalItemAdapter);
    }

    private void loadAndDisplayGlobalItems() {
        allGlobalItems.clear();
        if (mainActivity != null && mainActivity.globalItemCatalog != null) {
            allGlobalItems.addAll(new ArrayList<>(mainActivity.globalItemCatalog.values()));
            Collections.sort(allGlobalItems, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        }
        filterGlobalItems(searchEditText.getText().toString()); // Apply current search query
    }

    private void setupSearchListeners() {
        scanSearchButton.setOnClickListener(v -> {
            if (mainActivity != null) {
                mainActivity.initiateBarcodeScan("SEARCH_ITEM");
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterGlobalItems(s.toString());
            }
        });
    }

    private void filterGlobalItems(String query) {
        filteredGlobalItems.clear();
        String lowerCaseQuery = query.toLowerCase(Locale.getDefault());

        if (lowerCaseQuery.isEmpty()) {
            filteredGlobalItems.addAll(allGlobalItems);
        } else {
            for (GroceryItem item : allGlobalItems) {
                if (item.getName().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    item.getBarcodeValue().contains(query)) {
                    filteredGlobalItems.add(item);
                }
            }
        }
        if (globalItemAdapter != null) {
            globalItemAdapter.updateGlobalItems(filteredGlobalItems);
        }
    }

    @Override
    public void onGlobalItemClick(GroceryItem item) {
        if (mainActivity != null) {
            mainActivity.showEditOrDeleteGlobalItemDialog(item);
        }
    }

    public void searchByBarcode(String barcodeValue) {
        Log.d(TAG, "searchByBarcode called with: " + barcodeValue);
        filteredGlobalItems.clear();
        boolean found = false;
        for (GroceryItem item : allGlobalItems) {
            if (item.getBarcodeValue().equals(barcodeValue)) {
                filteredGlobalItems.add(item);
                found = true;
                break; 
            }
        }

        if (globalItemAdapter != null) {
            globalItemAdapter.updateGlobalItems(filteredGlobalItems);
        }

        if (getContext() != null) {
            if (found) {
                searchEditText.setText(barcodeValue);
                Toast.makeText(getContext(), "Item found for barcode: " + barcodeValue, Toast.LENGTH_SHORT).show();
            } else {
                searchEditText.setText(barcodeValue); 
                Toast.makeText(getContext(), "No item found for barcode: " + barcodeValue, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    public void refreshGlobalItemsDisplay(){
        loadAndDisplayGlobalItems();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null; 
    }
}

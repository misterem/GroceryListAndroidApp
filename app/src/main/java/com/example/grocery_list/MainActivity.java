package com.example.grocery_list;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    public List<String> institutions = new ArrayList<>();
    public Map<String, GroceryItem> globalItemCatalog = new HashMap<>();
    public Map<String, Map<String, Integer>> institutionItemLists = new HashMap<>();
    public List<DisplayGroceryItem> currentDisplayItemsForInstitutionView = new ArrayList<>();

    private BarcodeScanner barcodeScanner;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri temporaryImageUri;
    private String currentBarcodeScanContext;
    private static final String SCAN_CONTEXT_ADD_ITEM = "ADD_ITEM";
    private static final String SCAN_CONTEXT_SEARCH_ITEM = "SEARCH_ITEM";

    private static final String PREFS_NAME = "grocery_list_prefs";
    private static final String KEY_INSTITUTIONS = "institutions_list";
    private static final String KEY_GLOBAL_CATALOG = "global_item_catalog";
    private static final String KEY_INSTITUTION_LISTS_PREFIX = "institution_items_";
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadInstitutions();
        loadGlobalItemCatalog();
        loadInstitutionItemLists();

        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_institutions) {
                selectedFragment = new InstitutionViewFragment();
            } else if (itemId == R.id.navigation_global_catalog) {
                selectedFragment = new GlobalCatalogFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_institutions);
            loadFragment(new InstitutionViewFragment());
        }

        barcodeScanner = BarcodeScanning.getClient();
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && temporaryImageUri != null) {
                    processImageUri(temporaryImageUri);
                } else if (isSuccess) {
                    Log.e(TAG, "Image capture successful but temporaryImageUri is null.");
                    Toast.makeText(this, "Error: Image URI not available after capture.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Image capture failed or cancelled", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_container, fragment)
                .commit();
    }

    public void initiateBarcodeScan(String context) {
        this.currentBarcodeScanContext = context;
        requestCameraPermissionAndLaunchCamera();
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void requestCameraPermissionAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                File photoFile = createImageFile();
                temporaryImageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                cameraLauncher.launch(temporaryImageUri);
            } catch (IOException ex) {
                Log.e(TAG, "Error creating file for camera: " + ex.getMessage(), ex);
                Toast.makeText(this, "Error preparing camera.", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] p, @NonNull int[] gr) {
        super.onRequestPermissionsResult(rc, p, gr);
        if (rc == CAMERA_PERMISSION_REQUEST_CODE && gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionAndLaunchCamera();
        } else if (rc == CAMERA_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show();
        }
    }

    private void processImageUri(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No barcodes found", Toast.LENGTH_SHORT).show();
                    } else {
                        String rawValue = barcodes.get(0).getRawValue();
                        if (rawValue != null && !rawValue.isEmpty()) {
                            if (SCAN_CONTEXT_ADD_ITEM.equals(currentBarcodeScanContext)) {
                                handleScannedBarcodeForAddItem(rawValue);
                            } else if (SCAN_CONTEXT_SEARCH_ITEM.equals(currentBarcodeScanContext)) {
                                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
                                if (currentFragment instanceof GlobalCatalogFragment) {
                                    ((GlobalCatalogFragment) currentFragment).searchByBarcode(rawValue);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Barcode scanning failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    public void handleScannedBarcodeForAddItem(String barcodeValue) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
        String selectedInstitution = null;
        if (currentFragment instanceof InstitutionViewFragment) {
            selectedInstitution = ((InstitutionViewFragment) currentFragment).getSelectedInstitution();
        }

        if (selectedInstitution == null) {
             Toast.makeText(this, "Please select an institution first.", Toast.LENGTH_LONG).show();
             return;
        }

        if (globalItemCatalog.containsKey(barcodeValue)) {
            GroceryItem item = globalItemCatalog.get(barcodeValue);
            showAddItemConfirmationDialog(item, selectedInstitution);
        } else {
            showAddItemDetailsDialog(barcodeValue, selectedInstitution, true);
        }
    }

    // Overload for adding a new item, where after saving to global it proceeds to confirmation dialog
    public void showAddItemDetailsDialog(String barcodeValue, String selectedInstitution, boolean proceedToConfirmation) {
        showItemDetailsDialog(null, barcodeValue, selectedInstitution, proceedToConfirmation);
    }

    // Unified dialog for adding new or editing existing global item
    private void showItemDetailsDialog(GroceryItem existingItem, String barcodeForNewItem, String selectedInstitution, boolean proceedToConfirmationAfterSave) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_item_details, null);
        builder.setView(dialogView);

        final EditText nameInput = dialogView.findViewById(R.id.item_name_input_detail);
        final EditText priceInput = dialogView.findViewById(R.id.item_price_input_detail);
        final EditText noteInput = dialogView.findViewById(R.id.item_note_input_detail);
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        String originalBarcode = barcodeForNewItem;
        if (existingItem != null) {
            builder.setTitle("Edit Item Details");
            nameInput.setText(existingItem.getName());
            priceInput.setText(existingItem.getPrice()); // Keep as string for direct editing
            noteInput.setText(existingItem.getNote());
            originalBarcode = existingItem.getBarcodeValue();
        } else {
            builder.setTitle("Add New Item Details");
        }

        final String finalOriginalBarcode = originalBarcode; // For use in lambda

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String note = noteInput.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Item name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!priceStr.matches("\\d*\\.?\\d*")) { 
                 Toast.makeText(this, "Invalid price format.", Toast.LENGTH_SHORT).show();
                 return;
            }

            GroceryItem itemToSave = new GroceryItem(finalOriginalBarcode, name, priceStr, note);
            globalItemCatalog.put(finalOriginalBarcode, itemToSave);
            saveGlobalItemCatalog();
            Toast.makeText(this, name + (existingItem == null ? " saved to global catalog." : " updated."), Toast.LENGTH_SHORT).show();

            refreshGlobalCatalogFragment();
            
            if (proceedToConfirmationAfterSave && selectedInstitution != null) {
                showAddItemConfirmationDialog(itemToSave, selectedInstitution);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        // Only show delete button if it's an existing item
        if (existingItem != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> {
                showDeleteGlobalItemConfirmationDialog(existingItem);
            });
        }
        builder.show();
    }

    public void showEditOrDeleteGlobalItemDialog(GroceryItem item) {
        showItemDetailsDialog(item, null, null, false);
    }

    private void showDeleteGlobalItemConfirmationDialog(GroceryItem itemToDelete) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '" + itemToDelete.getName() + "' from the global catalog? This will also remove it from all institution lists.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteGlobalItem(itemToDelete);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteGlobalItem(GroceryItem itemToDelete) {
        if (itemToDelete == null) return;
        String barcode = itemToDelete.getBarcodeValue();
        globalItemCatalog.remove(barcode);
        saveGlobalItemCatalog();

        // Remove from all institution lists
        boolean institutionListModified = false;
        for (Map<String, Integer> institutionList : institutionItemLists.values()) {
            if (institutionList.containsKey(barcode)) {
                institutionList.remove(barcode);
                institutionListModified = true;
            }
        }
        if (institutionListModified) {
            saveInstitutionItemLists();
        }

        Toast.makeText(this, itemToDelete.getName() + " deleted globally and from all lists.", Toast.LENGTH_SHORT).show();
        refreshGlobalCatalogFragment();
        refreshCurrentInstitutionViewFragment(); 
    }

    private void refreshGlobalCatalogFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
        if (currentFragment instanceof GlobalCatalogFragment) {
            ((GlobalCatalogFragment) currentFragment).refreshGlobalItemsDisplay();
        }
    }

    private void refreshCurrentInstitutionViewFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
        if (currentFragment instanceof InstitutionViewFragment) {
            ((InstitutionViewFragment) currentFragment).updateRecyclerViewForInstitution(((InstitutionViewFragment) currentFragment).getSelectedInstitution());
        }
    }

    public void showAddItemConfirmationDialog(GroceryItem item, String selectedInstitution) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add/Update Item Quantity");

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_quantity_picker, null);
        builder.setView(dialogView);

        final TextView messageTextView = dialogView.findViewById(R.id.dialog_quantity_message);
        final TextView unitPriceTextView = dialogView.findViewById(R.id.dialog_unit_price_message);
        final EditText quantityEditText = dialogView.findViewById(R.id.edit_text_quantity);
        final Button minusButton = dialogView.findViewById(R.id.button_minus_quantity);
        final Button plusButton = dialogView.findViewById(R.id.button_plus_quantity);

        messageTextView.setText(String.format(Locale.getDefault(), "Item: %s\nInstitution: %s\nAdd quantity:", item.getName(), selectedInstitution));
        unitPriceTextView.setText(String.format(Locale.getDefault(), "Unit Price: %s", item.getFormattedUnitPrice()));
        quantityEditText.setText("1"); 
        quantityEditText.setSelection(quantityEditText.getText().length());

        minusButton.setOnClickListener(v -> {
            try {
                int currentVal = Integer.parseInt(quantityEditText.getText().toString());
                if (currentVal > 1) { 
                    quantityEditText.setText(String.valueOf(currentVal - 1));
                }
            } catch (NumberFormatException e) {
                quantityEditText.setText("1");
            }
        });

        plusButton.setOnClickListener(v -> {
            try {
                int currentVal = Integer.parseInt(quantityEditText.getText().toString());
                quantityEditText.setText(String.valueOf(currentVal + 1));
            } catch (NumberFormatException e) {
                quantityEditText.setText("1");
            }
        });

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String quantityStr = quantityEditText.getText().toString().trim();
            int quantityToAdd;
            try {
                quantityToAdd = Integer.parseInt(quantityStr);
                if (quantityToAdd <= 0) {
                    Toast.makeText(this, "Quantity to add must be positive.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Integer> itemsForInst = institutionItemLists.getOrDefault(selectedInstitution, new HashMap<>());
            int existingQuantity = itemsForInst.getOrDefault(item.getBarcodeValue(), 0);
            int newTotalQuantity = existingQuantity + quantityToAdd;

            itemsForInst.put(item.getBarcodeValue(), newTotalQuantity);
            institutionItemLists.put(selectedInstitution, itemsForInst);
            saveInstitutionItemLists();
            
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
            if (currentFragment instanceof InstitutionViewFragment) {
                ((InstitutionViewFragment) currentFragment).updateRecyclerViewForInstitution(selectedInstitution);
            }
            Toast.makeText(this, String.format(Locale.getDefault(), "%s (Total Qty: %d) updated for %s", item.getName(), newTotalQuantity, selectedInstitution), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public void showEditQuantityDialog(DisplayGroceryItem displayItem, String selectedInstitution) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Item Quantity");
        GroceryItem item = displayItem.getGroceryItem();
        if (item == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_quantity_picker, null);
        builder.setView(dialogView);

        final TextView messageTextView = dialogView.findViewById(R.id.dialog_quantity_message);
        final TextView unitPriceTextView = dialogView.findViewById(R.id.dialog_unit_price_message);
        final EditText quantityEditText = dialogView.findViewById(R.id.edit_text_quantity);
        final Button minusButton = dialogView.findViewById(R.id.button_minus_quantity);
        final Button plusButton = dialogView.findViewById(R.id.button_plus_quantity);

        messageTextView.setText(String.format(Locale.getDefault(), "Item: %s\nInstitution: %s\nSet new total quantity:", item.getName(), selectedInstitution));
        unitPriceTextView.setText(String.format(Locale.getDefault(), "Unit Price: %s", item.getFormattedUnitPrice()));
        quantityEditText.setText(String.valueOf(displayItem.getQuantity()));
        quantityEditText.setSelection(quantityEditText.getText().length());

        minusButton.setOnClickListener(v -> {
            try {
                int currentVal = Integer.parseInt(quantityEditText.getText().toString());
                if (currentVal > 0) { 
                    quantityEditText.setText(String.valueOf(currentVal - 1));
                }
            } catch (NumberFormatException e) {
                quantityEditText.setText(String.valueOf(displayItem.getQuantity()));
            }
        });

        plusButton.setOnClickListener(v -> {
            try {
                int currentVal = Integer.parseInt(quantityEditText.getText().toString());
                quantityEditText.setText(String.valueOf(currentVal + 1));
            } catch (NumberFormatException e) {
                quantityEditText.setText(String.valueOf(displayItem.getQuantity())); 
            }
        });

        builder.setPositiveButton("Update", (dialog, which) -> {
            String quantityStr = quantityEditText.getText().toString().trim();
            int newQuantity;
            try {
                newQuantity = Integer.parseInt(quantityStr);
                if (newQuantity < 0) {
                    Toast.makeText(this, "Quantity cannot be negative.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Integer> itemsForInst = institutionItemLists.getOrDefault(selectedInstitution, new HashMap<>());
            if (newQuantity == 0) {
                itemsForInst.remove(item.getBarcodeValue());
                Toast.makeText(this, item.getName() + " removed from " + selectedInstitution, Toast.LENGTH_SHORT).show();
            } else {
                itemsForInst.put(item.getBarcodeValue(), newQuantity);
                Toast.makeText(this, String.format(Locale.getDefault(), "%s (New Qty: %d) updated for %s", item.getName(), newQuantity, selectedInstitution), Toast.LENGTH_SHORT).show();
            }
            
            if (itemsForInst.isEmpty()) {
                institutionItemLists.remove(selectedInstitution);
            } else {
                institutionItemLists.put(selectedInstitution, itemsForInst);
            }

            saveInstitutionItemLists();
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
            if (currentFragment instanceof InstitutionViewFragment) {
                ((InstitutionViewFragment) currentFragment).updateRecyclerViewForInstitution(selectedInstitution);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    public void showAddInstitutionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Institution");

        final EditText input = new EditText(this);
        input.setHint("Enter institution name");
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        input.setPadding(40,40,40,40); // dp will be converted, but direct px value is better set via resources if possible
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String institutionName = input.getText().toString().trim();
            if (!institutionName.isEmpty()) {
                if (!institutions.contains(institutionName)) {
                    institutions.add(institutionName);
                    Collections.sort(institutions);
                    saveInstitutions();
                    institutionItemLists.putIfAbsent(institutionName, new HashMap<>());
                    saveInstitutionItemLists();
                    
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
                    if (currentFragment instanceof InstitutionViewFragment) {
                        ((InstitutionViewFragment) currentFragment).updateSpinner(institutionName);
                    }
                    Toast.makeText(MainActivity.this, institutionName + " added.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Institution already exists", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Institution name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- Data Persistence Methods ---
    public void saveInstitutions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_INSTITUTIONS, new HashSet<>(institutions));
        editor.apply();
    }

    public void loadInstitutions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> savedInstitutions = prefs.getStringSet(KEY_INSTITUTIONS, null);
        institutions.clear();
        if (savedInstitutions != null) {
            institutions.addAll(savedInstitutions);
            Collections.sort(institutions);
        }
    }

    public void saveGlobalItemCatalog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONObject jsonCatalog = new JSONObject();
        for (Map.Entry<String, GroceryItem> entry : globalItemCatalog.entrySet()) {
            try {
                JSONObject itemJson = new JSONObject();
                itemJson.put("name", entry.getValue().getName());
                itemJson.put("price", entry.getValue().getPrice()); // Save the raw string price
                itemJson.put("note", entry.getValue().getNote());
                itemJson.put("barcodeValue", entry.getValue().getBarcodeValue());
                jsonCatalog.put(entry.getKey(), itemJson.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Error saving item to JSON: " + entry.getKey(), e);
            }
        }
        editor.putString(KEY_GLOBAL_CATALOG, jsonCatalog.toString());
        editor.apply();
    }

    public void loadGlobalItemCatalog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_GLOBAL_CATALOG, null);
        globalItemCatalog.clear();
        if (jsonString != null) {
            try {
                JSONObject jsonCatalog = new JSONObject(jsonString);
                Iterator<String> keys = jsonCatalog.keys();
                while (keys.hasNext()) {
                    String barcode = keys.next();
                    JSONObject itemJson = new JSONObject(jsonCatalog.getString(barcode));
                    GroceryItem item = new GroceryItem(
                        itemJson.getString("barcodeValue"),
                        itemJson.getString("name"),
                        itemJson.getString("price"),
                        itemJson.getString("note")
                    );
                    globalItemCatalog.put(barcode, item);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error loading global item catalog from JSON", e);
            }
        }
    }

    public void saveInstitutionItemLists() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> currentPrefKeysForInstitutionLists = new HashSet<>();
        for (String institutionName : institutions) {
            currentPrefKeysForInstitutionLists.add(KEY_INSTITUTION_LISTS_PREFIX + institutionName);
        }

        for (Map.Entry<String, Map<String, Integer>> entry : institutionItemLists.entrySet()) {
            String prefKey = KEY_INSTITUTION_LISTS_PREFIX + entry.getKey();
            if (institutions.contains(entry.getKey())) { 
                JSONObject institutionMapJson = new JSONObject(entry.getValue());
                editor.putString(prefKey, institutionMapJson.toString());
            } else {
                 editor.remove(prefKey); 
            }
        }

        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(KEY_INSTITUTION_LISTS_PREFIX)) {
                if (!currentPrefKeysForInstitutionLists.contains(entry.getKey())) {
                    editor.remove(entry.getKey());
                }
            }
        }
        editor.apply();
    }

    public void loadInstitutionItemLists() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        institutionItemLists.clear();
        for (String institutionName : institutions) { 
            String jsonString = prefs.getString(KEY_INSTITUTION_LISTS_PREFIX + institutionName, null);
            Map<String, Integer> itemsMap = new HashMap<>();
            if (jsonString != null) {
                try {
                    JSONObject institutionMapJson = new JSONObject(jsonString);
                    Iterator<String> barcodes = institutionMapJson.keys();
                    while (barcodes.hasNext()) {
                        String barcode = barcodes.next();
                        itemsMap.put(barcode, institutionMapJson.getInt(barcode));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error loading item list for " + institutionName, e);
                }
            }
            institutionItemLists.put(institutionName, itemsMap);
        }
    }
    
    public List<DisplayGroceryItem> getCurrentDisplayItemsForInstitution(String institutionName) {
        currentDisplayItemsForInstitutionView.clear();
        if (institutionName != null && institutionItemLists.containsKey(institutionName)) {
            Map<String, Integer> itemsForInstitution = institutionItemLists.get(institutionName);
            if (itemsForInstitution != null) {
                List<DisplayGroceryItem> unsortedItems = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : itemsForInstitution.entrySet()) {
                    String barcode = entry.getKey();
                    int quantity = entry.getValue();
                    if (globalItemCatalog.containsKey(barcode)) {
                        GroceryItem itemDetails = globalItemCatalog.get(barcode);
                        unsortedItems.add(new DisplayGroceryItem(itemDetails, quantity));
                    }
                }
                Collections.sort(unsortedItems, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                currentDisplayItemsForInstitutionView.addAll(unsortedItems);
            }
        }
        return currentDisplayItemsForInstitutionView;
    }
}

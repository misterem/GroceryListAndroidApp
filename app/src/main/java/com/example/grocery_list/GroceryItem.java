package com.example.grocery_list;

import android.util.Log;
import java.text.NumberFormat;
import java.util.Locale;

public class GroceryItem {
    private String name;
    private String price; // Stored as a string like "2.99"
    private String note;
    private String barcodeValue;

    // Constructor
    public GroceryItem(String barcodeValue, String name, String price, String note) {
        this.barcodeValue = barcodeValue;
        this.name = name;
        this.price = price;
        this.note = note;
    }

    // Getters
    public String getName() {
        return name;
    }

    // Returns the raw string price, could be empty or unformatted
    public String getPrice() { 
        return price;
    }

    /**
     * Attempts to parse the stored price string into a double.
     * @return The price as a double, or 0.0 if parsing fails or price is empty/null.
     */
    public double getNumericPrice() {
        if (this.price == null || this.price.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String cleanPrice = this.price.replaceAll("[^\\d.]", "");
            if (cleanPrice.isEmpty()) return 0.0;
            return Double.parseDouble(cleanPrice);
        } catch (NumberFormatException e) {
            Log.e("GroceryItem", "Could not parse price string: " + this.price, e);
            return 0.0;
        }
    }

    /**
     * Returns the unit price formatted as a currency string (e.g., "₪2.99").
     * @return Formatted unit price string.
     */
    public String getFormattedUnitPrice() {
        return NumberFormat.getCurrencyInstance(new Locale("he", "IL")).format(getNumericPrice());
    }

    public String getNote() {
        return note;
    }

    public String getBarcodeValue() {
        return barcodeValue;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "GroceryItem{" +
                "name='" + name + '\'' +
                ", price='" + price + '\'' +
                ", note='" + note + '\'' +
                ", barcodeValue='" + barcodeValue + '\'' +
                '}';
    }
}

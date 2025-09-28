package com.example.grocery_list;

import java.text.NumberFormat;
import java.util.Locale;

public class DisplayGroceryItem {
    private GroceryItem groceryItem;
    private int quantity;

    public DisplayGroceryItem(GroceryItem groceryItem, int quantity) {
        this.groceryItem = groceryItem;
        this.quantity = quantity;
    }

    public GroceryItem getGroceryItem() {
        return groceryItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // Delegated methods from GroceryItem for convenience
    public String getName() {
        if (groceryItem == null) return "";
        return groceryItem.getName();
    }

    // Returns the single unit price string, now formatted for ILS (Shekels)
    public String getUnitPriceString() {
        Locale israelLocale = new Locale("he", "IL");
        if (groceryItem == null) {
            return NumberFormat.getCurrencyInstance(israelLocale).format(0.0);
        }
        // Use the GroceryItem's own formatted unit price method for consistency
        return groceryItem.getFormattedUnitPrice(); 
    }

    public String getNote() {
        if (groceryItem == null) return "";
        return groceryItem.getNote();
    }

    public String getBarcodeValue() {
        if (groceryItem == null) return "";
        return groceryItem.getBarcodeValue();
    }

    /**
     * Calculates the total price (unit price * quantity) and formats it as a currency string for ILS (Shekels).
     * @return Formatted total price string (e.g., "₪5.98").
     */
    public String getTotalPriceString() {
        Locale israelLocale = new Locale("he", "IL");
        if (groceryItem == null) {
            return NumberFormat.getCurrencyInstance(israelLocale).format(0.0);
        }
        double unitNumericPrice = groceryItem.getNumericPrice();
        double totalPrice = unitNumericPrice * quantity;
        return NumberFormat.getCurrencyInstance(israelLocale).format(totalPrice);
    }
}

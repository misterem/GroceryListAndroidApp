package com.example.grocery_list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class GroceryItemAdapter extends RecyclerView.Adapter<GroceryItemAdapter.GroceryItemViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DisplayGroceryItem item, int position);
    }

    private List<DisplayGroceryItem> displayGroceryItems;
    private final OnItemClickListener listener;

    // Constructor
    public GroceryItemAdapter(List<DisplayGroceryItem> displayGroceryItems, OnItemClickListener listener) {
        this.displayGroceryItems = displayGroceryItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroceryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_grocery, parent, false);
        return new GroceryItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroceryItemViewHolder holder, int position) {
        DisplayGroceryItem currentItem = displayGroceryItems.get(position);
        holder.nameTextView.setText(currentItem.getName());
        // Use getTotalPriceString() for the price TextView
        holder.priceTextView.setText(currentItem.getTotalPriceString());
        holder.quantityTextView.setText(String.format(Locale.getDefault(), "Qty: %d", currentItem.getQuantity()));

        if (currentItem.getNote() != null && !currentItem.getNote().isEmpty()) {
            holder.noteTextView.setText(currentItem.getNote());
            holder.noteTextView.setVisibility(View.VISIBLE);
        } else {
            holder.noteTextView.setVisibility(View.GONE);
        }

        // Set the click listener for the item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayGroceryItems == null ? 0 : displayGroceryItems.size();
    }

    // Method to update the list of items and refresh the adapter
    public void updateGroceryItems(List<DisplayGroceryItem> newItems) {
        this.displayGroceryItems = newItems;
        notifyDataSetChanged(); // This tells the RecyclerView to re-render
    }

    // ViewHolder class
    static class GroceryItemViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView priceTextView;
        TextView noteTextView;
        TextView quantityTextView;

        public GroceryItemViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.item_name_text_view);
            priceTextView = itemView.findViewById(R.id.item_price_text_view);
            noteTextView = itemView.findViewById(R.id.item_note_text_view);
            quantityTextView = itemView.findViewById(R.id.item_quantity_text_view);
        }
    }
}

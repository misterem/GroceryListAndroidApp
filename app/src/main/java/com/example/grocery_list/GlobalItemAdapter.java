package com.example.grocery_list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class GlobalItemAdapter extends RecyclerView.Adapter<GlobalItemAdapter.GlobalItemViewHolder> {

    private List<GroceryItem> groceryItems;
    private OnGlobalItemClickListener listener;

    // Interface for click events
    public interface OnGlobalItemClickListener {
        void onGlobalItemClick(GroceryItem item);
    }

    public GlobalItemAdapter(List<GroceryItem> groceryItems, OnGlobalItemClickListener listener) {
        this.groceryItems = groceryItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GlobalItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_global_catalog, parent, false);
        return new GlobalItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GlobalItemViewHolder holder, int position) {
        GroceryItem currentItem = groceryItems.get(position);

        holder.nameTextView.setText(currentItem.getName());
        holder.unitPriceTextView.setText(String.format(Locale.getDefault(), "Unit Price: %s", currentItem.getFormattedUnitPrice()));
        holder.barcodeTextView.setText(String.format(Locale.getDefault(), "Barcode: %s", currentItem.getBarcodeValue()));

        if (currentItem.getNote() != null && !currentItem.getNote().isEmpty()) {
            holder.noteTextView.setText(String.format(Locale.getDefault(), "Note: %s", currentItem.getNote()));
            holder.noteTextView.setVisibility(View.VISIBLE);
        } else {
            holder.noteTextView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGlobalItemClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groceryItems == null ? 0 : groceryItems.size();
    }

    public void updateGlobalItems(List<GroceryItem> newItems) {
        this.groceryItems = newItems;
        notifyDataSetChanged();
    }

    static class GlobalItemViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView unitPriceTextView;
        TextView barcodeTextView;
        TextView noteTextView;

        public GlobalItemViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.global_item_name_text_view);
            unitPriceTextView = itemView.findViewById(R.id.global_item_unit_price_text_view);
            barcodeTextView = itemView.findViewById(R.id.global_item_barcode_text_view);
            noteTextView = itemView.findViewById(R.id.global_item_note_text_view);
        }
    }
}

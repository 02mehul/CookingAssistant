package com.example.cookingassistant.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookingassistant.R;
import com.example.cookingassistant.models.Item;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.VH> {

    public interface OnItemClick {
        void onItemClick(Item item);
    }

    private List<Item> data = new ArrayList<>();
    private final OnItemClick listener;

    public ItemAdapter(OnItemClick listener) {
        this.listener = listener;
    }

    public void submit(List<Item> items) {
        data = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = data.get(position);
        h.name.setText(it.name);
        h.qty.setText("x" + it.quantity);
        h.itemView.setOnClickListener(v -> listener.onItemClick(it));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, qty;
        VH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            qty = itemView.findViewById(R.id.txtQty);
        }
    }
}

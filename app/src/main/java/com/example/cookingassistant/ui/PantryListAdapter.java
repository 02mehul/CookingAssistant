package com.example.cookingassistant.ui;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookingassistant.R;
import com.example.cookingassistant.db.Item;

import java.util.ArrayList;
import java.util.List;

public class PantryListAdapter extends RecyclerView.Adapter<PantryListAdapter.VH> {

    // New interface (preferred): edit / delete explicit actions
    public interface Actions {
        void onEdit(Item item);
        void onDelete(Item item);
    }

    // Legacy interface (back-compat with your InventoryActivity)
    public interface OnClick {
        void onItemClick(Item item);
    }

    private final Actions actions;
    private List<Item> data = new ArrayList<>();

    // Preferred constructor
    public PantryListAdapter(Actions actions) {
        this.actions = actions;
    }

    // Legacy constructor: maps "click" to "edit", disables delete
    public PantryListAdapter(OnClick click) {
        this.actions = new Actions() {
            @Override public void onEdit(Item item) { click.onItemClick(item); }
            @Override public void onDelete(Item item) { /* no-op for legacy */ }
        };
    }

    public void submit(List<Item> items) {
        data = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = data.get(position);
        h.name.setText(it.name);
        h.qty.setText("x" + it.quantity);

        // Row tap = edit
        h.itemView.setOnClickListener(v -> actions.onEdit(it));

        // Per-row overflow menu
        h.more.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(v.getContext(), h.more);
            pm.getMenuInflater().inflate(R.menu.row_item_menu, pm.getMenu());
            pm.setOnMenuItemClickListener((MenuItem mi) -> {
                int id = mi.getItemId();
                if (id == R.id.action_edit) {
                    actions.onEdit(it);
                    return true;
                } else if (id == R.id.action_delete) {
                    actions.onDelete(it);
                    return true;
                }
                return false;
            });
            pm.show();
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, qty;
        ImageButton more;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            qty  = itemView.findViewById(R.id.txtQty);
            more = itemView.findViewById(R.id.btnMore);
        }
    }
}

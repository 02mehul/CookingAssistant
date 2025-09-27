package com.example.cookingassistant.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookingassistant.databinding.RowShoppingItemBinding;
import com.example.cookingassistant.db.ShoppingItem;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.VH> {

    public interface Actions {
        void onToggle(ShoppingItem item);
        void onDelete(ShoppingItem item);
    }

    private final Actions actions;
    private final List<ShoppingItem> data = new ArrayList<>();

    public ShoppingListAdapter(Actions actions) { this.actions = actions; }

    public void submit(List<ShoppingItem> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(RowShoppingItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ShoppingItem it = data.get(pos);
        h.b.txtName.setText(it.name);
        String sub = (it.measure != null && !it.measure.isEmpty() ? it.measure : "") +
                (it.quantity > 1 ? "  ×" + it.quantity : "");
        h.b.txtSub.setText(sub.trim());
        h.b.chk.setChecked(it.checked);
        h.b.chk.setOnClickListener(v -> actions.onToggle(it));
        h.b.btnDelete.setOnClickListener(v -> actions.onDelete(it));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final RowShoppingItemBinding b;
        VH(RowShoppingItemBinding b) { super(b.getRoot()); this.b = b; }
    }
}

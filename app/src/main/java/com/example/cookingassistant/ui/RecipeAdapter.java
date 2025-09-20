package com.example.cookingassistant.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cookingassistant.R;
import com.example.cookingassistant.api.model.Meal;
import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.VH> {
    interface OnClick { void onRecipe(Meal m); }
    private final OnClick listener; private List<Meal> data = new ArrayList<>();
    RecipeAdapter(OnClick l){ listener=l; }
    void submit(List<Meal> list){ data=list==null?new ArrayList<>():list; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.row_recipe, p, false);
        return new VH(view);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos){
        Meal m = data.get(pos);
        h.title.setText(m.strMeal); h.category.setText(m.strCategory);
        Glide.with(h.img.getContext()).load(m.strMealThumb).into(h.img);
        h.itemView.setOnClickListener(v -> listener.onRecipe(m));
    }
    @Override public int getItemCount(){ return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img; TextView title, category;
        VH(@NonNull View v){ super(v); img=v.findViewById(R.id.img); title=v.findViewById(R.id.title); category=v.findViewById(R.id.category);}
    }
}

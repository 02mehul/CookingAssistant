package com.example.cookingassistant.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cookingassistant.R;
import com.example.cookingassistant.databinding.FragmentPantryBinding;
import com.example.cookingassistant.db.AppDatabase;
import com.example.cookingassistant.db.Item;
import com.example.cookingassistant.db.ItemDao;

import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.Executors;

public class PantryFragment extends Fragment {

    private FragmentPantryBinding binding;
    private ItemDao dao;
    private PantryListAdapter adapter;

    public PantryFragment() { super(R.layout.fragment_pantry); }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // tell Fragment we have a toolbar menu
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentPantryBinding.bind(view);
        dao = AppDatabase.getInstance(requireContext()).itemDao();

        // Use our toolbar as the ActionBar so fragment menu shows up in it
        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.pantryToolbar);

        // Recycler + adapter (row edit/delete)
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PantryListAdapter(new PantryListAdapter.Actions() {
            @Override public void onEdit(Item item) {
                Intent i = new Intent(requireContext(), ItemEditActivity.class);
                i.putExtra("id", item.id);
                startActivity(i);
            }
            @Override public void onDelete(Item item) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete item")
                        .setMessage("Delete \"" + item.name + "\"?")
                        .setPositiveButton("Delete", (d, w) ->
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    dao.delete(item);
                                    loadItems();
                                }))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        binding.recycler.setAdapter(adapter);

        // Add via FAB and empty-state button → Quick-Add dialog
        binding.fabAdd.setOnClickListener(v -> showQuickAddDialog());
        binding.btnEmptyAdd.setOnClickListener(v -> showQuickAddDialog());

        loadItems(); // initial
    }

    // Inflate toolbar menu (Add + Clear all)
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.pantry_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Handle toolbar clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            showQuickAddDialog();
            return true;
        } else if (id == R.id.action_clear_all) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear all items")
                    .setMessage("Remove every item from your pantry?")
                    .setPositiveButton("Clear", (d, w) ->
                            Executors.newSingleThreadExecutor().execute(() -> {
                                // snapshot before clearing so we can UNDO
                                final List<Item> backup = dao.getAllSync();
                                dao.clearAll();
                                loadItems();
                                requireActivity().runOnUiThread(() ->
                                        Snackbar.make(binding.getRoot(), "Pantry cleared", Snackbar.LENGTH_LONG)
                                                .setAction("UNDO", v -> Executors.newSingleThreadExecutor().execute(() -> {
                                                    if (backup != null) {
                                                        for (Item it : backup) {
                                                            // reinsert basic fields (id will auto-regenerate)
                                                            dao.insert(new Item(it.name, it.quantity));
                                                        }
                                                        loadItems();
                                                    }
                                                }))
                                                .show()
                                );
                            }))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Small dialog to quickly add an item without leaving the screen */
    private void showQuickAddDialog() {
        // Simple vertical form (Name + Quantity)
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(p, p, p, p);

        EditText name = new EditText(requireContext());
        name.setHint("Item name");
        container.addView(name);

        EditText qty = new EditText(requireContext());
        qty.setHint("Quantity");
        qty.setInputType(InputType.TYPE_CLASS_NUMBER);
        container.addView(qty);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add item")
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    String n = name.getText().toString().trim();
                    String q = qty.getText().toString().trim();
                    if (n.isEmpty() || q.isEmpty()) return;
                    int qv;
                    try { qv = Integer.parseInt(q); } catch (Exception e) { return; }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        dao.insert(new Item(n, qv));
                        loadItems();
                        requireActivity().runOnUiThread(() ->
                                Snackbar.make(binding.getRoot(), "Added " + n, Snackbar.LENGTH_SHORT).show()
                        );
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public void onResume() {
        super.onResume();
        loadItems(); // refresh after returning from editor
    }

    private void loadItems() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Item> all = dao.getAllSync();
            requireActivity().runOnUiThread(() -> {
                adapter.submit(all);
                boolean empty = (all == null || all.isEmpty());
                binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.fabAdd.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }
}

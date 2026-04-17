package com.webviewer.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class UrlManagerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UrlAdapter adapter;
    private List<UrlItem> urlList = new ArrayList<>();
    private SharedPreferences prefs;
    private static final String PREF_URL_LIST = "url_list";
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_manager);

        prefs = getSharedPreferences("WebViewerPrefs", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My URLs");
        }

        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fab = findViewById(R.id.fab_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UrlAdapter(urlList, this::onUrlSelected, this::onUrlDelete, this::onUrlEdit);
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getAdapterPosition();
                int to = target.getAdapterPosition();
                UrlItem moved = urlList.remove(from);
                urlList.add(to, moved);
                adapter.notifyItemMoved(from, to);
                saveUrls();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                urlList.remove(pos);
                adapter.notifyItemRemoved(pos);
                saveUrls();
                updateEmptyView();
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);

        fab.setOnClickListener(v -> showAddUrlDialog(null, -1));

        loadUrls();
        updateEmptyView();
    }

    private void onUrlSelected(UrlItem item) {
        Intent result = new Intent();
        result.putExtra("selected_url", item.url);
        setResult(RESULT_OK, result);
        finish();
    }

    private void onUrlDelete(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete URL")
                .setMessage("Remove \"" + urlList.get(position).title + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    urlList.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveUrls();
                    updateEmptyView();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onUrlEdit(int position) {
        showAddUrlDialog(urlList.get(position), position);
    }

    private void showAddUrlDialog(UrlItem existing, int editPosition) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_url, null);
        EditText titleInput = dialogView.findViewById(R.id.input_title);
        EditText urlInput = dialogView.findViewById(R.id.input_url);

        if (existing != null) {
            titleInput.setText(existing.title);
            urlInput.setText(existing.url);
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add URL" : "Edit URL")
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String title = titleInput.getText().toString().trim();
                    String url = urlInput.getText().toString().trim();

                    if (TextUtils.isEmpty(url)) {
                        Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(title)) title = url;
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }

                    if (editPosition >= 0) {
                        urlList.set(editPosition, new UrlItem(title, url));
                        adapter.notifyItemChanged(editPosition);
                    } else {
                        urlList.add(0, new UrlItem(title, url));
                        adapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                    }
                    saveUrls();
                    updateEmptyView();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateEmptyView() {
        if (urlList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void saveUrls() {
        try {
            JSONArray arr = new JSONArray();
            for (UrlItem item : urlList) {
                JSONObject obj = new JSONObject();
                obj.put("title", item.title);
                obj.put("url", item.url);
                arr.put(obj);
            }
            prefs.edit().putString(PREF_URL_LIST, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUrls() {
        try {
            String json = prefs.getString(PREF_URL_LIST, "[]");
            JSONArray arr = new JSONArray(json);
            urlList.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                urlList.add(new UrlItem(obj.getString("title"), obj.getString("url")));
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // --- Data Model ---
    public static class UrlItem {
        public String title, url;
        public UrlItem(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    // --- RecyclerView Adapter ---
    interface OnUrlAction {
        void onAction(int position);
    }

    interface OnUrlClick {
        void onClick(UrlItem item);
    }

    static class UrlAdapter extends RecyclerView.Adapter<UrlAdapter.VH> {
        private final List<UrlItem> items;
        private final OnUrlClick onClick;
        private final OnUrlAction onDelete, onEdit;

        UrlAdapter(List<UrlItem> items, OnUrlClick onClick, OnUrlAction onDelete, OnUrlAction onEdit) {
            this.items = items;
            this.onClick = onClick;
            this.onDelete = onDelete;
            this.onEdit = onEdit;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_url, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            UrlItem item = items.get(pos);
            h.title.setText(item.title);
            h.url.setText(item.url);

            // Generate a color based on domain
            int[] colors = {0xFF6200EE, 0xFF03DAC5, 0xFFE91E63, 0xFF4CAF50, 0xFF2196F3, 0xFFFF9800};
            int colorIdx = Math.abs(item.url.hashCode()) % colors.length;
            h.icon.setBackgroundColor(colors[colorIdx]);
            h.icon.setText(item.title.isEmpty() ? "W" : String.valueOf(item.title.charAt(0)).toUpperCase());

            h.itemView.setOnClickListener(v -> onClick.onClick(item));
            h.btnEdit.setOnClickListener(v -> onEdit.onAction(h.getAdapterPosition()));
            h.btnDelete.setOnClickListener(v -> onDelete.onAction(h.getAdapterPosition()));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, url, icon;
            ImageButton btnEdit, btnDelete;
            VH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.item_title);
                url = v.findViewById(R.id.item_url);
                icon = v.findViewById(R.id.item_icon);
                btnEdit = v.findViewById(R.id.btn_edit);
                btnDelete = v.findViewById(R.id.btn_delete);
            }
        }
    }
}

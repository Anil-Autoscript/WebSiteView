package com.webviewer.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import org.json.*;
import java.util.*;

public class HomeActivity extends AppCompatActivity {

    private List<PageItem> allItems = new ArrayList<>();
    private PageAdapter adapter;
    private RecyclerView recycler;
    private TextView emptyView;
    private String currentFilter = "all"; // all, url, html
    private String currentCat   = "all"; // all, search, social, tools, media, html
    private EditText searchInput;
    private TabLayout mainTabs, catTabs;
    private SharedPreferences prefs;
    private static final String PREF_PAGES = "pages_list";
    private int nextId = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        prefs = getSharedPreferences("WebViewerPrefs", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("WebViewer Pro");
        }

        recycler    = findViewById(R.id.recycler);
        emptyView   = findViewById(R.id.empty_view);
        searchInput = findViewById(R.id.search_input);
        mainTabs    = findViewById(R.id.main_tabs);
        catTabs     = findViewById(R.id.cat_tabs);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddDialog(-1));

        setupRecycler();
        setupTabs();
        setupSearch();

        loadPages();
        seedDefaultsIfEmpty();
        applyFilters();
    }

    private void setupRecycler() {
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        recycler.setLayoutManager(glm);
        adapter = new PageAdapter(item -> openPage(item), item -> showAddDialog(item.id));
        recycler.setAdapter(adapter);
    }

    private void setupTabs() {
        // Main tabs: Browser | HTML
        mainTabs.addTab(mainTabs.newTab().setText("Browser"));
        mainTabs.addTab(mainTabs.newTab().setText("HTML Pages"));
        mainTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab t) {
                currentFilter = t.getPosition() == 0 ? "all" : "html";
                if (t.getPosition() == 1) currentCat = "html";
                else if ("html".equals(currentCat)) currentCat = "all";
                syncCatTabs();
                applyFilters();
            }
            public void onTabUnselected(TabLayout.Tab t) {}
            public void onTabReselected(TabLayout.Tab t) {}
        });

        // Category tabs
        String[] cats = {"all", "search", "social", "tools", "media", "html"};
        String[] labels = {"All", "Search", "Social", "Tools", "Media", "HTML"};
        for (int i = 0; i < cats.length; i++) {
            TabLayout.Tab t = catTabs.newTab().setText(labels[i]).setTag(cats[i]);
            catTabs.addTab(t);
        }
        catTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab t) {
                currentCat = (String) t.getTag();
                if ("html".equals(currentCat)) {
                    currentFilter = "html";
                    mainTabs.getTabAt(1).select();
                }
                applyFilters();
            }
            public void onTabUnselected(TabLayout.Tab t) {}
            public void onTabReselected(TabLayout.Tab t) {}
        });
    }

    private void syncCatTabs() {
        for (int i = 0; i < catTabs.getTabCount(); i++) {
            TabLayout.Tab t = catTabs.getTabAt(i);
            if (t != null && currentCat.equals(t.getTag())) {
                t.select();
                return;
            }
        }
        if (catTabs.getTabAt(0) != null) catTabs.getTabAt(0).select();
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { applyFilters(); }
            public void afterTextChanged(Editable s) {}
        });
        searchInput.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_SEARCH) { applyFilters(); return true; }
            return false;
        });
    }

    private void applyFilters() {
        String q = searchInput.getText().toString().trim().toLowerCase();
        List<PageItem> filtered = new ArrayList<>();
        for (PageItem p : allItems) {
            boolean typeOk = "all".equals(currentFilter)
                    ? !PageItem.TYPE_HTML.equals(p.type)
                    : PageItem.TYPE_HTML.equals(p.type);
            if ("all".equals(currentFilter)) typeOk = true; // show both in browser tab by default
            boolean catOk = "all".equals(currentCat)
                    || currentCat.equals(p.category)
                    || ("html".equals(currentCat) && PageItem.TYPE_HTML.equals(p.type));
            boolean qOk   = q.isEmpty() || p.name.toLowerCase().contains(q);
            if (typeOk && catOk && qOk) filtered.add(p);
        }
        adapter.setItems(filtered);
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openPage(PageItem item) {
        Intent intent = new Intent(this, MainActivity.class);
        if (PageItem.TYPE_HTML.equals(item.type)) {
            intent.putExtra("html_content", item.html);
            intent.putExtra("page_title", item.name);
        } else {
            intent.putExtra("url", item.url);
        }
        startActivity(intent);
    }

    private void showAddDialog(int editId) {
        PageItem existing = editId >= 0 ? findById(editId) : null;

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_page, null);
        EditText nameInput    = v.findViewById(R.id.input_name);
        EditText urlInput     = v.findViewById(R.id.input_url);
        EditText htmlInput    = v.findViewById(R.id.input_html);
        Spinner  catSpinner   = v.findViewById(R.id.spinner_cat);
        RadioGroup typeGroup  = v.findViewById(R.id.type_group);
        LinearLayout urlFields  = v.findViewById(R.id.url_fields);
        LinearLayout htmlFields = v.findViewById(R.id.html_fields);

        String[] catLabels = {"Search", "Social", "Tools", "Media", "HTML"};
        String[] catValues = {"search", "social", "tools", "media", "html"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, catLabels);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        catSpinner.setAdapter(catAdapter);

        if (existing != null) {
            nameInput.setText(existing.name);
            if (PageItem.TYPE_HTML.equals(existing.type)) {
                typeGroup.check(R.id.radio_html);
                htmlInput.setText(existing.html);
                urlFields.setVisibility(View.GONE);
                htmlFields.setVisibility(View.VISIBLE);
            } else {
                typeGroup.check(R.id.radio_url);
                urlInput.setText(existing.url);
            }
            for (int i = 0; i < catValues.length; i++) {
                if (catValues[i].equals(existing.category)) { catSpinner.setSelection(i); break; }
            }
        }

        typeGroup.setOnCheckedChangeListener((g, id) -> {
            boolean isHtml = id == R.id.radio_html;
            urlFields.setVisibility(isHtml ? View.GONE : View.VISIBLE);
            htmlFields.setVisibility(isHtml ? View.VISIBLE : View.GONE);
            if (isHtml) catSpinner.setSelection(4); // html category
        });

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add page" : "Edit page")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show(); return; }
                    String cat = catValues[catSpinner.getSelectedItemPosition()];
                    boolean isHtml = typeGroup.getCheckedRadioButtonId() == R.id.radio_html;

                    if (isHtml) {
                        String html = htmlInput.getText().toString().trim();
                        if (html.isEmpty()) { Toast.makeText(this, "HTML required", Toast.LENGTH_SHORT).show(); return; }
                        if (existing != null) {
                            existing.name = name; existing.html = html; existing.category = "html";
                        } else {
                            allItems.add(0, new PageItem(nextId++, name, PageItem.TYPE_HTML, null, html, "html"));
                        }
                    } else {
                        String url = urlInput.getText().toString().trim();
                        if (url.isEmpty()) { Toast.makeText(this, "URL required", Toast.LENGTH_SHORT).show(); return; }
                        if (!url.startsWith("http")) url = "https://" + url;
                        if (existing != null) {
                            existing.name = name; existing.url = url; existing.category = cat;
                        } else {
                            allItems.add(0, new PageItem(nextId++, name, PageItem.TYPE_URL, url, null, cat));
                        }
                    }
                    savePages();
                    applyFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private PageItem findById(int id) {
        for (PageItem p : allItems) if (p.id == id) return p;
        return null;
    }

    private void savePages() {
        try {
            JSONArray arr = new JSONArray();
            for (PageItem p : allItems) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("name", p.name);
                o.put("type", p.type);
                o.put("url", p.url != null ? p.url : "");
                o.put("html", p.html != null ? p.html : "");
                o.put("category", p.category);
                arr.put(o);
            }
            prefs.edit().putString(PREF_PAGES, arr.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadPages() {
        try {
            String json = prefs.getString(PREF_PAGES, "[]");
            JSONArray arr = new JSONArray(json);
            allItems.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                allItems.add(new PageItem(
                        o.getInt("id"), o.getString("name"), o.getString("type"),
                        o.optString("url", null), o.optString("html", null),
                        o.optString("category", "tools")
                ));
                nextId = Math.max(nextId, o.getInt("id") + 1);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void seedDefaultsIfEmpty() {
        if (!allItems.isEmpty()) return;
        String[][] defaults = {
                {"Google","https://google.com","url","search"},
                {"YouTube","https://youtube.com","url","media"},
                {"GitHub","https://github.com","url","tools"},
                {"Twitter","https://twitter.com","url","social"},
                {"LinkedIn","https://linkedin.com","url","social"},
                {"ChatGPT","https://chat.openai.com","url","tools"},
        };
        for (String[] d : defaults) {
            allItems.add(new PageItem(nextId++, d[0], d[2], d[1], null, d[3]));
        }
        allItems.add(new PageItem(nextId++, "Hello World", PageItem.TYPE_HTML, null,
                "<div style='font-family:system-ui;text-align:center;padding:60px 20px'>"
                + "<h1 style='color:#7C6FFF'>Hello from WebViewer!</h1>"
                + "<p style='color:#888'>This is a custom HTML page.</p></div>",
                "html"));
        savePages();
    }

    // ─── Adapter ───────────────────────────────────────────────────────
    interface OnPageAction { void onAction(PageItem item); }

    static class PageAdapter extends RecyclerView.Adapter<PageAdapter.VH> {
        private List<PageItem> items = new ArrayList<>();
        private final OnPageAction onClick, onLongClick;

        PageAdapter(OnPageAction click, OnPageAction longClick) {
            this.onClick = click; this.onLongClick = longClick;
        }

        void setItems(List<PageItem> list) {
            items = list;
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_page_icon, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            PageItem item = items.get(pos);

            // Icon circle with initial letter
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(item.getIconColor());
            h.iconBg.setBackground(circle);
            h.iconLetter.setText(String.valueOf(item.getInitial()));

            h.name.setText(item.name);
            h.sub.setText(item.getDisplayUrl());

            boolean isHtml = PageItem.TYPE_HTML.equals(item.type);
            h.typeBadge.setText(isHtml ? "HTML" : "URL");
            h.typeBadge.setTextColor(isHtml ? 0xFF00C896 : 0xFF7C6FFF);

            h.itemView.setOnClickListener(v -> onClick.onAction(item));
            h.itemView.setOnLongClickListener(v -> { onLongClick.onAction(item); return true; });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            View iconBg; TextView iconLetter, name, sub, typeBadge;
            VH(View v) {
                super(v);
                iconBg     = v.findViewById(R.id.icon_bg);
                iconLetter = v.findViewById(R.id.icon_letter);
                name       = v.findViewById(R.id.item_name);
                sub        = v.findViewById(R.id.item_sub);
                typeBadge  = v.findViewById(R.id.type_badge);
            }
        }
    }
}

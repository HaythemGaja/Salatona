package com.leo.salah;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Searchable city list + manual coordinates entry. */
public class CityPickerActivity extends Activity {

    private ArrayAdapter<String> adapter;
    private final List<String> shown = new ArrayList<>();
    private final List<String> shownEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 28);
        root.setPadding(pad, Ui.dp(this, 24), pad, pad);
        root.setBackgroundColor(Ui.BG);

        TextView title = Ui.label(this, "Choose your city", 30, Ui.TEXT);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        Button custom = Ui.bigButton(this, "📍 Enter coordinates manually");
        Ui.margin(custom, 0, Ui.dp(this, 16), 0, Ui.dp(this, 8));
        custom.setOnClickListener(v -> customDialog());
        root.addView(custom);

        EditText search = new EditText(this);
        search.setHint("Search city…");
        search.setTextSize(20);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.DIM);
        search.setBackground(Ui.roundBg(Ui.CARD, Ui.dp(this, 14)));
        search.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 14));
        Ui.margin(search, 0, Ui.dp(this, 8), 0, Ui.dp(this, 12));
        root.addView(search);

        ListView lv = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, shown) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Ui.focusTextColors()); // dark text on the gold focused row
                tv.setTextSize(20);
                tv.setPadding(Ui.dp(getContext(), 20), Ui.dp(getContext(), 14),
                        Ui.dp(getContext(), 20), Ui.dp(getContext(), 14));
                return tv;
            }
        };
        lv.setAdapter(adapter);
        Ui.goldListSelector(lv); // focused row turns bright gold — easy to see on TV
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        root.addView(lv, lp);

        filter("");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        lv.setOnItemClickListener((AdapterView<?> parent, View view, int pos, long id) -> {
            String e = shownEntries.get(pos);
            new Prefs(this).setCity(Cities.name(e), Cities.country(e),
                    Cities.lat(e), Cities.lng(e));
            try { AthanScheduler.scheduleAll(this); } catch (Exception ignored) {}
            Toast.makeText(this, "City set: " + Cities.name(e) + " ✓", Toast.LENGTH_SHORT).show();
            finish();
        });

        setContentView(root);
    }

    private void filter(String q) {
        shown.clear();
        shownEntries.clear();
        String ql = q.toLowerCase().trim();
        for (String e : Cities.ALL) {
            if (ql.isEmpty() || Cities.name(e).toLowerCase().contains(ql)
                    || Cities.country(e).toLowerCase().contains(ql)) {
                shown.add(Cities.name(e) + ", " + Cities.country(e));
                shownEntries.add(e);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void customDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        final EditText lat = new EditText(this);
        lat.setHint("Latitude  (e.g. 32.90)");
        lat.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText lng = new EditText(this);
        lng.setHint("Longitude (e.g. 13.18)");
        lng.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(lat);
        box.addView(lng);
        new AlertDialog.Builder(this)
                .setTitle("Custom location")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        double la = Double.parseDouble(lat.getText().toString().trim());
                        double ln = Double.parseDouble(lng.getText().toString().trim());
                        if (Math.abs(la) > 90 || Math.abs(ln) > 180) throw new Exception();
                        new Prefs(this).setCity("Custom", la + ", " + ln, la, ln);
                        try { AthanScheduler.scheduleAll(this); } catch (Exception ignored) {}
                        Toast.makeText(this, "Location saved ✓", Toast.LENGTH_SHORT).show();
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "Enter valid coordinates", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

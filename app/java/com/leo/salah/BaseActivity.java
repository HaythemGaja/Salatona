package com.leo.salah;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;

import java.util.Locale;

/**
 * Base for every Salatona screen: applies the language, digit and night-mode
 * settings before the screen is built, and localises the finished view tree
 * once it has been laid out.
 */
public class BaseActivity extends Activity {

    private int state;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        state = state(this);
        applyDirection(getWindow().getDecorView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        int now = state(this);
        if (now != state) {
            // language, digits, clock format or the palette changed while away
            recreate();
            return;
        }
        final View root = getWindow().getDecorView();
        root.post(() -> {
            applyDirection(root);
            Lang.localize(root);
        });
    }

    private void applyDirection(View root) {
        root.setLayoutDirection(Lang.rtl()
                ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
    }

    private static int state(Activity a) {
        Prefs p = new Prefs(a);
        int h = 17;
        int lang = p.language();
        if (lang == 0) lang = Locale.getDefault().getLanguage().equals("ar") ? 1 : 2;
        h = h * 31 + lang;
        h = h * 31 + p.digits();
        h = h * 31 + p.clockMode();
        boolean h24 = p.clockMode() == 2
                || (p.clockMode() == 0 && DateFormat.is24HourFormat(a));
        h = h * 31 + (h24 ? 1 : 0);
        h = h * 31 + (p.night() ? 1 : 0);
        h = h * 31 + p.hijriOffset();
        return h;
    }
}

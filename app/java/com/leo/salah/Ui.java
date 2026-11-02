package com.leo.salah;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/** Shared TV-friendly styling helpers. No external dependencies. */
public class Ui {
    public static final int BG = 0xFF0E1B16;
    public static final int BG_DEEP = 0xFF091410;
    public static final int CARD = 0xFF16261F;
    public static final int CARD_HI = 0xFF1D3A2C;
    public static final int GOLD = 0xFFD4AF37;
    public static final int GOLD_BRIGHT = 0xFFFFD54F;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int DIM = 0xFF9E9E9E;
    public static final int GREEN = 0xFF66BB6A;
    public static final int DARK_TEXT = 0xFF10231A;

    public static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }

    public static Drawable roundBg(int color, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static Drawable roundBgStroke(int color, int strokeColor, int strokePx, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setStroke(strokePx, strokeColor);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static void margin(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).setMargins(l, t, r, b);
            v.setLayoutParams(lp);
        } else {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    lp != null ? lp.width : ViewGroup.LayoutParams.WRAP_CONTENT,
                    lp != null ? lp.height : ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(l, t, r, b);
            v.setLayoutParams(p);
        }
    }

    /**
     * STRONG focus: focused view gets a bright gold background, dark text and a
     * slight zoom — impossible to miss on a TV. Unfocused restores the given
     * base background and original text colors.
     */
    public static void strongFocus(final View v, final Drawable baseBg, final TextView... texts) {
        strongFocus(v, baseBg, null, texts);
    }

    /** Same, with a callback fired when the view gains focus (for focus bookkeeping). */
    public static void strongFocus(final View v, final Drawable baseBg, final Runnable onFocused,
                                   final TextView... texts) {
        final int[] orig = new int[texts.length];
        for (int i = 0; i < texts.length; i++) orig[i] = texts[i].getCurrentTextColor();
        v.setFocusable(true);
        v.setOnFocusChangeListener((view, has) -> {
            Context c = view.getContext();
            if (has) {
                if (onFocused != null) onFocused.run();
                view.setScaleX(1.03f);
                view.setScaleY(1.03f);
                view.setBackground(roundBgStroke(GOLD_BRIGHT, 0xFFFFFFFF, dp(c, 4), dp(c, 16)));
                for (TextView t : texts) t.setTextColor(DARK_TEXT);
                view.setElevation(dp(c, 8));
            } else {
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setElevation(0);
                view.setBackground(baseBg);
                for (int i = 0; i < texts.length; i++) texts[i].setTextColor(orig[i]);
            }
        });
    }

    /** Big D-pad friendly button with unmissable gold focus highlight. */
    public static Button bigButton(Context c, String text) {
        Button b = new Button(c);
        b.setText(text);
        b.setTextSize(20);
        b.setMinHeight(dp(c, 64));
        b.setPadding(dp(c, 36), dp(c, 8), dp(c, 36), dp(c, 8));
        b.setTextColor(TEXT);
        b.setBackground(roundBg(CARD, dp(c, 14)));
        b.setFocusable(true);
        // swap colors on focus — no reliance on subtle selectors
        b.setOnFocusChangeListener((v, has) -> {
            if (has) {
                v.setScaleX(1.05f); v.setScaleY(1.05f);
                v.setBackground(roundBgStroke(GOLD_BRIGHT, 0xFFFFFFFF, dp(c, 4), dp(c, 14)));
                b.setTextColor(DARK_TEXT);
                v.setElevation(dp(c, 8));
            } else {
                v.setScaleX(1f); v.setScaleY(1f);
                v.setElevation(0);
                v.setBackground(roundBg(CARD, dp(c, 14)));
                b.setTextColor(TEXT);
            }
        });
        return b;
    }

    /** Compact square icon button (e.g. ⚙ gear) with the same unmissable gold focus. */
    public static Button iconButton(Context c, String icon, int textSizeSp) {
        Button b = new Button(c);
        b.setText(icon);
        b.setTextSize(textSizeSp);
        int s = dp(c, 60);
        b.setMinWidth(s);
        b.setMinHeight(s);
        b.setWidth(s);
        b.setHeight(s);
        b.setTextColor(TEXT);
        b.setBackground(roundBg(CARD, dp(c, 16)));
        b.setFocusable(true);
        b.setOnFocusChangeListener((v, has) -> {
            if (has) {
                v.setScaleX(1.08f); v.setScaleY(1.08f);
                v.setBackground(roundBgStroke(GOLD_BRIGHT, 0xFFFFFFFF, dp(c, 4), dp(c, 16)));
                b.setTextColor(DARK_TEXT);
                v.setElevation(dp(c, 8));
            } else {
                v.setScaleX(1f); v.setScaleY(1f);
                v.setElevation(0);
                v.setBackground(roundBg(CARD, dp(c, 16)));
                b.setTextColor(TEXT);
            }
        });
        return b;
    }

    /** Focusable-button background: gold when focused, transparent otherwise. */
    public static Drawable focusBg(Context c, int radiusPx) {
        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_focused},
                roundBg(GOLD_BRIGHT, radiusPx));
        sl.addState(new int[]{android.R.attr.state_pressed},
                roundBg(0xFFB8952F, radiusPx));
        sl.addState(new int[]{}, roundBg(0x00000000, radiusPx));
        return sl;
    }

    /** Gold selector for ListViews so the focused row is obvious. */    public static void goldListSelector(ListView lv) {
        Context c = lv.getContext();
        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_focused},
                roundBg(GOLD_BRIGHT, dp(c, 10)));
        sl.addState(new int[]{android.R.attr.state_pressed},
                roundBg(0xFFB8952F, dp(c, 10)));
        sl.addState(new int[]{android.R.attr.state_selected},
                roundBg(GOLD_BRIGHT, dp(c, 10)));
        sl.addState(new int[]{}, roundBg(0x00000000, dp(c, 10)));
        lv.setSelector(sl);
        lv.setDrawSelectorOnTop(false); // behind the text; text turns dark via focusTextColors()
    }

    /** Text colors for list rows: dark when the row is focused (gold bg), light otherwise. */
    public static android.content.res.ColorStateList focusTextColors() {
        return new android.content.res.ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_focused},
                        new int[]{android.R.attr.state_pressed},
                        new int[]{}
                },
                new int[]{DARK_TEXT, DARK_TEXT, TEXT});
    }

    public static TextView label(Context c, String text, int sizeSp, int color) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        return t;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(roundBg(CARD, dp(c, 18)));
        l.setPadding(dp(c, 28), dp(c, 24), dp(c, 28), dp(c, 24));
        return l;
    }

    public static LinearLayout hrow(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
}

package com.leo.salah;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Qibla direction from the configured city on a satellite map.
 * Uses Esri World Imagery tiles directly (free, no API key) rendered by a
 * tiny self-contained slippy-map script — no Google API, no JS libraries.
 * The map is north-up, so the gold arrow points at the true qibla bearing.
 */
public class QiblaActivity extends BaseActivity {

    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Prefs p = new Prefs(this);
        double bearing = PrayerCalc.qiblaBearing(p.lat(), p.lng());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int pad = Ui.dp(this, 32);
        root.setPadding(pad, Ui.dp(this, 20), pad, Ui.dp(this, 20));

        TextView title = Ui.label(this, "🧭 Qibla", 34, Ui.TEXT);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        FrameLayout mapFrame = new FrameLayout(this);
        mapFrame.setBackground(Ui.roundBg(0xFF0B1E5B, Ui.dp(this, 16)));
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        mlp.setMargins(0, Ui.dp(this, 16), 0, 0);

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                WebResourceResponse cached = cachedTile(request.getUrl().toString());
                return cached != null ? cached
                        : super.shouldInterceptRequest(view, request);
            }
        });
        web.loadDataWithBaseURL(null, mapHtml(p.lat(), p.lng(), bearing),
                "text/html", "UTF-8", null);
        mapFrame.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(mapFrame, mlp);

        TextView note = Ui.label(this, Lang.t(
                "🛰 satellite view · gold arrow points to the Kaaba · map is saved for offline use"),
                16, Ui.DIM);
        note.setGravity(Gravity.CENTER);
        Ui.margin(note, 0, Ui.dp(this, 10), 0, 0);
        root.addView(note);

        String infoText;
        if (p.facing() >= 0) {
            double facingDeg = p.facing() * 45.0;
            double rel = ((bearing - facingDeg) % 360 + 540) % 360 - 180;
            String side;
            if (Math.abs(rel) <= 20) side = "ahead";
            else if (rel > 20 && rel <= 70) side = "ahead-right";
            else if (rel > 70 && rel < 110) side = "to your right";
            else if (rel >= 110 && rel < 160) side = "behind-right";
            else if (rel <= -20 && rel >= -70) side = "ahead-left";
            else if (rel < -70 && rel > -110) side = "to your left";
            else if (rel <= -110 && rel > -160) side = "behind-left";
            else side = "behind you";
            infoText = Lang.ar()
                    ? "القبلة " + Lang.t(side) + "\n" + String.format(Locale.US, "%.0f° من الشمال", bearing)
                    : String.format(Locale.US, "Qibla is %s\n%.0f° from North", side, bearing);
            infoText = Lang.num(infoText);
        } else {
            infoText = Lang.t(String.format(Locale.US,
                    "Qibla is %.0f° clockwise from North\nFace %s",
                    bearing, PrayerCalc.directionName(bearing)));
        }
        TextView info = Ui.label(this, infoText, 24, Ui.GOLD);
        info.setGravity(Gravity.CENTER);
        Ui.margin(info, 0, Ui.dp(this, 8), 0, 0);
        root.addView(info);

        TextView city = Ui.label(this,
                Lang.t("from " + Lang.city(p.cityName(), p.cityCountry())), 18, Ui.DIM);
        city.setGravity(Gravity.CENTER);
        Ui.margin(city, 0, Ui.dp(this, 8), 0, 0);
        root.addView(city);

        setContentView(root);
    }

    @Override
    protected void onDestroy() {
        try {
            if (web != null) {
                web.stopLoading();
                web.destroy();
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    /**
     * Serves Esri satellite tiles from a local disk cache. Tiles are downloaded
     * once when the map is first opened with internet, then reused offline.
     * Runs on WebView's background thread, so network I/O here is fine.
     */
    private WebResourceResponse cachedTile(String url) {
        try {
            int i = url.lastIndexOf("/tile/");
            if (i < 0) return null;
            String[] parts = url.substring(i + 6).split("/");
            if (parts.length != 3) return null;
            File dir = new File(getCacheDir(), "qibla_tiles");
            dir.mkdirs();
            File f = new File(dir, parts[0] + "_" + parts[1] + "_" + parts[2] + ".jpg");
            if (!f.exists()) {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(15000);
                try (InputStream in = c.getInputStream();
                     FileOutputStream out = new FileOutputStream(f)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                } finally {
                    c.disconnect();
                }
            }
            if (f.exists() && f.length() > 0) {
                return new WebResourceResponse("image/jpeg", null, new FileInputStream(f));
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Self-contained satellite viewer: Esri tiles + SVG qibla arrow, no API key. */
    private String mapHtml(double lat, double lng, double bearing) {
        String html =
                "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:#0B1E5B}"
                + "#map{position:absolute;left:0;top:0;right:0;bottom:0}"
                + "img.tile{position:absolute;width:256px;height:256px}"
                + "#ov{position:absolute;left:0;top:0;pointer-events:none}"
                + "#attr{position:absolute;right:6px;bottom:4px;color:#fff;font:11px sans-serif;opacity:.75;text-shadow:0 1px 2px #000}"
                + "</style></head><body>"
                + "<div id='map'></div><svg id='ov'></svg>"
                + "<div id='attr'>Imagery &copy; Esri, Maxar, Earthstar Geographics</div>"
                + "<script>"
                + "var LAT=__LAT__,LON=__LON__,BRG=__BRG__,Z=12;"
                + "function tx(lon){return (lon+180)/360*Math.pow(2,Z);}"
                + "function ty(lat){var r=lat*Math.PI/180;return (1-Math.log(Math.tan(r)+1/Math.cos(r))/Math.PI)/2*Math.pow(2,Z);}"
                + "function build(){"
                + "var cx=tx(LON),cy=ty(LAT),n=Math.pow(2,Z);"
                + "var W=window.innerWidth,H=window.innerHeight;"
                + "var x0=Math.floor(cx-W/512),x1=Math.floor(cx+W/512);"
                + "var y0=Math.max(0,Math.floor(cy-H/512)),y1=Math.min(n-1,Math.floor(cy+H/512));"
                + "var ox=(x0-cx)*256+W/2,oy=(y0-cy)*256+H/2;"
                + "var m=document.getElementById('map');m.innerHTML='';"
                + "for(var yy=y0;yy<=y1;yy++){for(var xx=x0;xx<=x1;xx++){"
                + "var qx=((xx%n)+n)%n;"
                + "var im=document.createElement('img');im.className='tile';"
                + "im.style.left=(ox+(xx-x0)*256)+'px';im.style.top=(oy+(yy-y0)*256)+'px';"
                + "im.src='https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/'+Z+'/'+yy+'/'+qx;"
                + "m.appendChild(im);}}"
                + "var svg=document.getElementById('ov');"
                + "svg.setAttribute('width',W);svg.setAttribute('height',H);"
                + "var rad=BRG*Math.PI/180,len=Math.min(W,H)*0.42;"
                + "var ex=W/2+Math.sin(rad)*len,ey=H/2-Math.cos(rad)*len;"
                + "var hl=34,ha=0.5;"
                + "var p1x=ex-Math.sin(rad-ha)*hl,p1y=ey+Math.cos(rad-ha)*hl;"
                + "var p2x=ex-Math.sin(rad+ha)*hl,p2y=ey+Math.cos(rad+ha)*hl;"
                + "var s='<defs><filter id=\"sh\" x=\"-40%\" y=\"-40%\" width=\"180%\" height=\"180%\">"
                + "<feDropShadow dx=\"0\" dy=\"0\" stdDeviation=\"4\" flood-color=\"#000\" flood-opacity=\"0.7\"/></filter></defs>';"
                + "s+='<g filter=\"url(#sh)\">';"
                + "s+='<circle cx=\"'+W/2+'\" cy=\"'+H/2+'\" r=\"13\" fill=\"#448AFF\" stroke=\"#fff\" stroke-width=\"4\"/>';"
                + "s+='<line x1=\"'+W/2+'\" y1=\"'+H/2+'\" x2=\"'+ex+'\" y2=\"'+ey+'\" stroke=\"#D4AF37\" stroke-width=\"9\" stroke-linecap=\"round\"/>';"
                + "s+='<polygon points=\"'+ex+','+ey+' '+p1x+','+p1y+' '+p2x+','+p2y+'\" fill=\"#D4AF37\"/>';"
                + "s+='<text x=\"'+ex+'\" y=\"'+(ey-24)+'\" font-size=\"54\" text-anchor=\"middle\">\\uD83D\\uDD4B</text>';"
                + "s+='</g>';svg.innerHTML=s;}"
                + "window.onload=build;"
                + "</script></body></html>";
        return html
                .replace("__LAT__", String.format(Locale.US, "%.6f", lat))
                .replace("__LON__", String.format(Locale.US, "%.6f", lng))
                .replace("__BRG__", String.format(Locale.US, "%.2f", bearing));
    }
}

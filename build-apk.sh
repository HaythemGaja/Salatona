#!/bin/bash
# Build LeoSalah.apk (versionCode 29 / versionName 2.0)
set -e
cd "$(dirname "$0")"
JDK="${JAVA_HOME:-$PWD/../jdk}"
export PATH="$JDK/bin:$PATH"

BT="${BUILD_TOOLS:-$PWD/../sdk/build-tools/34.0.0}"
PLAT="${ANDROID_JAR:-$PWD/../sdk/platforms/android-34/android.jar}"
APP="$PWD/app"
OUT="$PWD/build/out"
KEYSTORE="${KEYSTORE:-$PWD/../debug.keystore}"
APK_NAME="LeoSalah.apk"
VER_CODE=29
VER_NAME="2.0"

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== aapt2 compile =="
"$BT/aapt2" compile --dir "$APP/res" -o "$OUT/compiled-res.zip"

echo "== aapt2 link =="
"$BT/aapt2" link -o "$OUT/base.apk" \
  -I "$PLAT" \
  --manifest "$APP/AndroidManifest.xml" \
  --java "$OUT/gen" \
  "$OUT/compiled-res.zip" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code "$VER_CODE" \
  --version-name "$VER_NAME"

echo "== javac =="
find "$APP/java" "$OUT/gen" -name "*.java" > "$OUT/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -nowarn -cp "$PLAT" -d "$OUT/classes" @"$OUT/sources.txt"

echo "== d8 =="
"$BT/d8" --lib "$PLAT" --min-api 26 --output "$OUT/dex" \
  $(find "$OUT/classes" -name "*.class")

echo "== package dex =="
cp "$OUT/base.apk" "$OUT/app.apk"
(cd "$OUT/dex" && zip -q -j "$OUT/app.apk" classes.dex)

echo "== zipalign =="
"$BT/zipalign" -f 4 "$OUT/app.apk" "$OUT/app-aligned.apk"

echo "== apksigner (v1+v2+v3) =="
"$BT/apksigner" sign \
  --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "$OUT/$APK_NAME" "$OUT/app-aligned.apk"

echo "== verify =="
"$BT/apksigner" verify --print-certs "$OUT/$APK_NAME"
"$BT/zipalign" -c 4 "$OUT/$APK_NAME" && echo "zipalign: OK"
echo "BUILD OK: $OUT/$APK_NAME"
ls -la "$OUT/$APK_NAME"

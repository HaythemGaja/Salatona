#!/bin/bash
# Headless daylight-saving checks (no device, no Android runtime).
# Uses the SDK's android.jar only to satisfy the few Android imports.
set -e
cd "$(dirname "$0")/.."

SDK="${ANDROID_SDK_ROOT:-$PWD/../sdk}"
ANDROID_JAR="${ANDROID_JAR:-$SDK/platforms/android-34/android.jar}"
OUT="$PWD/build/test"
rm -rf "$OUT"
mkdir -p "$OUT"

javac -encoding UTF-8 -source 8 -target 8 -nowarn -cp "$ANDROID_JAR" -d "$OUT" \
  app/java/com/leo/salah/PrayerTimes.java \
  app/java/com/leo/salah/PrayerCalc.java \
  app/java/com/leo/salah/Prefs.java \
  app/java/com/leo/salah/Daylight.java \
  app/java/com/leo/salah/Lang.java \
  app/java/com/leo/salah/Cities.java \
  test/DstTest.java

java -cp "$OUT:$ANDROID_JAR" DstTest

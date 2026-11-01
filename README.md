# Salatona (صلاتنا)

Android TV prayer-times app: a full-day prayer timeline (Islamic
midnight → Islamic midnight), per-prayer athan with selectable voices,
weather forecast under each prayer, Hijri/Gregorian timetable, Qibla
compass, tasbih counter, post-prayer duas, and a Ramadan Suhoor/Iftar card.

- Package: `com.leo.salah` (upgrades existing installs in place)
- Prayer times: on-device PrayTimes engine (no API key)
- Weather: Open-Meteo hourly forecast (free, no API key)
- Current release: v1.26 (versionCode 27)

## Timeline

The salah day runs from Islamic midnight to Islamic midnight — midnight in
salah is half the night, halfway between Isha and Fajr. Eight anchors share
the line evenly (midnight, six prayers, midnight); the gold fill advances by
real time within each segment. A prayer lights up gold only from its time
until 30 minutes after.

## Build

`build-apk.sh` is the manual SDK-tools build (aapt2 + javac + d8 +
zipalign + apksigner). It expects this layout:

```
android-build/
  sdk/
    build-tools/34.0.0/
    platforms/android-34/android.jar
  debug.keystore          # signing key, kept OUTSIDE this repo
  salah/                  # this repo
```

Then:

```sh
bash build-apk.sh   # writes build/out/LeoSalah.apk
```

The debug keystore is not part of this repo. For a release build that
updates the existing install without losing data, sign with the release
key instead — never embed a keystore in the app or the repo.

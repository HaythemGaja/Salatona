# Salatona (صلاتنا)

Android TV prayer-times app: a full-day prayer timeline (Islamic
midnight → Islamic midnight), per-prayer athan with selectable voices,
weather forecast under each prayer, Hijri/Gregorian timetable, Qibla
compass, tasbih counter, post-prayer duas, and a Ramadan Suhoor/Iftar card.

- Package: `com.leo.salah` (upgrades existing installs in place)
- Prayer times: on-device PrayTimes engine (no API key)
- Weather: Open-Meteo hourly forecast (free, no API key)
- Current release: v2.0 (versionCode 29)

## What is new in 2.0

- **Arabic and English UI** — a language row (follow the TV, العربية, English),
  full RTL layout, Arabic-Indic or Western digits, and Arabic month/Hijri names.
- **Hijri date offset** — ±2 days on the Umm al-Qura calendar, shared by the
  dashboard, timetable, events and Ramadan/Taraweeh logic.
- **Friday mode** — Dhuhr becomes Jumu'ah from Fajr to Maghrib, with a Surat
  al-Kahf card from Thursday evening.
- **Quran radio** — 19 live stations and reciter channels, in a foreground
  player that pauses for the athan and resumes after it.
- **Qibla room mode** — tell Salatona which way the TV faces and it says
  whether the Qibla is ahead, left, right or behind you.
- **More calculation methods** — Dubai, Kuwait, Qatar, Singapore, Turkey,
  Oman Awqaf and Morocco Habous, plus per-country defaults when the city changes.
- **Arabic announcements** — a spoken Arabic athan option alongside the
  bundled voices and the custom audio file.
- **Unmute for the athan** — a muted TV is unmuted for the call and muted again
  afterwards.
- **Follow the TV's clock** — 12/24-hour follows the system setting (or can be
  forced in Settings).
- **Night colours** — a warmer, dimmer palette for the dark hours.

## Daylight saving

Prayer times are converted to the TV's local clock using the time-zone rules
in force **at each prayer's own instant**, so a transition that falls inside
the day is handled correctly. Settings → Prayer times → *Daylight saving*
adds the manual control some TVs need:

- **Auto** (default) — follow the TV's time zone and its DST rules.
- **On · +1 hour** — add an hour (TV stuck on winter time, or a fixed zone).
- **Off · −1 hour** — subtract an hour (TV that switched early, or is fixed
  an hour ahead).

The row shows the live zone and offset (`America/New_York · daylight saving
active · UTC−4`) so the result is easy to check. Every change reschedules the
athan alarms immediately. The conversion lives in `PrayerCalc.toLocal()` and
has headless checks in `test/`.

## Settings

Settings is a two-pane TV screen: a section rail (Prayer times, Athan, Tools,
Display, System, About) on the start side, and the open section's rows on the
end side. Up/Down move through the rail, OK or Right opens a section, Back
returns to it. Within a section, Left/Right change a value in place, so focus
is never rebuilt or lost.

Display holds **Language** (follow the TV, العربية, English), **Numbers**
(Arabic-Indic/Western), **Clock format** (follow the TV, 12h, 24h) and **Night
colours**. Tools holds Qibla, the TV-facing row and Quran radio.

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

The SDK/JDK/keystore locations can also be set with `JAVA_HOME`,
`BUILD_TOOLS`, `ANDROID_JAR` and `KEYSTORE`, which is handy when the repo is
built outside that exact directory layout.

The debug keystore is not part of this repo. For a release build that
updates the existing install without losing data, sign with the release
key instead — never embed a keystore in the app or the repo.

## Tests

```sh
ANDROID_SDK_ROOT=../sdk bash test/run-dst-test.sh
```

Runs 84 headless checks (all 14 calculation methods, winter/summer/
transition days in New York, a half-hour zone, and the `toMillis` wall-clock
round-trip). No device needed; `android.jar` is only on the compile classpath.

[v1.27 verification notes and screenshots](docs/VERIFICATION.md) ·
[Ideas from Afnon](docs/AFNON-IDEAS.md)

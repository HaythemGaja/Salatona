# Salatona (صلاتنا)

Android TV prayer-times app: a full-day prayer timeline (Islamic
midnight → Islamic midnight), per-prayer athan with selectable voices,
weather forecast under each prayer, Hijri/Gregorian timetable, Qibla
compass, tasbih counter, post-prayer duas, and a Ramadan Suhoor/Iftar card.

- Package: `com.leo.salah` (upgrades existing installs in place)
- Prayer times: on-device PrayTimes engine (no API key)
- Weather: Open-Meteo hourly forecast (free, no API key)
- Current source release: v2.1 (versionCode 30)

## What is new in 2.1

The dashboard keeps its existing layout and display-only screensaver behavior.
The clock has a soft contrast shadow and the countdown has a subtle outline.
Manual Ambient display now updates the sky with prayer phase and weather, and
its exit hint fades after five seconds.

**Settings → Ramadan → Expanded Ramadan mode** is **off by default**. When
turned on, the existing Ramadan card gains the Ramadan day, fasting duration,
a progress line between Fajr and Maghrib, and the next Suhoor end time after
Iftar. It respects the Hijri date offset and does not show a next-day Ramadan
Suhoor on Eid. No new alarm or sound is added; the existing Taraweeh reminder
now lives in the Ramadan section.

Language, numbers and clock format have their own section and explicit choice
lists. Language switching keeps the selected settings section and row. Arabic
navigation mirrors correctly, digits obey the selected preference, and Arabic
letter spacing stays natural. A prayer-name index mismatch is also corrected.

See [update notes and validation](docs/UPDATE-2.1.md). No APK is included.

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

Settings are grouped into Prayer times, Athan & alerts, Ramadan, Screen saver
& appearance, Language & time, Tools, Permissions & reliability, and About.
The scrollable rail is mirrored for Arabic.

- **Up/Down** browse; **OK/tap** opens a choice list or toggles a switch.
- Direction keys never change values in the main list. The arrow toward the
  content opens a section; the arrow toward the rail returns to navigation.
- **Back** returns from a detail page, then to sections, then to the dashboard.
- Volume uses a preview slider with **Apply** and **Cancel**.
- Each section remembers its row and scroll position. Language changes keep
  your current section and selection.

Prayer times shows city, timetable and Hijri date first. Calculation methods,
Asr school, daylight saving and minute adjustments live under **Advanced
prayer settings**. Athan & alerts shows common voice, volume and reminder
controls, followed by one link per prayer; each prayer has a short detail page.
Screen saver setup instructions are behind **Help**, keeping the main page short.

See [settings organization notes](docs/SETTINGS-UPDATE.md). This settings-only
revision retains all dashboard and screensaver source from the preceding package.

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

# Salatona 2.1 — settings and optional Ramadan display

## Dashboard and screensaver

The dashboard's information order, timeline, clock, sky scene and display-only
behavior are retained. Only the clock contrast shadow and countdown outline
are refined. No shortcuts, prayer tracking or interactive timeline are added.
The manual ambient view now drives its sky from the dashboard, just like the
system screensaver, and fades the exit hint after five seconds.

## Settings and language

- Scrollable, narrower navigation rail with eight clearly named sections.
- Dedicated Language section and Ramadan section.
- Explicit choice dialogs for language, number style and clock format.
- Preserve selected section, row and scroll position across recreation.
- Mirrored Arabic navigation and directional affordances.
- Subtle settings-card outlines and taller focusable rows.
- Correct digit normalization: Western digits stay Western even with Arabic
  date formatters; Arabic countdowns translate even with Arabic-Indic digits.
- Avoid letter spacing that breaks Arabic's connected appearance.
- Fix five-prayer versus six-timeline-item indexing in prayer names and Athan
  controls (Dhuhr was incorrectly able to appear as Sunrise).

## Optional expanded Ramadan mode

Enable Settings → Ramadan → Expanded Ramadan mode. The stored default is off,
so existing users retain their original Ramadan card. The feature only appears
in Ramadan, following the existing Hijri date adjustment.

The same card gains the Ramadan day and Fajr-to-Maghrib duration. A subtle
progress line appears during the fasting window. After Iftar it shows the next
Suhoor end time and remaining hours/minutes when tomorrow is still in Ramadan.
At the end of Ramadan it does not advertise a Ramadan fast on Eid. Prayer
times for tomorrow are cached during daily refresh rather than recalculated
on each second's tick. The existing Taraweeh reminder remains available in
this section; no new Suhoor alarm has been added.

## Validation performed for this update

Run `python3 test/run-headless.py` with Python 3 and JDK 11 or later.
The harness compiles the actual calculation, preference and language sources
against small Android stand-ins used only by these core tests.

- Existing daylight-saving/calculation suite: **84 passed, 0 failed**.
- New language, prayer-name and Ramadan boundary regressions: **36 passed**.
- JDK parser check: **33 production Java files parsed successfully**.

These checks do not validate Android view rendering or Activity focus behavior.
The Android SDK/build tools and an emulator are unavailable in this environment;
a full APK build and on-device Arabic/English and 720p/1080p screensaver checks
remain necessary. The screenshots elsewhere in this project belong to v2.0.

Recommended device checks: switch languages from the Language section in both
directions; cancel every choice dialog; change digit style with Arabic selected;
use the D-pad to enter and leave all sections; return from city selection;
check ambient sky changes and hint fade; test Ramadan before Fajr, at midday,
after Maghrib, and on its last day, with the option both off and on.

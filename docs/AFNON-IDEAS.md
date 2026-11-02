# What Salatona can borrow from Afnon

[Afnon](https://github.com/egyjs/afnon-android-tv-launcher) is an Android TV
launcher built around the same prayer day as Salatona: offline times, adhan,
Qibla, Ramadan and Friday modes, Arabic-first UI, plain Java/Views, D-pad
only. It is a different product (a launcher), not a drop-in library, but
several of its ideas fit Salatona with little or no new dependency.

This note started as a priority list. **v2.0 implements items 1–8 and 10,
plus the Arabic-announcement part of 9.** The sections below keep the original
rationale and are useful for future work; the recorded adhan voices remain
asset-limited.

## Status in v2.0

| # | Idea | Status | Notes |
|---|------|--------|------|
| 1 | Hijri date offset (±2 days) | ✅ Done | Umm al-Qura everywhere, ±2-day setting, shared by dashboard/timetable/events/Taraweeh |
| 2 | Friday / Jumu'ah + Surat al-Kahf | ✅ Done | Dhuhr → Jumu'ah from Fajr to Maghrib; Kahf card from Thursday Maghrib |
| 3 | Arabic UI, RTL, Arabic-Indic digits | ✅ Done | Language row, RTL, digits setting, Arabic dates/Hijri/months |
| 4 | Quran radio | ✅ Done | 19 stations/reciters; foreground player pauses for adhan |
| 5 | Unmute for the athan, restore after | ✅ Done | Unmutes a muted stream and restores mute/volume |
| 6 | Follow the TV's 12/24-hour setting | ✅ Done | Also has 12h/24h overrides in Display |
| 7 | Qibla room mode | ✅ Done | TV-facing row; “behind-right, 136° from north” |
| 8 | More calculation methods + city defaults | ✅ Done | Dubai, Kuwait, Qatar, Singapore, Turkey, Oman, Morocco; per-country defaults |
| 9 | More voices / Arabic announcements | ◑ Partial | Arabic TTS announcement added (English fallback); new recordings need licensed audio |
| 10 | Night mode palette | ✅ Done | Warm/dim palette switch in Display |

---

## 1. Hijri date offset (±2 days)

**Why.** Salatona currently mixes two Hijri calendars and gives the user no
override:

- `PrayerCalc.hijriToday()` uses `java.time.chrono.HijrahDate` (Umm al-Qura)
  for the header, Ramadan and the timetable — `PrayerCalc.java#L153`.
- `IslamicEvents` uses `android.icu.util.IslamicCalendar` with
  `ISLAMIC_CIVIL` — `IslamicEvents.java#L59`. The civil and Umm al-Qura
  calendars can differ by a day, so the events screen and the Ramadan card
  can disagree.
- Mosque/moon-sighting dates often differ by a day from any computed
  calendar, especially outside Saudi Arabia.

**What Afnon does.** It stores `hijri_offset` in `[-2, +2]`
([Prefs.java#L415-L422](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Prefs.java#L415-L422)),
uses Umm al-Qura everywhere, applies the offset, and rolls the Hijri date at
Maghrib ([PrayerDay.java#L90-L108](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/PrayerDay.java#L90-L108)).

**Suggested change.** Add `Prefs.hijriOffset()`, a "Hijri date" cycle row in
Settings → Prayer times (Calendar section), and a single
`PrayerCalc.hijriToday(offset)` used by the header, timetable, events,
Ramadan detection and the Taraweeh scheduler. Switching `IslamicEvents` to
Umm al-Qura as well removes the current inconsistency. Also note the Maghrib
rollover: an Islamic date starts at sunset, which is what Afnon's `eve`
logic handles and Salatona does not.

## 2. Friday / Jumu'ah mode

**Why.** Salatona has no Friday handling at all (no `friday`/`jumu`/`kahf`
matches in `app/java`). On Friday the dashboard still says "Dhuhr", the adhan
is the Dhuhr athan, and nothing marks the day.

**What Afnon does.** `PrayerDay` computes `friday` (Friday until its
Maghrib) and `kahf` (the Hijri Friday, from Thursday's Maghrib)
([PrayerDay.java#L30-L35](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/PrayerDay.java#L30-L35),
[#L90-L95](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/PrayerDay.java#L90-L95)).
Every screen substitutes «الجمعة» for Dhuhr, and a Surat al-Kahf reminder
appears on Thursday evening and Friday.

**Suggested change.** Add `PrayerCalc.isFriday()` (Dhuhr→Maghrib on Friday)
and a Friday card in `DashboardView`; label the Dhuhr timeline node/overlay
"Jumu'ah" and use a distinct announcement. A small "Read Surat al-Kahf"
banner needs no new screen. This is mostly string/label plumbing.

## 3. Arabic UI, RTL and Arabic-Indic digits

**Why.** Salatona's UI is English with Arabic prayer names; the audience is
Libya and the wider Arab world. `res/values/strings.xml` contains only
`app_name`; every other string is hardcoded in Java.

**What Afnon does.** `Screen.loadText()` reads a language setting
(`system`/`ar`/`en`) and a digit setting (`eastern`/`western`), sets RTL,
and swaps fonts (`Text.java`). The settings screen rebuilds live when the
language changes ([Screen.java#L124-L165](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Screen.java#L124-L165)).

**Suggested change.** Move user-facing strings to `values/strings.xml` and
add `values-ar/`, set `android:supportsRtl="true"`, and a language row
(System or Display). Keep the English default for existing installs. This is
the largest text-only change in the list, but it has no calculation risk.

## 4. Quran radio

**Why.** A prayer app on the TV is often left on; Quran radio is a natural
companion and Afnon's most visible extra.

**What Afnon does.** `Radio.java` lists 19 streams: Cairo Quran Radio,
Nablus, Shabab FM, Qatar 1/2, Oman, Jordan, the Saudi Quran and Nida al-Islam
stations, plus ten mp3quran reciters
([Radio.java](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Radio.java)).
It streams with `MediaPlayer`, keeps audio focus, pauses for the adhan and
reconnects, and draws level bars from a `Visualizer`
([README](https://github.com/egyjs/afnon-android-tv-launcher#features)).

**Suggested change for Salatona.** Salatona already has a foreground
`mediaPlayback` service, audio focus and an overlay, so the plumbing is
there. Phase it:

1. A `RadioService` + a station list, play/stop only.
2. A "Quran radio" row in Settings → Athan or a small card on the home
   screen with the current station.
3. Pause/resume around the athan (reuse the existing focus callbacks).

Check each stream's terms before shipping; Afnon keeps its list in source
rather than bundling audio. Avoid the `RECORD_AUDIO` level bars unless the
privacy cost is acceptable.

## 5. Unmute the TV for the athan, then restore it

**Why.** A muted TV can swallow the athan. Salatona sets the stream volume
in `applyAthanVolume()` (`AthanOverlayService.java#L465-L470`) but never
calls `ADJUST_UNMUTE`; on some TV firmware `setStreamVolume` alone does not
lift an existing mute, and the adhan is silent.

**What Afnon does.** `unmuteForAdhan()` checks `isStreamMute` and volume 0,
unmutes, raises the volume, remembers both, and restores them when the adhan
ends ([NowActivity.java#L182-L200](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/NowActivity.java#L182-L200)).

**Suggested change.** In `applyAthanVolume()`, capture `isStreamMute`, call
`adjustStreamVolume(ADJUST_UNMUTE, 0)` when needed, and restore the mute
flag in `restoreVolume()`. Ten lines, no UI. This is the best effort/value
ratio on the list.

## 6. Follow the TV's 12/24-hour setting

**Why.** Salatona hardcodes 12-hour times: `formatTime()` always appends
AM/PM (`PrayerCalc.java#L95`), and the clock uses `"hh:mm:ss a"`
(`DashboardView.java#L43`).

**What Afnon does.** `Prefs.clockFormat()` uses
`DateFormat.is24HourFormat(c)` when the setting is "system"
([Prefs.java#L529](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Prefs.java#L529)).

**Suggested change.** Make `formatTime` consult
`DateFormat.is24HourFormat(context)` — it needs a `Context`, so add an
overload and use it in the dashboard, timetable, overlay and timeline — and
switch `CLOCK_FMT` at runtime. Most TVs already have a user preference for
this; Salatona should honour it.

## 7. Qibla "room mode"

**Why.** A TV is fixed in a room; a north-up compass map answers "where is
the Qibla?" but a viewer wants "is it behind me?".

**What Afnon does.** It stores the TV's facing angle and draws the dial with
the TV at the top, then says "Qibla is behind you" when appropriate
([Prefs.java#L176-L184](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Prefs.java#L176-L184),
[QiblaView.java](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/QiblaView.java)).

**Suggested change.** Add a one-time "Which way does the TV face?" row in
`QiblaActivity`, compute `bearing - facing`, and show a plain-language
result ("ahead-left", "behind you"). No sensor needed.

## 8. More calculation methods and per-city defaults

**Why.** Salatona offers 7 methods (`PrayerTimes.METHOD_NAMES`) and one
global method. Afnon ships city-specific defaults: Egypt's method for Cairo,
Umm al-Qura for Saudi cities, Dubai/Kuwait/Qatar/Singapore methods, plus
Oman's Ministry of Awqaf and Morocco's Habous rules fitted to published
timetables
([Method.java](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Method.java)).

**Suggested change.** Add the Oman and Morocco methods first (they serve
users whose official timetables Salatona cannot currently match), then the
Gulf methods, and put a method/Asr default on each `Cities` entry so
changing city picks the local convention. Keep the manual override.

## 9. More adhan voices and Arabic announcements

**Why.** Salatona has three bundled voices, one Libyan voice, a custom file
and an English TTS announcement (`Prefs.VOICE_NAMES`). Afnon has 16 voices
and separate Arabic announcement clips for «حان الآن موعد صلاة …» and
«اقترب موعد صلاة …».

**Suggested change.** Add licensed recordings rather than copying Afnon's
(unlicensed collection). For announcements, prefer recorded Arabic clips or
an Arabic TTS voice over the current English sentence; make the language
follow the UI language from idea 3.

## 10. Night mode palette

**Why.** Salatona is already dark, but a warmer, dimmer variant for late
Isha / pre-Fajr is easier on the eyes on a TV in a dark room.

**What Afnon does.** `Tokens.use(Prefs.night(c))` swaps a full night palette
and theme, with a moon button on Home and a Settings → Display row.

**Suggested change.** Add a "Night colours" switch (Settings → Display) that
swaps `Ui`'s palette constants at runtime; `SalahDream` and `AmbientActivity`
can default to it after Isha.

---

## Deliberately not recommended

Afnon is also a launcher, and some of its machinery does not belong in a
prayer app:

- Replacing the TV home screen, the remote-key accessibility service, and
  the `WRITE_SECURE_SETTINGS` sleep-timer tricks: large permissions and
  firmware-specific behaviour for no prayer-time benefit.
- The phone installer / ADB pairing app: out of scope for Salatona.
- `RECORD_AUDIO` for radio level bars: privacy cost for a cosmetic effect.
- Its bundled adhan audio: no licence; use licensed recordings instead.

## Suggested order

1. Hijri offset, unmute-for-adhan, 12/24-hour clock (small, high impact).
2. Friday mode and Qibla room mode.
3. Arabic UI/RTL.
4. Calculation methods and per-city defaults.
5. Quran radio as a separate feature branch.

## Sources

- [Afnon README](https://github.com/egyjs/afnon-android-tv-launcher)
- [Afnon PrayerDay.java](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/PrayerDay.java)
- [Afnon Prefs.java](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Prefs.java)
- [Afnon Radio.java](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Radio.java)
- [Afnon Method.java](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/Method.java)
- [Afnon NowActivity.java](https://github.com/egyjs/afnon-android-tv-launcher/blob/main/app/src/main/java/tv/afnon/home/NowActivity.java)

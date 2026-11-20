# Settings organization update

This revision changes SettingsActivity, adds its Arabic translations, and
updates documentation. Dashboard, ambient/screensaver, prayer calculation,
preferences, alarms, radio and other activity sources remain unchanged from
the preceding delivered ZIP. No new dashboard features are introduced.

| Section | Contents |
| --- | --- |
| Prayer times | City, monthly timetable, Hijri date; advanced calculation and time adjustments on a separate page |
| Athan & alerts | Main voice, test, volume, fade-in, reminders, popup style, and a short detail page for each prayer |
| Ramadan | Existing expanded-display option and Taraweeh reminder |
| Screen saver & appearance | Night colours, ambient display, system screensaver selection, setup help |
| Language & time | Explicit language, digit style and clock-format choices |
| Tools | Qibla, TV direction, Quran radio, duas and Islamic events |
| Permissions & reliability | Battery status, alarm rescheduling and Android permissions |
| About | Version, time zone, privacy and close settings |

## Interaction

OK/tap opens an explicit selection list. Choosing an item saves it; Cancel or
Back dismisses the list without changing it. Volume is adjusted in a slider
dialog and saved only with Apply. Switches change only with OK/tap.
Horizontal arrows in the settings lists navigate between the rail and content;
they no longer cycle settings or accidentally toggle switches.

Back returns one level from detail pages, preserving the parent row and scroll.
Every category remembers its position. Language changes preserve the current
page; Arabic uses a mirrored rail and translated labels and dialogs. Dialogs
have a dark surface and a gold focus outline.

## Validation

`python3 test/run-headless.py`: existing calculation suite 84/84; core language,
prayer-name and Ramadan regressions 36/36; all 33 production Java files parse.
These are core regression and Java syntax checks. They do not exercise Android
views or remote input. APK compilation and on-device UI checks remain pending
because Android build tools and an emulator are unavailable here.

Suggested device checks:

1. Open each section in English and Arabic using both a remote and touch.
2. Verify arrows never edit a setting, and OK opens each option list.
3. Cancel a voice choice and volume change; confirm the original values remain.
4. Apply a volume change and revisit the section to confirm persistence.
5. Open each prayer's Athan page, change a setting, and return to the same row.
6. Open advanced prayer settings and return using Back and the explicit back row.
7. Change language while scrolled down and confirm the page is retained.
8. Return from city selection, audio selection and Android permission screens.
9. Check the dashboard and screensaver against the preceding build.

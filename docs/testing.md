# Testing

Record each build and test here, newest first. Say plainly what was not tested.

## 2026-11-10: collapsible underground (JaielJohannPerez fork)

- `gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** using Android Studio's bundled Java runtime and the existing Android SDK. The pre-existing local daemon JVM configuration was left unchanged and is not included in the feature.
- Changed XML files parsed successfully; `git diff --check` passed.
- Installed with `adb -s emulator-5554 install -r`; continued the existing emulator save without clearing app data. Not installed or tested on a phone.
- Screenshot and UI hierarchy checks: rooms initially visible; up arrow hides rooms; down arrow restores them; sky grows from 558px to 1533px on this emulator, while HUD and bottom tabs remain accessible.
- Defense while collapsed reveals the panel and upgrade sheet. Collapsing with the sheet open closes it, and expanding again leaves it closed. Checks passed against captured UI hierarchies.
- No Deepcell or Java fatal exception entries in the emulator crash buffer. Unrelated system Bluetooth crashes were present in that buffer.
- Reviewed the resize calculation: an enemy's bottom-edge progress `(y + spriteHeight) / skyHeight` is preserved. Production, Collector, upkeep, upgrade purchase and save code were not changed.
- Not interactively tested: active tutorial transitions, staffed-room/Collector production while hidden, rapid repeated resizing near impact, other screen sizes, older Android versions, or applying this patch to older commits. Those remain follow-up checks rather than claimed passes.
- Returned the emulator game to its menu after the smoke test.

## 2026-11-07: economy, upkeep, prices, boss bar position

- `gradlew assembleDebug`: **BUILD SUCCESSFUL**. Tested on the emulator; installed on the user's Pixel 7 at their request (not run there by the author). Emulator data was cleared (an earlier save edit piped through `adb shell` corrupted the file; edits now go through `adb push`).

| Check | Result |
|---|---|
| Ten survivors, 2 food: food runs out within seconds; food count red; "Out of food" toast; rooms "no food, stopped"; guards stop | Pass (screenshot) |
| Guard Training priced in food (mushroom icon), dimmed at 0 food | Pass (screenshot) |
| Boss bar top right, beside the hatch label, "Boss 68/68" | Pass (screenshot) |
| No crash after the change (only the earlier corrupted-save crash in the log) | Pass |

Not checked: offline meals over a long absence, the tap cap at high tool levels in play, buying a food upgrade, recruit prices past the first.

## 2026-11-07: tutorial highlight, harder progression, new upgrades, boss bar, music mix

- `gradlew assembleDebug`: **BUILD SUCCESSFUL**. Tested on the emulator; installed on the user's Pixel 7 at their request (not run there by the author). Later tutorial steps and wave 10 were reached by editing the emulator save (`run-as` copy of `deepcell.db`), because the tap bot could not hit moving drones.

| Check | Result |
|---|---|
| Glowing frame: sky (step 1), Scrap Dig (2), Survivors tab (3), storage box (5) | Pass (screenshots) |
| Step cards: "Step N of 6", title, short text | Pass |
| Wave banner, with the boss warning on wave 10 | Pass (screenshot) |
| Boss health bar appears and drains (68/68 to 58/68) | Pass (screenshots) |
| Crew chip "5 workers · +5/s"; "+1" with five roof guards | Pass (screenshot) |
| HUD chips no longer overlap with 4-digit scrap | Pass |
| Sprinkler, Repair Arm, Guard Training, Work Shifts listed with icons | Sprinkler seen; others in the JSON, not opened |
| Wave 10 breached a one-guard bunker and sent it back to wave 9 | Pass |
| Gameplay music player running on the emulator (`dumpsys audio`: started) | Pass; loudness not judged by ear |

Not checked: the four new upgrades' effects in play, recruit price steps beyond the first, the music on the phone speaker.

## 2026-11-07: tutorial, harder waves, Collector job, turret, music

- `gradlew assembleDebug`: **BUILD SUCCESSFUL**. Tested on emulator `Medium_Phone`. Installed on the user's Pixel 7 at their request (not run there by the author).

| Check | Result |
|---|---|
| Installed over the previous build: save opens (DB v2 to v3) | Pass |
| New game: name, then Tutorial/Skip dialog | Pass (screenshot) |
| New game has one Roof survivor, 15 food, empty rooms with the no-workers hint | Pass (screenshot) |
| Tutorial: shoot, dig and collect, recruit (arrow on Survivors, then on Recruit), job (arrow on the recruit), storage (arrow on the room with the worker), upgrades, finish | Pass (screenshots each step) |
| Recruit gets a generated name (Ivo) | Pass |
| Job picker: four jobs with figures and effects, current job marked; change applies | Pass (screenshot) |
| Guard shoots by themselves; wave 1 to 5 held without tapping | Pass |
| Waves get harder: hatch down to 1/100 by wave 8 with little tapping; breach dropped wave 7 to 6 | Pass |
| Auto-Turret bought: shows on the hatch dome, turns to targets, flashes | Pass (recording) |
| Collector job: farm storage emptied into food (0 to 30) | Pass (counts); walk not re-recorded |
| Collector machine gone from the Machines tab | Pass |
| Music files: 55 s, 69 s, 55 s loops, 1.9 MB total | Generated; not listened to |

Not checked: how the music and the quieter explosions sound, the boss track switching in at wave 10, the Music switch, migration of a save that owned the Collector machine, the tutorial on a small screen, the turret tracer at full speed (too fast for the recording).

## 2026-11-07: storage, Collector, main menu, title shots, sound loudness, performance

- `gradlew assembleDebug`: **BUILD SUCCESSFUL**. `gradlew lintDebug`: 0 errors, 63 warnings (most new ones are `IconDuplicates` from identical animation frames in each density folder).
- Emulator `Medium_Phone` (API 37). Afterwards **installed on the user's Pixel 7** at their request (wireless debugging); not tested there by us.

| Check | Result |
|---|---|
| Save from version 1 opens (stock columns added by `onUpgrade`) | Pass |
| Rooms fill storage on their own; chips show 50/50 and 30/30 in red when full | Pass (screenshot) |
| Tapping a room collects its storage (scrap 23 → 73, food 10 → 40) | Pass |
| Collector bought (level 1, next cost 66); walks into Scrap Dig, grabs, "+5", walks out; then Mushroom Farm | Pass (recording) |
| Main menu: save card, Continue, New game, Settings; New game dialog with replace warning; Cancel keeps the save | Pass (screenshots) |
| Settings from the menu; Detail spinner (Auto/High/Low) | Pass |
| Title shots: farmer close-up and bunker under the logo, dust motes visible | Pass (screenshots) |
| Outlined icons, HUD chips, tab labels | Pass (screenshot) |
| Sound files: RMS now about −12 to −18 dBFS (was −17 to −31) | Measured; not listened to |
| No crashes in `logcat -b crash` | Pass |

Frame times on the game screen (`dumpsys gfxinfo`, 20 s each):

| | Median | 90th | 99th | Native heap |
|---|---|---|---|---|
| Before this pass | 27 ms | 36 ms | 61 ms | 56 MB |
| After, High detail | 18 ms | 27 ms | 46 ms | 47 MB |
| After, Low detail | 18 ms | 19 ms | 34 ms | 51 MB |

Not checked: a real low-end phone, how the sounds sound to a listener, the Storage Racks purchase, a full Collector run with the room scrolled off screen, the title in Low detail beyond a screenshot.

## 2026-11-07: animation, effects, sound, roof guards, easing, main menu

- `gradlew assembleDebug`: **BUILD SUCCESSFUL**. `gradlew lintDebug`: 0 errors, 45 warnings (new: `IconDuplicates` for `anim_guard_idle_0` / `_4`, which are the same pose on purpose so the loop closes).
- Emulator `Medium_Phone` (API 37) only; **not installed on the phone**. Effects are too short for screenshots, so they were checked with `adb shell screenrecord` and frames pulled with ffmpeg.

| Check | Result |
|---|---|
| Digger loops the pickaxe swing at the scrap heap; dust where the pickaxe lands when the room is tapped | Pass (recording) |
| Farmer (newly recruited, set to Mushroom Farm) reaches into the first grow table; spores on tap | Pass (recording) |
| Roof guard stands on the bunker deck, left of the hatch, mirrored to aim outwards; idle loop | Pass (screenshot) |
| Tapping an enemy: guard fires with muzzle flash, sparks on the drone | Pass (recording) |
| Enemy killed: explosion with debris and smoke | Pass (recording) |
| Enemy reaches the hatch: explosion at the roof, hatch HP drops | Pass (recording) |
| Drones spin rotors, shrikes flap, all sway as they fall | Seen in recordings |
| Hatch health label is drawn above enemies and effects | Pass (recording) |
| Settings: Main menu returns to the title; only the title activity is left; Continue shows the saved wave | Pass |
| No app crashes in `logcat -b crash` | Pass |

Not checked: how the sounds actually sound (they play through SoundPool, but nobody has listened to them on a device yet), the Sound switch muting them, the upgrade sheet slide and button pop (not recorded), the boss explosion size, the Drill Rig room with workers, more than one guard or four workers at once.

## 2026-11-07: lighting, framing, icons, logo and title screen

- `gradlew assembleDebug`: **BUILD SUCCESSFUL**. `gradlew lintDebug`: 0 errors, 44 warnings (new: 9 `UnusedAttribute` for the `android:font*` attributes in `res/font/barlow.xml` on API < 26; the `app:` attributes cover those versions).
- Emulator `Medium_Phone` (API 37) only; **not installed on the phone** (the user is using it).

| Check | Result |
|---|---|
| Title: logo, panel, shots pan, blurred transition between shots (three screenshots over 9 s) | Pass |
| Continue opens the game | Pass |
| Game: dark focused rooms with black cell frames, worker with contact shadow, side-view dusk bunker with roof guard, rendered HUD and tab icons, Barlow font | Pass (screenshot) |
| Defense sheet: rendered upgrade icons, price chips | Pass (screenshot) |
| Survivors: figures, job chips with rendered icons, recruit row | Pass (screenshot) |
| Enemies (shrike, scout, bomber) on the surface | Seen in a screenshot |
| No app crashes in `logcat -b crash` (only the emulator's Bluetooth service) | Pass |

Not checked: Settings screen, the Drill Rig room image in play, the boss sprite, the launcher icon on the home screen, older Android versions (font fallback below API 26).

## 2026-11-07: Blender art rework

- `gradlew assembleDebug`: **BUILD SUCCESSFUL**. `gradlew lintDebug`: 0 errors, 33 warnings (none new from the art).
- Installed on the `Medium_Phone` emulator (API 37). Title screen checked by screenshot: rendered surface and bunker, slate panel, key-chip buttons.
- Game screen checked from a screenshot of the emulator: HUD with rendered icons and wave pill, surface with bunker and roof guard, scout drone falling, Scrap Dig and Mushroom Farm renders with worker, payout labels, round tabs.
- Installed on the user's Pixel 7 over wireless debugging at the user's request, and launched. Not tested further there.
- Not checked: the upgrade sheet, Survivors and Settings screens in the new style, the Drill Rig room image, the boss and bomber sprites in play.

## 2026-11-07: renamed to Deepcell

- `gradlew assembleDebug`: **BUILD SUCCESSFUL** with package `com.deepcell.game`.
- Emulator `Medium_Phone` (API 37): the old `com.burrownine.game` build was uninstalled first. The Deepcell launch screen, title (DEEP / CELL, new emblem) and game screen show correctly. A new game starts as "Bunker Nine". No crashes in `logcat -b crash`.
- Not re-checked after the rename: Survivors, Settings, notifications (no code changed there besides the package name). Not installed on a real phone.

## 2026-11-07: graphics and UI rework

- `gradlew assembleDebug`: **BUILD SUCCESSFUL**. `gradlew lintDebug`: 0 errors (new warnings: large vector backgrounds, explained in `docs/decisions.md`).
- Installed on the `Medium_Phone` emulator (API 37). **Not installed on a real phone.**

| Check | Result |
|---|---|
| Title screen: emblem, title, field, buttons, bunker on the horizon | Pass (screenshot) |
| Game screen: HUD chips, wave badge, bunker with roof guard, enemies falling and drifting to the bunker | Pass (screenshot) |
| Rooms: B1/B2 plates, room art, worker figure, payout label, progress bar fills when the room is tapped | Pass (screenshot) |
| Defense tab opens the upgrade sheet with icons, level bars, price buttons; tab highlighted | Pass (screenshot) |
| Survivors screen: figures by job, job chips, food chip, recruit row | Pass (screenshot) |
| Settings screen: card with Switch and Spinner, Share, Reset save card | Pass (screenshot) |
| No crashes in `logcat -b crash` | Pass |

Not checked after the rework: the "+N" popup animation (it plays too fast for screenshots), the Drill Rig machine art in the room, the tab-toggle close, the boss skull icon, the new notification icon, small screens.

## 2026-11-07: base app

### Build

- `gradlew assembleDebug`: **BUILD SUCCESSFUL** (Gradle 9.6.0, AGP 9.4.1, JDK 17, Android SDK 37). APK: `app/build/outputs/apk/debug/app-debug.apk`.
- `gradlew lintDebug`: 0 errors. Remaining warnings are deliberate: platform `Switch` (Module 6) instead of SwitchCompat, portrait lock, plural suggestions, KTX `transaction {}` suggestions.

### Emulator smoke test

Installed on the Android Studio emulator `Medium_Phone` (Android API 37). **Not installed on a real phone.**

| Check | Result |
|---|---|
| Title screen, bunker name, Play starts a new game | Pass |
| Continue enabled only when a save exists; save summary shows name and wave | Pass |
| Enemies spawn, fall (Handler loop), damage the hatch; waves advance | Pass |
| Dig: progress, full bar pays scrap; survivor in Scrap Dig adds +1 per tap | Pass |
| Buy upgrade: scrap spent, level and price update | Pass |
| "Not enough scrap" Toast | Pass |
| Bottom tabs switch the upgrade list | Pass |
| Survivors screen: list, tap changes job, "Not enough food" on recruit | Pass |
| Hatch breach: lost 25% scrap (10 → 8), hatch restored, same wave repeats, notification posted | Pass |
| Drill Rig: spinning gear on Scrap Dig, bar fills by itself | Pass |
| Offline earnings: app in background ~45 s, Toast "Drill Rig dug 25 scrap" on return | Pass |
| Settings: Sound switch value kept, difficulty Spinner opens, Share opens system chooser | Pass |
| Reset save: disabled until CheckBox ticked, then returns to title with no save | Pass |
| Status bar / navigation bar don't cover the HUD or tabs (targetSdk 37 edge-to-edge) | Pass, after adding `fitsSystemWindows` |
| No crashes in `logcat -b crash` during the run | Pass |

### Not tested

- Tapping an enemy to kill it (moving targets were hard to hit with scripted taps). Enemy kills by the Auto-Turret were not checked either; the Auto-Turret was not bought.
- Boss wave (wave 10) and its notification.
- Difficulty change actually changing enemy HP.
- Android 7–12 devices, small screens, tablets, a real phone.
- Database upgrade path (there is only version 1).

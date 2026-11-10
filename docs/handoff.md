# Handoff

Where Deepcell stands and what the next person (or agent) needs to know. Last updated 2026-11-10 (MSVIII fork: tutorial pause, consistent resource price icons, food payout). Read with `AGENTS.md`, `docs/design.md`, `docs/roadmap.md` and `docs/workflow.md` (tools, Blender MCP setup, testing).

## MSVIII fork changes (2026-11-10)

- `Tutorial` reports step changes to `GameActivity`. The game loop does not advance while a tutorial step is active; step 1 places one queued enemy in the sky for a manual shot. Skip/completion resumes play. Tutorial instructions display a paused hint.
- `tutorial_paused` is an existing-table setting, not a database migration. `saveNow` records it and `onResume` excludes elapsed tutorial time from offline earnings and meals, including screen changes and app recreation.
- `assets/floors.json` holds room bar lengths and payouts. Mushroom Farm now gives 5 food, matching 5 scrap; its base storage is now 50. Existing saves retain their resource amounts and room progress.
- `ui/ResourceIcons.kt` applies the HUD's existing scrap/food images to upgrade and recruitment prices at 24dp. No assets were regenerated.

## State

- Playable base game: four Activities (title, game, survivors, settings), SQLite save, JSON balance, offline earnings, notifications. See `docs/roadmap.md` for done/next.
- Art: everything visible in play is a Blender render made from code (`tools/blender/`), exported as WebP to `res/drawable-nodpi/`. Panels and buttons are 9-patches from `tools/art/gen_ui.py`. Small inline icons (scrap, food, back, close, skull), notification icon and legacy launcher are vectors from `tools/art/gen_art.py`.
- Font: Barlow Condensed (`res/font/barlow.xml`, SIL OFL licence in `docs/licenses/`).
- Animation: survivors, guards and enemies are rendered frame loops (`AnimationDrawable`), effects are rendered flipbooks (`ui/Fx.kt` plays both). Sounds are synthesised (`tools/audio/gen_sfx.py`) and played by `ui/Sfx.kt` (SoundPool).
- Economy: rooms produce into storage; tapping collects; survivors on the Collector job walk the rooms and collect. See `docs/decisions.md` (2026-11-07 entries).
- Tutorial: `ui/Tutorial.kt` + `layout/view_tutorial.xml` (included in the game and Survivors screens). Step saved in settings (`tutorial_step`). Each screen passes a function giving the view each step points at.
- Music: `tools/audio/gen_music.py` writes `res/raw/music_*.ogg`; `ui/Music.kt` plays them (menu in MainActivity, game/boss in GameActivity.updateMusic).
- Performance: images in `drawable-xxhdpi/xhdpi/hdpi` (not nodpi), `FloorAdapter` ViewHolder + `refresh()`, Detail setting (`ui/Quality.kt`). Measure with `adb shell dumpsys gfxinfo com.deepcell.game` (reset, play 20 s, read percentiles) and `dumpsys meminfo`.
- Title screen: `ui/TitleSlideshow.kt` + `ui/TitleAmbience.kt` (dust, drones) pans three camera shots and dips through pre-blurred copies between them. First try; the user was unsure about the idea and hasn't judged it yet.
- Commits so far: base app → graphics rework → rename to Deepcell → art scripts → Blender rework (`d7d12dd`) → lighting/frames/icons/logo/title pass (`c4319f0`) → animation, effects, sound, easing (`d85c019`) → storage, Collector, main menu, performance (`6185994`) → tutorial, difficulty, turret, music → progression, boss bar → economy → README and fork guide. Public on GitHub: https://github.com/Yauren/deepcell (branch `main`; release APKs under Releases; tag `fork-base` is the starting point for groupmates' forks; `FORKING.md` has their setup prompt; combining the forks is described in `docs/merging.md`).

## Art pipeline

1. Edit a script in `tools/blender/` (`room_dig.py`, `room_farm.py`, `shaft.py`, `surface.py`, `bunker.py`, `sprites.py`, `anim_sprites.py`, `vfx.py`, `icons.py`, `logo.py`, `title_shots.py`; shared helpers in `common.py` and `room_shell.py`).
2. Run it in Blender with `BL_DIR` set to the folder (see `tools/blender/README.md`). With the Blender MCP connector: `execute_blender_code` with `BL_DIR = r"<path to your clone>/tools/blender"` then `exec(open(BL_DIR + "/room_dig.py").read())`.
3. `python tools/blender/export_to_app.py` (Pillow, about 15 s). Writes each image at three densities. It also adds survivor contact shadows, puts the bunker on the surface title shot, makes the blurred title copies, and writes the frame sequences plus their `animation-list` XML (`anim_*.xml`, `fx_*.xml`, frame timings live in its `ANIMS` table). `--frames` exports only the frames (the full export takes about two minutes).
4. Sounds: `python tools/audio/gen_sfx.py` (numpy, ffmpeg) writes `res/raw/sfx_*.ogg`; music: `python tools/audio/gen_music.py` (numpy, scipy, ffmpeg, about 40 s) writes `res/raw/music_*.ogg`.
5. Interface drawables and tween animations: `python tools/art/gen_ui.py`; vectors: `python tools/art/gen_art.py`. Both overwrite their files, so edit the scripts, not the outputs.
6. `gradlew assembleDebug`, install on the emulator, screenshot, record in `docs/testing.md`.

Look rules that came from the user's feedback:
- Dark, lamp-lit, few strong light sources with visible cones; areas between lamps stay dark. Focus matters more than detail.
- Everything at eye level / front-on (rooms, bunker, surface). No top-down views.
- Exterior is dusk: dark silhouettes with a warm rim from the sun behind; objects are lit by their own lamps, not daylight.
- Each cell (room, shaft segment) has a black frame. Characters need contact shadows.
- Workers work in the room's work zone (scrap heap, grow tables), not off to the side. Roof guards stand on the deck.
- Movement eases in and out; avoid constant-speed slides.
- Stylized: chunky bevelled shapes, banded toon light, dark outlines, cool rim light. No pixel art (the user ruled it out).

## Gotchas

- Each Blender script builds its own scene and calls `base_materials()`. Materials are shared by name across scenes, so always re-run the script you are rendering; don't render an old scene in a saved file.
- Room renders are 2.5:1, the shaft 0.4:1, with the same vertical frame so the landing meets the room floor. Workers stand on the left third; labels sit in the top corners.
- Bunker deck surface is at about 49% of the image height and the hatch top at about 30%; `GameActivity` (`GUARD_SLOTS`, `DECK_Y`, `HATCH_Y`) and the sky's bottom margin (84dp) depend on them. Re-check if the bunker render changes.
- Title shots (`title_shots.py`) take minutes each; render one at a time with `TITLE_SHOTS_ONLY = ("dig",)` before `exec`, or the MCP call times out (Blender keeps rendering; watch the file time).
- Scene scripts set globals (`rot`, `show`, ...); `title_shots.py` execs them with `globals()` and `anim_sprites.py` uses `turn()` so a room script's `rot` tuple can't overwrite it.
- Never run long animations on ListView row views (rows are re-bound every second); drive them from the game loop like the Collector.
- Work zones are fractions of the room render (`FloorAdapter`: `HEAP_X`, `TABLES_X`). Re-check if a room render changes.
- Blender treats a trailing `.13` in an object name as a duplicate number. `anim_sprites.py` empties earlier sprite scenes before each build so part names stay exact.
- Pure emission brighter than about 1.3 renders flat white (Standard view transform); keep effect colours low to keep their hue.
- Screenshots are too slow to catch effects; use `adb shell screenrecord` and pull frames with ffmpeg.
- `room_dig_auto` is the same room with the Drill Rig; the adapter swaps images when the room is automated.
- The Blender MCP is MCP for Blender (`uvx mcp-for-blender`, formerly `blender-mcp`); setup is in `docs/workflow.md`. The add-on here was one protocol version behind (works; update with `uvx mcp-for-blender install-addon`). The asset libraries and 3D generators are off; all models are code-built.
- Windows / Git Bash: run adb with `-s emulator-5554` (the user's Pixel 7 is also connected over wireless debugging). Use `MSYS_NO_PATHCONV=1` for `adb shell` paths. Long heredocs in Bash sometimes fail; write a script file and run it instead.
- Emulator AVD: `Medium_Phone` (API 37). `svc power stayon true` keeps it awake for screenshots. It is started with `-no-snapshot-save`, so a restart throws away app data (the save) made since the snapshot. Don't fire many adb commands in parallel or screenshot in a tight loop: it stalled System UI and killed the adb server once.
- To test later game states on the emulator, stop the app, copy the save to `/data/local/tmp` with `run-as ... cp`, `adb pull` it, edit it with Python's sqlite3, `adb push` it back and `run-as ... sh -c 'cat /data/local/tmp/dc.db > databases/deepcell.db'`. Don't pipe the file through `adb shell` stdin: that corrupted it once. Tapping moving drones from a script does not work reliably.
- Turret frames share one centre (the gun's axis) so the gun ImageView rotates about its middle; `TURRET_X`, `DOME_Y` place it on the hatch dome.

## User preferences

- **Do not install on the user's phone unless they ask.** They asked for the Blender rework build, the storage/Collector build (`6185994`) the tutorial/difficulty/music build (`bc5c076`) the progression/boss bar build (`60d0fac`) and the economy build (`59367fd`), which is what the phone has now.
- Each round of feedback becomes its own commit.
- Wants game-feel: animation, effects, sound, eased motion.
- No taglines, filler text or fake features in the UI.
- Show work as screenshots; be honest about what wasn't checked.

## Not verified / open

- Sounds not yet heard by the user; upgrade sheet slide and button pop not recorded; several guards or four workers at once not seen.
- Settings screen in the new style (now seen, with the Main menu button), Drill Rig room in play, boss sprite in play, launcher icon on a home screen, Android 7 (font fallback below API 26), small screens.
- Characters have short loops only (work, idle, fire); no walking. Organic shapes (creatures, faces) are the weak spot of code-built models.
- Sounds and music were generated without being listened to by the author; the user should judge them.
- Difficulty curve (`waves.json` growth numbers, guard rate) checked only by a few minutes of emulator play: wave 7 to 9 already broke a lightly upgraded bunker.
- Economy numbers (prices, recruit growth x1.9, upkeep 1 food / 40 s, wave growth) were raised at the user's request ("too easy"); they still need play on the phone to tune.
- Low-end mode was measured on the emulator only (forced Low), not on a real weak phone.
- Mushroom Farm automation, Lift, more floors: see `docs/roadmap.md`.

<p align="center">
  <img src="docs/images/banner.png" alt="Deepcell" width="100%">
</p>

<p align="center">
  An Android bunker-defense and management game.
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%207.0%2B-3DDC84">
  <img alt="Language" src="https://img.shields.io/badge/Kotlin-XML%20layouts-7F52FF">
  <img alt="Dependencies" src="https://img.shields.io/badge/libraries-androidx%20only-informational">
  <img alt="Art" src="https://img.shields.io/badge/art-Blender%20(code--built)-E87D0D">
</p>

---

## The game

A solar storm woke up an old automated air-defense network. Its drones, and mutated birds called Shrikes, attack anything on the surface. You run a poor underground bunker. At first everything is done by hand: you tap drones to shoot them and tap the rooms to dig scrap and grow mushrooms. Over time you recruit survivors, give them jobs and build machines that do the work for you, while every wave of drones gets bigger.

<p align="center">
  <img src="docs/images/menu.png" width="200" alt="Title screen">
  <img src="docs/images/game.png" width="200" alt="Game screen">
  <img src="docs/images/jobs.png" width="200" alt="Job picker">
  <img src="docs/images/upgrades.png" width="200" alt="Upgrades and boss bar">
</p>

### Features

- **Defend the hatch**: drones and Shrikes dive at your bunker. Tap them, let your roof guards shoot, and buy an Auto-Turret that turns and fires on its own. Every 10th wave a Command Drone boss arrives, with its own health bar.
- **Waves that keep growing**: more enemies, more health and more speed every wave. If the hatch breaks you lose a quarter of your scrap and fall back a wave.
- **Underground rooms**: Scrap Dig and Mushroom Farm fill their storage by themselves when survivors work there. Tap a room to collect, or let a Collector carry it up.
- **Survivors with jobs**: Roof guard, digger, farmer or Collector. Each recruit gets a generated name, costs more food than the last, and eats food every 40 seconds. Run out and the bunker stops.
- **Eleven upgrades** in three tabs (Defense, Supplies, Machines), paid in scrap or food.
- **Offline progress**: machines and workers keep going while the app is closed (workers only while the food lasts).
- **Tutorial** offered on every new game, with a glowing frame around the thing to tap.
- **Game feel**: rendered frame animations, eased movement, explosions and sparks, synthesised sound effects and three original music tracks (menu, gameplay, boss).
- **Runs on weak phones**: a Detail setting (Auto picks Low on low-memory phones) and images in three density sizes.

## Download

Get the latest APK from **[Releases](https://github.com/Yauren/deepcell/releases/latest)** and open it on an Android phone (Android 7.0 or newer). Your phone will ask you to allow installing apps from your browser or file manager the first time.

## How to play

<img src="docs/images/tutorial.png" width="220" align="right" alt="Tutorial step">

1. **Shoot**: tap drones in the sky before they reach the hatch. Your roof guard shoots on their own too.
2. **Dig**: tap Scrap Dig until the bar fills. Each full bar is scrap.
3. **Recruit**: open **Survivors** and tap **Recruit** (costs food).
4. **Give jobs**: tap a survivor to pick Roof, Scrap Dig, Mushroom Farm or Collector. Each job says what it does.
5. **Collect**: workers fill each room's storage box. Tap the room to collect, or assign a Collector. When the box is full, work stops.
6. **Keep everyone fed**: every survivor eats. Keep farmers on Mushroom Farm.
7. **Upgrade**: spend scrap and food in **Defense**, **Supplies** and **Machines**. Tap the bunker to repair the hatch (1 scrap for 5 health).

The full rules and every number are in [`docs/design.md`](docs/design.md); balance values live in [`app/src/main/assets/`](app/src/main/assets/).

<br clear="right">

## Build and run

Requirements: **Android Studio** with **Android SDK 37**. The app runs on **Android 7.0 (API 24)** and newer.

1. Android Studio → **File → Open** → choose this folder (the one with `settings.gradle.kts`).
2. Wait for Gradle sync. Android Studio writes `local.properties` with your SDK path (not committed; it differs per PC).
3. Pick a phone or emulator and press **Run**.

From the command line:

```bash
./gradlew assembleDebug
```

On Windows use `gradlew.bat assembleDebug`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. If Gradle can't find the SDK, create `local.properties` with one line such as `sdk.dir=C\:/Users/YOU/AppData/Local/Android/Sdk`.

## Project layout

```
app/src/main/
  java/com/deepcell/game/
    MainActivity.kt        title screen: Continue, New game (name, tutorial), Settings
    GameActivity.kt        the game screen and the Handler game loop
    SurvivorsActivity.kt   survivors, job picker, recruiting
    SettingsActivity.kt    sound, music, difficulty, detail, reset save, share, main menu
    model/Models.kt        data classes: Upgrade, Floor, Enemy, Wave, Survivor, GameState
    data/GameDatabase.kt   SQLiteOpenHelper: the save (never wiped on upgrade)
    data/JsonLoader.kt     reads balance JSON for upgrades, waves and floors
    ui/                    adapters, tutorial, effects, sound, music, title screen
  assets/                  upgrades.json, waves.json, floors.json: balance values
  res/                     layouts, strings, colours, themes, drawables, anim, raw (audio)
docs/                      design, module map, roadmap, decisions, testing, handoff
tools/blender/             Blender scripts that build and render every scene, character and icon
tools/audio/               Python scripts that synthesise the sound effects and compose the music
tools/art/                 Python scripts for interface drawables, vectors and the README images
```

## Assets

Everything you see and hear was made for this project, from code:

| Asset | Made with | Where |
|---|---|---|
| Rooms, lift shaft, surface, bunker | Blender scripts, rendered to WebP | `tools/blender/room_*.py`, `shaft.py`, `surface.py`, `bunker.py` |
| Survivors, enemies, turret (frame animations) | Blender scripts with simple rigs, posed per frame | `tools/blender/sprites.py`, `anim_sprites.py` |
| Explosions, dust, spores, sparks | Blender flipbooks | `tools/blender/vfx.py` |
| Icons, logo, title shots | Blender | `tools/blender/icons.py`, `logo.py`, `title_shots.py` |
| Panels, buttons, tweens, vectors | Python (Pillow) | `tools/art/gen_ui.py`, `gen_art.py` |
| Sound effects (13) | Python synthesis (numpy), OGG | `tools/audio/gen_sfx.py` |
| Music: menu, gameplay, boss | Composed note by note in Python (numpy, scipy), seamless OGG loops | `tools/audio/gen_music.py` |
| Font | Barlow Condensed, SIL Open Font License 1.1 | `res/font/`, [`docs/licenses/`](docs/licenses/) |

To change an asset, edit its script and re-run it; don't edit the exported files. [`tools/blender/README.md`](tools/blender/README.md) explains the render and export steps (`export_to_app.py` writes each image in three density sizes and builds the `animation-list` XML).

## Documentation

| File | What it holds |
|---|---|
| [`docs/design.md`](docs/design.md) | the game rules and every number |
| [`docs/module-map.md`](docs/module-map.md) | course module → code |
| [`docs/roadmap.md`](docs/roadmap.md) | what is done and what is next |
| [`docs/decisions.md`](docs/decisions.md) | why things are the way they are |
| [`docs/testing.md`](docs/testing.md) | every build and what was (and wasn't) tested |
| [`docs/handoff.md`](docs/handoff.md) | current state, art pipeline, gotchas |
| [`docs/workflow.md`](docs/workflow.md) | tools, Blender MCP setup, building and testing |

## Credits

Made with help from AI coding assistants. All art, sound and music are generated by the scripts in `tools/`. Deepcell is an original game: no names, art or layouts are taken from other games.

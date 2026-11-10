# Module map

Where the code uses each course module. Keep this table current when you add code.

| Module | Topic | Where it is used |
|---|---|---|
| 1 | Intro to Android | `AndroidManifest.xml`: app, 4 activities, launcher activity, permission |
| 2 | Android Studio | Standard Gradle project (Kotlin DSL): `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, Gradle wrapper |
| 3 | Kotlin | All code. Data classes in `model/Models.kt` (`Upgrade`, `Floor`, `EnemyType`, `Enemy`, `Wave`, `Survivor`, `GameState`); `object` singletons (`JsonLoader`, `Notifier`, `DbContract`); lambdas for adapter callbacks; `when`, collections, null safety |
| 4 | Components and resources | `res/values/strings.xml` (all UI text, string-array for difficulty), `colors.xml`, `themes.xml` (dark theme + text styles), `dimens.xml`, button styles (`BunkerButton`, `.Dark`, `.Danger`), `res/drawable-nodpi/` (WebP renders: rooms, lift shaft, surface, bunker, survivors, enemies, game icons, logo, launcher emblem, title shots and their blurred copies), `res/drawable-xxhdpi/` (9-patch panels and key-chip buttons), `res/drawable/` (vector icons; selector, layer-list and gradient shapes for buttons, tabs, progress bars and overlays), `res/drawable-v26/ic_launcher.xml` (adaptive icon), `res/anim/` (`hit_flash`, `hatch_shake`, `shake_cycle`, `gear_spin`, `float_up`, `lamp_flicker`, `sheet_in`, `sheet_out`, `press_pop`, `hud_pop`, `enemy_in`, with decelerate/accelerate/overshoot interpolators), `res/drawable/anim_*.xml` and `fx_*.xml` (`animation-list` frame animations of rendered frames), `res/raw/` (sound effects), `res/font/` (Barlow Condensed, SIL Open Font License), `assets/` (`upgrades.json`, `waves.json`, `floors.json`); `ui/ResourceIcons.kt` shares HUD resource artwork with price buttons |
| 5 | Activities and lifecycle | `MainActivity`, `GameActivity`, `SurvivorsActivity`, `SettingsActivity`. `GameActivity.onResume` loads the save, adds offline earnings unless resuming a tutorial pause and starts the Handler loop; `onPause` stops the loop and saves; `onDestroy` closes the database. `MainActivity.onResume` refreshes the Continue button and starts the title slideshow; `onPause` stops it |
| 6 | Android UI | Layouts: LinearLayout (all screens, room workers), RelativeLayout (headers), FrameLayout (surface, sky, effect layers, bunker and roof crew, each room, upgrade sheet over the rooms), TableLayout (settings). Widgets: ListView (rooms with a footer view, upgrades, survivors), Button, ImageButton (bottom tabs, gear, back, close), ImageView (enemies, bunker, survivors, props), TextView, EditText (bunker name), CheckBox (reset confirm), Switch (sound), Spinner (difficulty), ProgressBar (hatch health, room progress, upgrade level). onClick listeners throughout. `Handler` + `Runnable` game loop in `GameActivity` pauses simulation while `Tutorial` reports an active step; `ui/TitleSlideshow` uses a Handler and tween animations (scale, translate, alpha) to pan camera shots and blur across between them |
| 7 | Intents | Explicit: Main → Game (`EXTRA_NEW_GAME`, `EXTRA_BUNKER_NAME`), Game → Survivors (`EXTRA_BUNKER_NAME`), Game → Settings (`EXTRA_BUNKER_NAME`, `EXTRA_WAVE`), Settings → Main after reset. Extras read with `intent.extras`. Implicit: Share button in `SettingsActivity` uses `ACTION_SEND` with a chooser |
| 8 | Notifications | Toasts in all activities. `ui/Notifier.kt`: `NotificationChannel` (API 26+), `NotificationCompat.Builder`, `PendingIntent` back to `GameActivity`, `NotificationManagerCompat.notify`. Sent on boss wave cleared and hatch breached |
| 9 | SQLite | `data/GameDatabase.kt`: `DbContract` contract object, `SQLiteOpenHelper`, tables `game_state`, `upgrades`, `automation`, `survivors`, `settings`; `ContentValues`, `insert`, `insertWithOnConflict`, `update`, `delete`, `query`, `Cursor`; transactions for multi-table saves |
| 10 | JSON parser | `data/JsonLoader.kt` reads `assets/upgrades.json`, `assets/waves.json` and `assets/floors.json` with `JSONObject` / `JSONArray` |

## Used but not a module topic

These are small, needed by Android itself, and explained in `docs/decisions.md`:

- `AppCompatActivity` and `Theme.AppCompat` (from the default Android Studio template).
- `BaseAdapter` for the room, upgrade and survivor rows, and `ArrayAdapter` for the Spinner. These are how a ListView or Spinner gets its rows.
- `View.isSelected` to highlight the open bottom tab through a `selector` drawable.
- Android 13+ `POST_NOTIFICATIONS` runtime permission request in `MainActivity`. Without it, notifications don't appear on new phones.
- `Animation` / `AnimationUtils.loadAnimation` to play the `res/anim` tweens.
- `AnimationDrawable` (frame animation from an `animation-list`) for survivors, enemies and effects (`ui/Fx.kt`).
- `SoundPool` with `AudioAttributes` for sound effects (`ui/Sfx.kt`); `volumeControlStream` so the volume keys change game sound.
- `Dialog` with a custom layout for New Game (`dialog_new_game.xml`), the tutorial question (`dialog_tutorial.xml`) and the job picker (`dialog_job.xml`).
- `MediaPlayer` for looping background music (`ui/Music.kt`), faded with a `Handler`.
- `<include>` to reuse the tutorial overlay layout in two screens.
- Plurals resource (`room_workers`) for "1 worker" / "5 workers".
- Toast is still used for purchases, errors, recruiting, job changes, breaches and offline earnings; only the per-wave "cleared" toast became the wave banner.
- ViewHolder pattern in `FloorAdapter` (row views found once, stored in the row's tag).
- `ActivityManager` (`isLowRamDevice`, `memoryClass`) to pick the detail level (`ui/Quality.kt`).
- Density-specific resource folders (`drawable-xxhdpi/xhdpi/hdpi`) so each phone decodes images at its own size.
- `kotlin.random.Random` for the enemy spawn position.

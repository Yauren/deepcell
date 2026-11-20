# Fork notes: MSVIII

Fork: https://github.com/MSVIII/deepcell
Started from: `fork-base` (`d814c79ba6132058e5b96e633ee1c6a936f30963`)

## What I added or changed

| Change | Files | Notes |
|---|---|---|
| Freeze every tutorial instruction | `GameActivity.kt`, `ui/Tutorial.kt`, `data/GameDatabase.kt`, tutorial layout and strings | Manual actions remain usable; a stationary wave target keeps step 1 playable; tutorial time is excluded from offline catch-up. |
| Unify resource price icons | `ui/ResourceIcons.kt`, `ui/UpgradeAdapter.kt`, `SurvivorsActivity.kt`, price layouts and dimensions | Reuse HUD scrap/food artwork at 24dp; keep feature-specific icons. |
| Match food and scrap payouts | `assets/floors.json`, `data/JsonLoader.kt`, `GameActivity.kt` | Both rooms yield 5 per full bar; balance is JSON-driven. |
| Document initial fork setup | `docs/forks/MSVIII.md` | 2026-11-10: cloned the personal fork, verified the starting commit, configured origin and upstream, and read the onboarding guides. No game code changed. |

Local setup: `origin` is this fork. `upstream` fetches from Yauren/deepcell and has its push URL disabled. The local Android SDK path is in ignored `local.properties`; personal paths are not committed. Android Studio's generated daemon JVM settings are ignored locally as well.

## Balance changes

`assets/floors.json`: Mushroom Farm payout 3 → 5 food per completed 10-progress bar. Scrap remains 5. Base farm storage follows the existing ten-bar formula, 30 → 50 food. Progress speed, upkeep, recruitment and upgrade prices are unchanged.

## Database changes

No tables, columns, database version or save migration changed. Added the `tutorial_paused` key to the existing settings table to exclude frozen tutorial time on resume. Existing resources, progress and upgrades are retained.

## Assets

None. No generation scripts or exported assets changed. At the user's request, installed Blender 5.2.4 LTS, uv 0.13.7, ffmpeg 9.0.2, and a dedicated Windows Python 3.12.16 environment with Pillow 13.0.0, NumPy 2.5.4 and SciPy 1.18.1. Installed and enabled MCP for Blender (add-on protocol 15) and configured the local Codex connection. No art or sound was regenerated.

## Removed from the base

None.

## Tested

### 2026-11-10 requested changes

- Updated APK builds successfully; inspected its packaged floor balance values (both payouts 5, both bars 10). Code/resource review found no legacy currency icon references in Kotlin or layouts.
- Installed on a temporary read-only API 37.1 emulator. Runtime verification was blocked by System UI stalls and UI Automation timeouts/exit 137, including after a 2 GB-memory restart. The initial title screen was reached; tutorial, payout, icon layout, resume/skip/offline behavior and save compatibility were not verified in play.
- No physical phone installation or test. No new app dependencies, asset regeneration, schema changes or save deletion.

### Initial fork setup

- Verified that the GitHub repository is a fork of Yauren/deepcell and the initial checkout matches `fork-base`.
- Checked Git, Android Studio, Java, the Android SDK and Python availability.
- Baseline build: **BUILD SUCCESSFUL** on 2026-11-10 with Gradle 9.6.0, AGP 9.4.1, JDK 21 and Android SDK 37. Command: `gradlew.bat --no-daemon '-Pandroid.builder.sdkDownload=false' assembleDebug`. Existing declared dependencies were downloaded; no game dependency declarations changed. APK produced at `app/build/outputs/apk/debug/app-debug.apk`.
- Imported Pillow, NumPy and SciPy successfully; verified ffmpeg exposes the libvorbis OGG encoder. The Blender MCP server connected to Blender 5.2.4 LTS and verified add-on protocol 15. Codex needs a restart to load its new MCP configuration.
- No phone or emulator play test has been performed during this setup. No asset renders, audio listening checks or save migration tests have been performed.

## Documentation observations

Some older design, handoff and roadmap sections conflict with later entries and the code. Food upkeep, Sprinkler, Repair Arm and music already exist. Workers also contribute to offline room progress while their food lasts; machines do not consume food. Those base documents were left unchanged during onboarding.

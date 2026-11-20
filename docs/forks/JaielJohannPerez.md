# Fork notes: JaielJohannPerez

Fork: https://github.com/JaielJohannPerez/deepcell
Started from: `d814c79ba6132058e5b96e633ee1c6a936f30963` (cloned main; `fork-base` was not present locally).

## What I added or changed

| Change | Files | Notes |
|---|---|---|
| Collapsible underground rooms | `ui/UndergroundToggle.kt`, `GameActivity.kt`, `layout/view_underground_toggle.xml`, `layout/activity_game.xml`, `values/strings.xml` | Green up/down arrow hides the existing room panel and gives the surface more space. Upgrades and room tutorial steps reveal it. Workers, machines, food upkeep and Collectors keep their existing behavior. |

## Balance changes

None. Resizing keeps an enemy's relative progress towards the hatch; its configured speed is unchanged, so a taller sky takes longer to cross.

## Database changes

None. No new columns, DB_VERSION change or migration. The rooms start expanded when GameActivity is created.

## Assets

No generated art or audio changed. The toggle uses Android's built-in arrow drawables.

## Removed from the base

Nothing removed, moved or reformatted. Existing unrelated `gradle/gradle-daemon-jvm.properties` is left alone.

## Integration

Keep future floors inside `undergroundPanel`. Include the new helper and included XML along with the small Activity/layout/string changes. Written against `d814c79`; an older or independently edited GameActivity/layout may need those hooks applied manually. No history rewrite is needed.

## Tested

`gradlew.bat assembleDebug` succeeded. On `emulator-5554`, continued the existing save and verified collapse, arrow reopen, Defense auto-reveal, and closing an upgrade sheet by collapsing. Screenshots and UI hierarchy checks confirmed the expanded sky; no Deepcell crash entries found. Returned to the menu afterward.

Not tested on a phone, other screen sizes/Android versions, or older commits. Active tutorial transitions, staffed production/Collectors while hidden and rapid toggles near enemy impact were reviewed in code but not interactively exercised. See `docs/testing.md` for the full result.

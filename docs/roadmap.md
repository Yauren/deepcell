# Roadmap

## Done in the MSVIII fork (2026-11-10)

- Freeze combat, production and upkeep throughout every tutorial instruction; manual actions remain available and step 1 supplies a stationary drone. Tutorial time is excluded from offline catch-up.
- Use the HUD scrap and food artwork for all upgrade and recruitment prices with bounded 24dp icons.
- Mushroom Farm payout raised from 3 to 5 food per full bar. Room payouts and bar lengths now live in `assets/floors.json`; both rooms have base storage for 50 resources.

## Done in the base app (2026-11-07)

- Project setup: Kotlin, XML layouts, Gradle Kotlin DSL, one `app` module, minSdk 24.
- 4 activities connected with explicit Intents and extras.
- Game screen in sketch order: HUD, sky, hatch, floors, upgrades, bottom tabs.
- Handler game loop: enemies fall, spawn per wave, auto machines tick once per second.
- Tap-to-shoot, manual Dig and Harvest, tap-to-repair hatch.
- 4 enemy types and a 10-wave cycle with a boss on wave 10, loaded from `waves.json`.
- 6 upgrades in 3 tabs, loaded from `upgrades.json`.
- Automation: Auto-Turret and Drill Rig.
- Breach rule: lose 25% scrap, repeat the wave.
- SQLite save: resources, upgrade levels, automation owned/level, survivors, wave, hatch health, floor progress, last-played time, settings.
- Offline earnings from automated rooms only.
- Survivors screen: jobs and recruiting.
- Settings: sound switch, difficulty spinner, reset save, share.
- Toasts, notifications, tween animations, placeholder vector art.

## Done in the graphics/UI rework (2026-11-07)

- Our bunker drawn on the surface, with roof guards; enemies drift towards it.
- Underground rooms as a side-on cutaway with a lift shaft, floor plates, room art, working survivors, payout label, "+N" popup, and the Drill Rig machine when automated.
- Upgrade sheet that opens from the bottom tabs, with upgrade icons and level bars.
- New icons, emblem/logo, launcher icon, monochrome notification icon, button and panel styles.
- Title, Survivors and Settings screens restyled; extra text removed.

## Done in the Blender art rework (2026-11-07)

- Rooms, lift shaft, surface backdrop, bunker, survivors, enemies and the scrap/food icons are stylized 3D renders made in Blender from code, exported as WebP.
- Scrap Dig has a second render with the Drill Rig built in, shown once the room is automated.
- Interface restyled: slate 9-patch panels with cut corners, olive key-chip buttons, round tab slots with a green ring, condensed bold text.
- Lamp light over each room breathes; roof guards sway.

## Done in the lighting and title pass (2026-11-07)

- Focused lamp lighting with shadows in the rooms; black cell frames; contact shadows under survivors.
- Eye-level bunker and surface; bunker relit for dusk.
- Rendered icons for tabs, upgrades and settings; new rendered logo and launcher emblem; Barlow Condensed font.
- Animated title screen: panning camera shots with blurred transitions.

## Done in the animation and sound pass (2026-11-07)

- Rendered animations: diggers swing at the scrap heap, farmers pick at the grow tables, roof guards scan and fire, drones spin their rotors, the shrike flaps.
- Rendered effects: explosions, dig dust, spores, bullet sparks.
- Sound effects for every action, muted by the Sound switch.
- Roof guards stand on the bunker deck; eased movement (enemy flight, sheet, popups, buttons, HUD); health label drawn over the enemies.
- Settings has a Main menu button.

## Done in the storage and Collector pass (2026-11-07)

- Rooms produce by themselves into storage with a capacity; tap to collect; Storage Racks upgrade.
- Collector: bought machine that walks to every room and collects (also offline).
- Main menu: Continue with a save card, New game dialog, Settings.
- Sharper 3D title shots with depth of field and moving dust and drones.
- Louder sound effects; outlined icons; labelled tabs; HUD chips.
- Performance: density-bucketed images, ViewHolder and in-place refresh, effect view reuse, Detail setting with automatic low mode.

## Done in the tutorial and difficulty pass (2026-11-07)

- Tutorial offered on New game (six steps, skippable).
- Start with only the roof guard; recruits get generated names.
- Guards shoot by themselves; waves grow in size, speed and HP; breach drops a wave.
- Collector moved from a machine to a survivor job; job picker with effects; hint in empty rooms.
- Visible Auto-Turret on the hatch dome (turns, flash, tracer).
- Explosion sound quieter; menu, gameplay and boss music with a Music switch.

## Done in the tutorial highlight and progression pass (2026-11-07)

- Glowing frame around each tutorial target; clearer step cards.
- Recruit cost grows; rooms show real crew numbers; "+N" for extra guards.
- Harder enemies and waves, pricier upgrades; Guard Training, Work Shifts, Sprinkler, Repair Arm.
- Wave banner; boss health bar; louder music that phone speakers carry.

## Done in the economy pass (2026-11-07)

- Survivors eat food (also offline); hunger stops workers and guards.
- Taps capped at one bar; tool upgrades capped at 5; Guard Training and Work Shifts cost food.
- All prices and recruiting raised; boss bar moved out of the sky.

## Known gaps

- **Food**: food is only used for recruiting. The group should decide whether survivors eat.
- **Art**: scenes, characters and enemies are Blender renders built from code (`tools/blender/`); icons are still code-drawn vectors. Characters and enemies have short rendered loops (`tools/blender/anim_sprites.py`); there is no walking yet.
- Only briefly run on a real phone (see `docs/testing.md`).

## Next (suggested, not agreed)

1. Sprinkler / Auto-Harvester: automates Mushroom Farm, earns food offline.
2. Repair Arm: repairs the hatch slowly by itself.
3. Lift: decide what it does (for example, a bonus to all floor payouts) before building it.
4. More floors, unlocked with scrap.
5. Different boss per 10-wave cycle.
6. Core Restored: the end goal once every machine is built.
7. Lift (moves the Collector between more floors once there are more than two).
8. Music.
9. Final art.

Add a line to `docs/decisions.md` when the group agrees on any of these.

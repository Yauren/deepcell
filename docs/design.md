# Deepcell: game design

Status of each part is in `docs/roadmap.md`. This file is the agreed concept plus the exact rules the base app uses.

## Premise

Deepcell is post-apocalyptic. A solar storm reawakened an old automated air-defense network. Its drones, together with mutated birds called Shrikes, attack anything on the surface. Survivors live in a poor underground bunker, Bunker Nine by default (the player can rename it).

## Core theme: automation

The bunker starts poor, so everything is manual:

| Manual job | Machine that automates it | In base app |
|---|---|---|
| Tap enemies to shoot | Auto-Turret | Yes |
| Tap Dig in Scrap Dig | Drill Rig | Yes |
| Tap Harvest in Mushroom Farm | Sprinkler / Auto-Harvester | Planned |
| Move resources between floors | Lift | Planned |
| Tap the hatch to repair | Repair Arm | Planned |
| (end goal) | Core Restored | Planned |

Only automated rooms earn while the app is closed.

## Screen layout (vertical phone)

Top to bottom:

1. HUD: Scrap and Food counters (icons, no labels), wave badge (skull on boss waves), and a gear button for Settings.
2. Surface: storm sky with a ruined skyline. Enemies come down from the top and drift towards our bunker, which sits on the ground at the bottom of the surface. Survivors on the Roof job stand guard on the bunker deck. A label in the top-left shows the bunker name and hatch health.
3. Underground: a side-on cutaway of the bunker. A lift shaft runs down the left side with floor plates (B1, B2). Each room shows its wall, props, the survivors working there, its payout (for example +5 scrap), and a progress bar. Tap the room itself to work it. Automated rooms show their machine and a spinning gear. Earth continues below the deepest room.
4. Bottom tabs: Defense, Supplies, Automation open an upgrade sheet over the rooms (tap the tab again or ✕ to close). Survivors opens the Survivors screen.

A green arrow between the surface and underground collapses the entire room panel, giving the surface the remaining height; tap the down arrow to show the rooms again. Rooms start expanded each time the game Activity is created. Production, upkeep and Collectors continue while hidden. Collapsing closes the upgrade sheet; selecting an upgrade tab reopens the room panel. Tutorial steps about rooms also reveal it. Resizing preserves each live enemy's progress towards the hatch.

The art style is stylized 3D rendered to images: dark, lamp-lit rooms, procedural rust and grime, banded (toon) lighting, dark outlines and a cool rim light. Palette: soot, concrete, rust, sodium-lamp amber, toxic green, cold slate for the interface. Interface text is kept to names and numbers.

## Resources

- **Scrap**: earned from killed enemies and from Scrap Dig. Pays for upgrades and repairs.
- **Food**: earned from Mushroom Farm. Pays for recruiting survivors and the food upgrades, and every survivor eats 1 food every 40 s (also offline). At 0 food workers and guards stop until there is food again.

No other resources.

## Survivors

Each survivor has one job:

- **Roof**: shoots by themselves (guards take turns, one shot per 2 s / guards, faster with Guard Training, 1 damage, at drones in the lower 60% of the sky) and +1 damage on every manual tap.
- **Scrap Dig** or **Mushroom Farm**: fills that room by themselves (1 progress per second each).
- **Collector**: walks into each room in turn and brings its storage up; more Collectors walk faster. With a Collector, offline output is collected too.

Survivors are never killed. A new game starts with one survivor on the Roof and 40 food. Recruiting costs 25 food for the first recruit and x1.9 for each survivor already in the bunker; recruits get a generated name and start in Scrap Dig. Rooms show their real crew size and work per second; the roof shows "+N" for guards beyond the four drawn. Tap a survivor in the Survivors screen to open the job picker.

## Tutorial

Offered after naming a new bunker. Steps: shoot a drone, dig and collect, recruit, change a job, storage and collecting, upgrades and repair. Can be skipped at any step.

## Enemies

| Enemy | HP | Speed (dp/s) | Hatch damage | Scrap reward |
|---|---|---|---|---|
| Scout Drone | 2 | 50 | 7 | 2 |
| Shrike | 1 | 85 | 4 | 2 |
| Bomber Drone | 7 | 28 | 25 | 6 |
| Command Drone (boss) | 50 | 13 | 70 | 50 |

The numbers live in `assets/waves.json`.

## Waves

- `waves.json` defines a 10-wave cycle. Wave 10 has the boss, so every 10th wave is a boss wave.
- Wave *n* uses cycle entry (n − 1) mod 10 for its mix of enemies. Waves grow: +12% enemies and +4% HP per wave (`countPerWave`, `hpPerWave`), spawns 2.5% closer per wave down to 40% (`spawnFasterPerWave`, `minSpawnScale`), and each full cycle adds 60% HP and 10% speed (`hpScalePerCycle`, `speedPerCycle`) and one more boss every second cycle. A banner announces each wave; a boss health bar shows while a boss is up.
- Difficulty multiplies enemy HP: Easy ×0.75, Normal ×1, Hard ×1.5.
- Enemies spawn one at a time, every `spawnMs`. There is a 2-second break before each wave.
- A wave is cleared when every enemy has spawned and none are left. Clearing a boss wave sends a notification.

## Combat

- **Manual shot**: tap an enemy. Damage = 1 + Sharpened Rounds level + survivors on the Roof.
- **Roof guards**: shoot by themselves (see Survivors).
- **Auto-Turret**: sits on the hatch dome; once per second it turns to the enemy closest to the hatch and fires. Damage = turret level.
- An enemy that reaches the bottom of the sky hits the hatch for its damage and disappears.

## Hatch and losing

- Max hatch health = 100 + 20 per Hatch Plating level. Buying plating also adds 20 to current health.
- Tap the bunker to repair the hatch: 1 scrap for +5 health.
- **Breach**: when hatch health reaches 0, the bunker loses 25% of its scrap (rounded down), the sky is cleared, the hatch is restored to full, and the game goes back one wave. A notification is sent. Nothing else is lost.

## Floors

| Floor | Full bar | Pays | Manual tap adds | Machine |
|---|---|---|---|---|
| Scrap Dig | 10 | 5 scrap | 1 + Better Shovels level + diggers | Drill Rig: +level progress per second |
| Mushroom Farm | 10 | 3 food | 1 + Spore Trays level + farmers | Sprinkler: +level progress per second |

## Upgrades

From `assets/upgrades.json`. Price of the next level = baseCost × costGrowth^level, rounded.

| Tab | Upgrade | Effect | Base cost | Growth | Max |
|---|---|---|---|---|---|
| Defense | Sharpened Rounds | +1 tap damage | 20 scrap | 1.9 | 10 |
| Defense | Hatch Plating | +20 max hatch health | 30 scrap | 1.9 | 10 |
| Defense | Guard Training | guards shoot 15% faster | 40 food | 2.0 | 6 |
| Supplies | Better Shovels | +1 dig progress per tap | 25 scrap | 2.0 | 5 |
| Supplies | Spore Trays | +1 farm progress per tap | 25 scrap | 2.0 | 5 |
| Supplies | Storage Racks | +25 storage per room | 40 scrap | 2.0 | 8 |
| Supplies | Work Shifts | each worker +1 work per second | 80 food | 2.5 | 4 |
| Automation | Auto-Turret | auto shot each second, damage = level | 100 scrap | 2.4 | 5 |
| Automation | Drill Rig | fills Scrap Dig, speed = level, works offline | 120 scrap | 2.4 | 5 |
| Automation | Sprinkler | fills Mushroom Farm, speed = level, works offline | 120 scrap | 2.4 | 5 |
| Automation | Repair Arm | +level hatch health per second | 150 scrap | 2.5 | 5 |

A manual tap finishes at most the current bar, however high the tool level.

## Offline earnings

When the game screen resumes, the app counts the seconds since it was last paused (capped at 8 hours). Only automated rooms earn for that time. In the base app, that is the Drill Rig: seconds × Drill Rig level progress goes into Scrap Dig. Enemies don't attack while the app is closed.

## Feedback

- Toasts: purchases, "Not enough scrap", wave cleared, breach, offline earnings.
- Notifications: boss wave cleared, hatch breached.
- Animations: workers loop their work at the room's work zone (diggers at the scrap heap, farmers at the grow tables); roof guards scan the sky and fire when the player shoots; drones spin rotors, the shrike flaps. Enemy flash on hit, hatch shake on damage, spinning gear on automated rooms.
- Effects: explosion when an enemy dies or hits the hatch, sparks when a bullet hits or the hatch is repaired, dust or spores when a room is worked.
- Sound: dig, harvest, rifle, turret, bullet hit, explosion, hatch impact, repair, upgrade bought, wave cleared, breach alarm, tab click. The Sound switch mutes them.
- Easing: movement uses ease-in/ease-out, not constant speed. Enemies drift towards the roof on a smoothstep curve with a sine sway that dies out near the hatch; the upgrade sheet eases in (decelerate) and out (accelerate); payout text pops with overshoot, rises with ease-out and fades with ease-in; tabs squash and spring back; HUD numbers pop when they go up; the title camera eases in and out of each pan. Continuous spins (gear, rotors) stay linear.
- Rooms produce by themselves: workers (and the Drill Rig) fill the bar; each full bar goes into the room's storage (shown as a chip, e.g. 12/50, red when full). Work stops when storage is full. Tapping a room adds a little work and collects the storage. A survivor on the Collector job walks into each room in turn and collects for you, also while the app is closed. A room with no workers says so and links to Survivors.
- Music: menu, gameplay and boss loops (composed in `tools/audio/gen_music.py`); Settings has a Music switch.
- Main menu: Continue (card with bunker name, wave, scrap, food, crew), New game (name dialog), Settings.

## Originality

Deepcell must stay an original game. Don't copy names, art, UI, or level layouts from Fallout Shelter, Idle Miner Tycoon, Grow Castle, or other games.

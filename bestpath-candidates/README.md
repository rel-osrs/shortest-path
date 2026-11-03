# Transport candidates from the BestPath data

Suggestions from comparing the transports of the BestPath Data Tool
([rel-osrs/best-path-tool](https://github.com/rel-osrs/best-path-tool)) with this plugin's
`src/main/resources/transports/*.tsv`. The first version (24 Oct) was 4,635 rows in 12 lists. This
version re-checks every row against Skretzo/shortest-path `master` at `de7874af` (3 Nov 2026) and
sorts the same 12 lists by how sure we are that each row is right. Everything here is offered under
this repository's BSD-2-Clause licence.

## Layout

```
high/  medium/  low/             one file per list below; one suggestion per row
  small rooms or nothing new/      new transports whose far side adds little or nothing reachable
dropped/                        one file per list; rows we no longer suggest, with the reason
item-groups.tsv                 the item groups written out in Items cells
```

Rows per list and confidence (`+ n` = rows in the small-rooms folder of that confidence):

| List | | high | medium | low | dropped |
|---|---|---|---|---|---|
| `1a missing requirements` | one requirement your row lacks | 136 | 125 | 143 | · |
| `1e requirements the format cannot hold` | a requirement the TSV format cannot express | 100 | 5 | 15 | · |
| `1f start or end 3 or more tiles off` | the start or end tile of your row (teleports: landing 5+ tiles away) | 17 | 55 | 141 | · |
| `1g plane differs` | the plane of your row's start or end | · | · | · | 2 |
| `1i start or landing not on a floor tile` | your start or end tile is not walkable | 16 | 2 | · | 14 |
| `3b new transports - Wiki or cache source` | new transports with a Wiki or cache citation | 60 + 26 | 322 + 139 | 83 + 76 | 324 |
| `3b new transports - Wiki or cache source - teleports` | new teleports | 11 | 1 | · | 18 |
| `3b new transports - Wiki or cache source - network stops` | new stops on networks | · | · | · | · |
| `3c new ladders and stairs - from the game cache` | ladder / stairs / crossing pairs found in the cache | 26 + 24 | 185 + 1008 | · + 64 | 331 |
| `3d new transports - tiles not sourced` | new transports whose tiles have no source | 4 | 30 + 12 | 8 + 75 | 228 |
| `3e new transports - in copies of other areas` | transports inside quest and cutscene copies | · | · | · | 230 |
| `3e new transports - in copies of other areas - teleports` | teleports into those copies | · | · | · | 2 |
| **total** | | **420** | **1884** | **605** | **1149** |

Network stops and the two quest-copy lists have nothing left to suggest: every network stop was on a
network the plugin already has or in the house (see Removed), and the copies are all dropped.

## Confidence

Confidence is only about whether the data is right. What a row changes for routing is in the
`Value` column (new transports) and decides the small-rooms folder, not the confidence.

- **1a requirements**: *high* = a Wiki page cited for a skill, quest or item the row lacks;
  *medium* = a quest-stage varbit/varplayer where the Wiki says the quest gates it and the stage
  value comes from Quest Helper or the cache, or the row already has a requirement in that column;
  *low* = from Quest Helper only (it tracks quest steps, which does not show a passage is locked) or
  no source.
- **1e format cannot hold**: by source: *high* = Wiki, *medium* = game cache, *low* = Quest Helper only
  or no source. `Feature needed` says what the plugin would have to support.
- **1f / 1i locations**: *high* = your current tile is not walkable on `collision-map.zip` and the
  suggested one is; *medium* = both are walkable, so only an in-game look can settle it; *low* = both
  walkable and the plugin's other rows for the same object disagree with the suggestion (see
  Staircases), or no source.
- **3b / 3c / 3d new transports**: *high* = tiles from the game cache and a Wiki page cited;
  *medium* = one of the two; *low* = no source for the tiles, or a staircase whose start and landing
  are 0-1 tiles apart while the plugin crosses that same staircase object 3+ tiles elsewhere. Both
  directions of one transport share a `Group` and sit in the same file.

## One change per row

Each row in 1a and 1e changes one requirement and leaves the tiles alone. Each row in 1f, 1g and 1i
changes only the start or end tile and leaves the requirements alone. A row of yours that needed
two requirements, or a requirement and a tile, appears once per change. `File` and `Line` point at
the row in the plugin today; `Current row` and `Suggested row` use ` | ` for a tab. Teleports that
the plugin has but lands elsewhere are in 1f.

## Small rooms or nothing new

New transports are measured on your collision map: how many tiles the plugin cannot reach today
become reachable through the row (walking plus your existing transports, requirements ignored).
Rows that open fewer than 300 tiles or nothing (mostly upper floors reached by a ladder or stairs)
and reach no bank, altar, anvil, apothecary, clue location or tooling test route go in this folder.
They are kept because they may still be correct and useful, but they change few routes.

## Staircases

121 staircase rows in 1f move both ends of one of your staircases so they are 0-1 tiles apart.
Both your tiles and the suggested tiles are walkable, so these are not walkability fixes. Your rows
start at one end of the stairs and land at the other end (3-7 tiles), following the direction the
stairs run on the collision map. The suggestions come from pairing the stairs object on the two
floors and taking a walkable tile beside it on both floors. Where the plugin has the same staircase
object elsewhere and mostly crosses it 3+ tiles, the suggestion is *low*; otherwise *medium*.

## Removed

Left out completely (610 rows):

- **House teleports** (121): portal nexus, house portals, jewellery boxes, mounted items, house
  obelisk, house spirit tree and fairy ring, and teleports into the house.
- **Networks the plugin already has** (480): Charter ship 404, Hot air balloon 30, Quetzal whistle 14, Gnome glider 7, Lake Molch boat 4, Burgh de Rott boat 4, Minigame Teleport 4, Great Conch rowboat 3, Mountain Guide 3, Dorgeshuun guide 3, Fossil Island rowboat 2, Fairy ring 1, Wilderness lever 1.
- **Tile moves of network stops** (4) and **planted spirit tree
  requirements** (5; `spirit-tree-patches.tsv` covers them).

## Dropped

- No routing effect or out of scope (807): 359 start unreachable even with every candidate added; 229 inside quest or cutscene copies; 2 teleports into quest copies; 111 agility course and minigame obstacles; 99 passages the collision map already walks through, no requirement; 7 Lunar group teleports.
- Probably wrong (221): 217 no menu option, no object id and no source; 3 moved an end to another plane; 1 an end that is not walkable.
- Done upstream (113).
- The plugin already has it (8).

## Requirements the format cannot hold

| Feature needed | rows |
|---|---|
| one of several requirements (OR) | 66 |
| quest started (any stage) | 32 |
| item must be worn | 7 |
| unlock state from dialogue | 5 |
| the file has no column for it | 5 |
| collection log unlock | 3 |
| respawn point | 1 |
| quest NOT done | 1 |

## How the rows were checked

- Every row was compared with `master` at `de7874af`; rows added or changed upstream since the
  first version are dropped as done. Teleports are matched by name and item, not only by tile.
- Walkability and connected areas come from `collision-map.zip`. "Reachable" = from Lumbridge by
  walking plus every transport and teleport in the plugin, requirements ignored.
- Item groups such as `[light source]` are written out as item-id lists (`a=1|b=1|...`) and named in
  `item-groups.tsv`. A named group in `item-variations.tsv` would be shorter.
- `Duration` is only filled where a Wiki page gives it; our own tick estimates were removed.
- Nothing was re-checked in game and the matching is automatic, so treat every row as a suggestion.

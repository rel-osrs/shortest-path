# Transport candidates from the BestPath data

Suggestions from comparing the transports of the BestPath Data Tool
([rel-osrs/best-path-tool](https://github.com/rel-osrs/best-path-tool)) with this plugin's
`src/main/resources/transports/*.tsv`. The first version (24 Oct) was 4,635 rows in 12 lists and
was too noisy. This version re-checks every row against Skretzo/shortest-path `master` at `de7874af`
(3 Nov 2026) and sorts each one by how sure we are and how much it changes routing. Everything here
is offered under this repository's BSD-2-Clause licence.

## Start here

| Folder | | requirement-changes | location-changes | new-transports | new-teleports |
|---|---|---|---|---|---|
| `1-add/` | start here: strong evidence and a clear effect on routing | 136 | 33 | 85 | 11 |
| `2-check/` | probably right, or worth it if right; needs a look | 125 | 21 | 453 | 1 |
| `3-low/` | weak evidence or little effect; safe to skip | 143 | 54 | 1604 | 0 |

- `plugin-features.md`: 120 requirements the TSV format cannot hold, grouped into 8 things the plugin would need to support (quest started, one of several requirements, item worn, ...).
- `dropped.tsv`: 1272 rows we no longer suggest, each with the reason.

## One change per row

Every row is a single suggestion of one kind:

- `requirement-changes.tsv`: one requirement (one column: Skills, Items, Quests, Varbits or
  VarPlayers) on a row you already have. Its tiles are left as they are.
- `location-changes.tsv`: the start and/or end tile of a row you already have. Its requirements are
  left as they are.
- `new-transports.tsv` and `new-teleports.tsv`: rows you don't have, in your column order.

A row that needed two requirements, or a requirement and a tile, appears once per change, so each
can be taken or left on its own. `File` and `Line` point at the row in the plugin today;
`Current row` and `Suggested row` use ` | ` for a tab, in that file's column order.

## Columns

- **Confidence**: how sure we are that the data is right.
  - Requirements: *high* = a Wiki page cited for a skill, quest or item; *medium* = a quest-stage
    varbit/varplayer where the Wiki says the quest gates it and the stage value comes from Quest Helper
    or the cache, or the row already has a requirement in that column; *low* = from Quest Helper only
    (it tracks quest steps, which does not show a passage is locked) or no source.
  - Locations: *high* = your current tile is not walkable on `collision-map.zip` and the suggested one
    is; *medium* = both are walkable, so only an in-game look can settle it; *low* = staircase tile
    differences, or no source.
  - New transports: *high* = tiles from the game cache and a Wiki page cited, and the start is
    reachable in the plugin today; *medium* = one of those; *low* = no source for the tiles, or a
    staircase whose landing is the object's own tile rather than where the player arrives.
- **Value** (new transports): what the row changes on your collision map and transports.
  "reaches ..." = makes a known place reachable that is unreachable today (a bank, altar, anvil or
  apothecary from `destinations/`, a clue location, or a tooling test route); "opens a new area" /
  "opens a small area" = tiles the plugin cannot reach today; "shortcut" = steps of walking saved;
  "enforces a requirement ..." = the collision map already walks through this passage, so the row
  only matters together with a collision change.
- **Group**: rows sharing a group are one transport (both directions, kept together) or the same
  change on repeated objects (e.g. the Lithkren Vault barriers).
- **Source**: Wiki page and revision id, game cache (cache 2735), or Quest Helper file.
- **From**: the list and line in the first version.

## Removed

These are left out completely:

- **House teleports** (121 rows): portal nexus, house portals, jewellery boxes, mounted glory /
  xeric's talisman / digsite pendant / mythical cape, house obelisk, house spirit tree and fairy ring,
  and the teleports into the house. The plugin's house code already models these.
- **Networks the plugin already has** (480 rows): charter ship 404, hot air balloon 30, quetzal whistle 14, gnome glider 7, Lake Molch boat 4, Burgh de Rott boat 4, Minigame Teleport 4, Great Conch rowboat 3, Mountain Guide 3, Dorgeshuun guide 3, Fossil Island rowboat 2, fairy ring 1, Wilderness lever 1.
- **Tile moves of network stops** (4 rows: gnome gliders, a fairy ring, a minigame
  teleport) and **planted spirit tree requirements** (5 rows; `spirit-tree-patches.tsv`
  already covers them).

## Dropped (`dropped.tsv`)

- Probably wrong (344): 217 with no menu option, no object id and no source; 123 staircases where the suggestion put both ends on the object's tile, while your rows cross the stairs (3 to 4 tiles, like most plugin stairs); 3 that moved an end to another plane; 1 with an end that is not walkable.
- No routing effect or out of scope (807): 359 whose start the plugin cannot reach even with every candidate added; 231 inside quest or cutscene copies of other areas; 111 agility course and minigame obstacles; 99 passages the collision map already walks through with no requirement; 7 Lunar group teleports.
- Done upstream (113) and already in the plugin (8).

## How the rows were checked

- **Against the plugin today**: every row was compared with `master` at `de7874af`; rows added or
  changed since the first version are marked done. Teleports are matched by name and item, not only
  by landing tile.
- **Collision map**: walkability of every start and end, and which areas are connected, from
  `collision-map.zip`. "Reachable" means reachable from Lumbridge using walking plus every transport
  and teleport in the plugin with requirements ignored, i.e. for an account with everything
  unlocked.
- **Item groups** such as `[light source]` are written out as item-id lists (`a=1|b=1|...`);
  `item-groups.tsv` names them. A named group in `item-variations.tsv` would be shorter.
- **Duration** is only filled where a Wiki page gives it; our own tick estimates were removed.
- Nothing was re-checked in game, and the matching is automatic, so please treat every row as a
  suggestion.

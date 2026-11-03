# Transport candidates from the BestPath data (reviewed)

Suggestions from comparing the transports of the BestPath Data Tool with this plugin's
`src/main/resources/transports/*.tsv`. The first version (24 Oct) had 4,635 rows. The second sorted them
by confidence against Skretzo/shortest-path `master` at `de7874af` and left out house teleports and networks
the plugin already has. This version is the result of reviewing every one of the remaining 2,914 rows
individually. Everything here is offered under this repository's BSD-2-Clause licence.

## What the review decided

| | rows | meaning |
|---|---|---|
| accepted | 902 | correct as suggested |
| fixed | 1514 | right idea; the row is corrected (object id, tiles or requirement) |
| check in game | 114 | the evidence could not settle it; the note says what to check |
| rejected | 242 | wrong, a duplicate, or unusable (in `removed in review/`) |
| redundant | 142 | correct but no routing effect, or the change belongs on another row (in `removed in review/`) |

| List | | accepted | fixed | check | rejected | redundant |
|---|---|---|---|---|---|---|
| `1a missing requirements` | a requirement your row lacks | 203 | 22 | 51 | 9 | 119 |
| `1e requirements the format cannot hold` | requirements that needed several rows or another column | 5 | 98 | 5 | 7 | 5 |
| `1f start or end 3 or more tiles off` | the start or end tile of your row | 25 | 2 | 3 | 183 | · |
| `1i start or landing not on a floor tile` | your start or end tile is not walkable | 6 | 5 | · | 7 | · |
| `3b new transports - Wiki or cache source` | new transports | 366 | 279 | 36 | 14 | 14 |
| `3b new transports - Wiki or cache source - teleports` | new teleports | 9 | 4 | · | · | · |
| `3b new transports - Wiki or cache source - network stops` | new network stops | · | 1 | · | · | · |
| `3c new ladders and stairs - from the game cache` | new ladders, stairs and crossings | 220 | 1057 | 14 | 16 | · |
| `3d new transports - tiles not sourced` | new transports whose tiles had no source | 68 | 46 | 5 | 6 | 4 |

## Layout

```
high/  medium/  low/              accepted, fixed and check-in-game rows, one file per list
  small rooms or nothing new/       the same for transports to small rooms or places with little new
removed in review/               rejected and redundant rows, with the reason
found in review.tsv              extra suggestions the review produced (see below)
dropped/                         rows left out before the review (unchanged)
item-groups.tsv                  item groups written out as id lists
```

Confidence (the folder) is about the original evidence: high = Wiki page and game cache, medium = one of
them, low = neither or Quest Helper only. After the review it matters less than the Verdict column.

## Columns

- `Id`: the suggestion's id (H/M/L = confidence, `s` = small rooms, then the list).
- `Verdict`: accepted, fixed or check in game.
- New transports: the plugin columns hold the **final** values, ready to copy into a transport TSV.
  `Changed in review` says what differs from BestPath's suggestion (for example `object Climb-down Ladder 4647
  -> Climb-down Ladder 4648; start 2230 4951 1 -> 2230 4952 1`).
- Changes to your rows: `File` and `Line` point at the row in the plugin today, `Current row` is that row,
  `Final row` is what it should become (cells joined by ` | ` in that file's column order) and `Change`
  describes the difference. A suggestion that needs several rows (one per alternative, because the format has
  no OR across columns) appears once per row with the same `Id`.
- `Review note`: why the row is right or what was fixed, with the evidence; `Claude:` marks adjustments made
  after the reviewer, `Fix notes:` remarks that came with the fix.

Each row changes one kind of thing. Requirement changes and tile changes to the same plugin row are separate
rows, and object-id corrections to your rows are separate suggestions in `found in review.tsv`.

## found in review.tsv

- **Same requirement on a neighbouring row** (114): a
  requirement suggested for one of your rows also has to go on the other rows of the same transport (same
  object and landing, start up to 3 tiles away), or the pathfinder simply uses the ungated start.
- **Object id or menu text** (18): your row cites an object that is not at
  its start in the current cache (for example Canifis stairs 16671 -> 61189/61190, Brimhaven vines 29731 ->
  21731, Horror from the Deep walls 4384 -> 4545/4546).
- **Requirement on the arrival row** (7): network rows merge the
  departure and arrival rows' requirements, so "visited" gates (Kourend, Varlamore, Laguna Aurorae) belong
  on the arrival row.
- **Plugin rows out of date**: the Contact! dungeon ladders (`transports.tsv` 3568-3585) and the Ardougne
  basement ladder (686-689) point at ladders that are no longer in the cache; replacements are in the lists.
- **To check**: two copies of the Corporeal Beast lair, the Crandor secret wall gate, and the gated doors that
  are walkable on `collision-map.zip` (requirements there do nothing until the edge is closed).

## How the rows were checked

- Objects and placements come from the current game cache (abextm/osrs-cache, 28 Oct 2026), decoded directly,
  with the bridge flag applied (objects stored one plane up on bridge tiles show one plane lower).
- **Object next to the start**: every start was checked against the cited object's placements; when the cited
  object is not there, the object within 1 tile with the row's menu option (in any of its varbit forms) is
  named. Return rows of ladders and stairs very often cited the other floor's object.
- **Side the object is used from**: the object definition (opcode 69) says from which sides an object can be
  used, the same rule the game client uses. On the plugin's own rows 98% of ladder and stair starts and 95% of
  landings follow it, so starts and landings were moved to a usable side where needed; straight stairs now cross
  the flight. Stepping stones, pillars and crossings, where you stand on the object, were left alone.
- **Requirements**: objects that change form with a varbit give exact ranges (for example `15128>37;15128<51`),
  and those replace quest-stage guesses. Variable names were checked against RuneLite's gameval lists and Quest
  Helper. Wiki notes cited by BestPath were trusted; values marked "from memory" in a note are not sourced.
- **Walkability and areas** come from `collision-map.zip`, read with the pathfinder's rules (a blocked start
  is entered from an open neighbouring tile, a blocked landing is walked off, and a chain of transports is fine).
- Items in a row are met by carried or worn items, so "must be worn" is an approximation.
- Nothing was re-checked in game. Rows marked "check in game" say what to look at.

package shortestpath.pathfinder.sailing;

import java.util.function.IntPredicate;
import shortestpath.PrimitiveIntList;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.CollisionMap;
import shortestpath.pathfinder.SplitFlagMap;

/**
 * Proves a sailing target unreachable while the search runs, so it needn't flood the sea to find out. Two searches go
 * back from the tiles the boat could arrive on, a little per step: over open tiles (a boat's centre only crosses open
 * tiles, so land the sea never touches runs out in milliseconds) and by the boat's own moves (a pocket its hull can't
 * enter). Running out proves there is no route; meeting a tile the search reached, or a budget, proves nothing.
 */
final class SailingReach
{
	static final int TILE_BUDGET = 1 << 19;
	static final int MOVE_BUDGET = 1 << 12;
	// Open tiles taken per step, and steps per tile taken by moves: about a tenth of a search step's work
	private static final int TILES_PER_STEP = 16;
	private static final int STEPS_PER_MOVE = 8;
	private static final SplitFlagMap.RegionExtent EXTENT = SplitFlagMap.getRegionExtents();

	private final CollisionMap map;
	private final SailingSearch sailing;
	private final int start;
	// Whether the search has reached a tile, so it's on a route from the start
	private final IntPredicate reached;
	// Each side's tiles, null once it can't prove anything
	private Tiles open = new Tiles();
	private Tiles moved = new Tiles();
	private int steps;

	SailingReach(CollisionMap map, SailingSearch sailing, int start, PrimitiveIntList arrivals, IntPredicate reached)
	{
		this.map = map;
		this.sailing = sailing;
		this.start = start;
		this.reached = reached;
		for (int i = 0; i < arrivals.size(); i++)
		{
			open.add(arrivals.get(i));
			moved.add(arrivals.get(i));
		}
	}

	/** Takes both searches a little further; whether the target is now proven unreachable. */
	boolean step()
	{
		for (int i = 0; i < TILES_PER_STEP && open != null; i++)
		{
			if (open.head == open.queue.size())
			{
				return true;
			}
			final int at = open.queue.get(open.head++);
			if (reached.test(at) || open.queue.size() > TILE_BUDGET)
			{
				open = null;
				break;
			}
			final int x = WorldPointUtil.unpackWorldX(at);
			final int y = WorldPointUtil.unpackWorldY(at);
			final int z = WorldPointUtil.unpackWorldPlane(at);
			if (map.n(x, y, z))
			{
				open.add(WorldPointUtil.packWorldPoint(x, y + 1, z));
			}
			if (map.s(x, y, z))
			{
				open.add(WorldPointUtil.packWorldPoint(x, y - 1, z));
			}
			if (map.e(x, y, z))
			{
				open.add(WorldPointUtil.packWorldPoint(x + 1, y, z));
			}
			if (map.w(x, y, z))
			{
				open.add(WorldPointUtil.packWorldPoint(x - 1, y, z));
			}
		}
		if (moved == null || ++steps % STEPS_PER_MOVE != 0)
		{
			return false;
		}
		if (moved.head == moved.queue.size())
		{
			return true;
		}
		final int at = moved.queue.get(moved.head++);
		if (at == start || reached.test(at) || moved.queue.size() > MOVE_BUDGET)
		{
			moved = null;
			return false;
		}
		for (int move = 0; move < sailing.moves.size(); move++)
		{
			final int from = WorldPointUtil.dxdy(at, -sailing.moves.dx(move), -sailing.moves.dy(move));
			if (!moved.contains(from) && sailing.canSail(map, WorldPointUtil.unpackWorldX(from),
				WorldPointUtil.unpackWorldY(from), WorldPointUtil.unpackWorldPlane(from), move, from == start))
			{
				moved.add(from);
			}
		}
		return false;
	}

	// The tiles a search reached, a bit each in a long per row of each region and plane, in the order it reached them
	private static final class Tiles
	{
		final long[][] rows = new long[(EXTENT.getWidth() + 1) * (EXTENT.getHeight() + 1) * 4][];
		final PrimitiveIntList queue = new PrimitiveIntList();
		int head;

		boolean contains(int packed)
		{
			final int r = region(packed);
			return r < 0 || rows[r] != null
				&& (rows[r][WorldPointUtil.unpackWorldY(packed) & 63] >>> (WorldPointUtil.unpackWorldX(packed) & 63) & 1) != 0;
		}

		void add(int packed)
		{
			if (!contains(packed))
			{
				final int r = region(packed);
				if (rows[r] == null)
				{
					rows[r] = new long[64];
				}
				rows[r][WorldPointUtil.unpackWorldY(packed) & 63] |= 1L << (WorldPointUtil.unpackWorldX(packed) & 63);
				queue.add(packed);
			}
		}

		// Index of the tile's region and plane in rows, or -1 off the map, which counts as reached so is never added
		private static int region(int packed)
		{
			final int x = (WorldPointUtil.unpackWorldX(packed) >> 6) - EXTENT.getMinX();
			final int y = (WorldPointUtil.unpackWorldY(packed) >> 6) - EXTENT.getMinY();
			return x < 0 || y < 0 || x > EXTENT.getWidth() || y > EXTENT.getHeight() ? -1
				: (x + y * (EXTENT.getWidth() + 1)) * 4 + WorldPointUtil.unpackWorldPlane(packed);
		}
	}
}

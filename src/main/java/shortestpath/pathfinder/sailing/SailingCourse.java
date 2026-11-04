package shortestpath.pathfinder.sailing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import shortestpath.PrimitiveIntList;
import shortestpath.WorldPointUtil;

/**
 * Straightens a sailing route the search found into fewer, longer legs, the way a player would sail it.
 * <p>
 * The search finds the shortest route over the boat's moves, but many routes are equally short: a stretch that goes
 * N, NNE, N, NNE ends on the same tile as N, N, NNE, NNE, and along a coast the search follows every bend with a short
 * leg. So, like Best Path's course pulling, from each corner of the route this looks for the furthest later corner it
 * can reach in at most two straight legs, and sails those instead. Two kinds of legs are tried for a stretch:
 * <ul>
 * <li>its own moves in another order, grouped by heading, which sails the same distance with fewer turns;</li>
 * <li>any one or two headings that land exactly on the stretch's end, which can be shorter.</li>
 * </ul>
 * Every leg must stay a whole number of the boat's moves, so a straightened route is still one the search could have
 * found: each move is checked with the search's own rule ({@link MoveCheck}), and a route that arrived by facing a
 * target still arrives facing the way its last leg sails ({@link ArrivalCheck}).
 * <p>
 * A stretch is only replaced when that costs no more, counting the distance sailed plus {@code turnPenalty} for every
 * 22.5 degrees the boat turns, including the turns into and out of the stretch. The penalty is what makes a slightly
 * longer route with far fewer turns worth taking; the search itself only uses turns to break ties.
 */
final class SailingCourse
{
	/** How many moves past a corner the next corner is looked for. */
	static final int LOOKAHEAD = 160;
	/** The search for a further corner stops after this many in a row that can't be reached in two legs. */
	static final int MISSES = 6;
	/** How many ways to sail a stretch are tried, cheapest first, before it counts as unreachable in two legs. */
	static final int ATTEMPTS = 4;
	/** At most this many passes over the route. */
	static final int PASSES = 3;
	/** Two legs further apart than this, in 22.5 degree steps, aren't tried: the corner between them is a U-turn. */
	static final int MAX_LEG_TURN = 4;
	private static final int HEADINGS = 16;

	/**
	 * Whether the boat can make {@code move} from the tile at {@code fromPacked}; {@code firstMove} marks the route's
	 * first move.
	 */
	interface MoveCheck
	{
		boolean canSail(int fromPacked, int move, boolean firstMove);
	}

	/**
	 * Whether the route still arrives when it ends on the tile at {@code packed} facing {@code heading}.
	 */
	interface ArrivalCheck
	{
		boolean arrives(int packed, int heading);
	}

	private final SailingMoves moves;
	private final MoveCheck check;
	private final ArrivalCheck arrival;
	private final int turnPenalty;
	// Whether the boat can make each move asked about so far, by packed tile << 6 | move << 1 | first move
	private final Map<Long, Boolean> checked = new HashMap<>();

	private SailingCourse(SailingMoves moves, MoveCheck check, ArrivalCheck arrival, int turnPenalty)
	{
		this.moves = moves;
		this.check = check;
		this.arrival = arrival;
		this.turnPenalty = turnPenalty;
	}

	/**
	 * The route from {@code start} along {@code route} (the boat's moves, in order), straightened.
	 *
	 * @param initialHeading the heading the boat faces at the start, or -1 when unknown (the first turn is then free)
	 * @param turnPenalty    what each 22.5 degrees of turning costs, in {@link SailingMoves#LENGTH_UNITS_PER_TILE}
	 * @param arrival        the last leg's heading must satisfy it; {@code null} when any heading arrives
	 * @return the straightened route's moves, in order
	 */
	static int[] straighten(SailingMoves moves, int start, int[] route, int initialHeading, int turnPenalty,
		MoveCheck check, ArrivalCheck arrival)
	{
		if (route.length < 2)
		{
			return route;
		}
		// Each pass starts its stretches at the corners the last one left, so a zigzag that straddled two stretches can
		// come out on a later pass; a pass that changes nothing is the last
		SailingCourse course = new SailingCourse(moves, check, arrival, turnPenalty);
		int[] straight = route;
		for (int pass = 0; pass < PASSES; pass++)
		{
			int[] next = course.straighten(start, straight, initialHeading);
			if (Arrays.equals(next, straight))
			{
				break;
			}
			straight = next;
		}
		return straight;
	}

	private int[] straighten(int start, int[] route, int initialHeading)
	{
		final int n = route.length;
		// corner[k] is the tile the boat is on after k moves
		final int[] corner = new int[n + 1];
		corner[0] = start;
		for (int k = 0; k < n; k++)
		{
			corner[k + 1] = step(corner[k], route[k]);
		}
		final int end = corner[n];

		final PrimitiveIntList out = new PrimitiveIntList(n);
		int heading = initialHeading;
		int i = 0;
		while (i < n)
		{
			int[] best = null;
			int bestEnd = -1;
			int misses = 0;
			for (int j = i + 2; j <= Math.min(n, i + LOOKAHEAD); j++)
			{
				int[] legs = twoLegs(route, corner, i, j, heading, j < n ? moves.heading(route[j]) : -1, j == n ? end : -1);
				if (legs != null)
				{
					best = legs;
					bestEnd = j;
					misses = 0;
				}
				else if (++misses > MISSES)
				{
					break;
				}
			}
			if (best == null)
			{
				out.add(route[i]);
				heading = moves.heading(route[i]);
				i++;
				continue;
			}
			for (int move : best)
			{
				out.add(move);
			}
			heading = moves.heading(best[best.length - 1]);
			i = bestEnd;
		}
		final int[] straight = new int[out.size()];
		for (int k = 0; k < straight.length; k++)
		{
			straight[k] = out.get(k);
		}
		return straight;
	}

	/**
	 * A way to sail from corner i to corner j in at most two legs that the boat can make and that costs no more than the
	 * route's own moves between them, the cheapest of the first few tried, as the moves in order; {@code null} if there
	 * is none.
	 *
	 * @param heading     the boat's heading at corner i, or -1 when unknown
	 * @param nextHeading the heading the route sails on from corner j, or -1 if it ends there
	 * @param end         the route's last tile when corner j is it, else -1: the last leg must then still arrive
	 */
	private int[] twoLegs(int[] route, int[] corner, int i, int j, int heading, int nextHeading, int end)
	{
		// What the route's own moves cost from i to j, and the moves it uses
		int ownCost = 0;
		int ownLegs = 0;
		int previous = heading;
		final int[] counts = new int[moves.size()];
		int distinct = 0;
		for (int k = i; k < j; k++)
		{
			int move = route[k];
			ownCost += moves.length(move) + turnCost(previous, moves.heading(move));
			if (k == i || moves.heading(route[k - 1]) != moves.heading(move))
			{
				ownLegs++;
			}
			previous = moves.heading(move);
			if (counts[move]++ == 0)
			{
				distinct++;
			}
		}
		ownCost += turnCost(previous, nextHeading);

		List<int[]> candidates = new ArrayList<>();
		// The stretch's own moves, grouped by heading: the same distance, with one corner
		if (distinct <= 2)
		{
			int first = -1;
			int second = -1;
			for (int move = 0; move < counts.length; move++)
			{
				if (counts[move] > 0)
				{
					if (first < 0)
					{
						first = move;
					}
					else
					{
						second = move;
					}
				}
			}
			if (second < 0)
			{
				candidates.add(new int[]{first, counts[first], -1, 0});
			}
			else
			{
				candidates.add(new int[]{first, counts[first], second, counts[second]});
				candidates.add(new int[]{second, counts[second], first, counts[first]});
			}
		}
		// Any one or two headings that land exactly on corner j
		final int dx = WorldPointUtil.unpackWorldX(corner[j]) - WorldPointUtil.unpackWorldX(corner[i]);
		final int dy = WorldPointUtil.unpackWorldY(corner[j]) - WorldPointUtil.unpackWorldY(corner[i]);
		for (int p = 0; p < moves.size(); p++)
		{
			final int px = moves.dx(p);
			final int py = moves.dy(p);
			// One heading: (dx, dy) is a whole number of p's moves
			if ((long) px * dy == (long) py * dx && (px != 0 ? dx % px == 0 && dx / px > 0 : dy % py == 0 && dy / py > 0))
			{
				candidates.add(new int[]{p, px != 0 ? dx / px : dy / py, -1, 0});
			}
			for (int q = 0; q < moves.size(); q++)
			{
				if (q == p || turnSteps(moves.heading(p), moves.heading(q)) > MAX_LEG_TURN)
				{
					continue;
				}
				final int qx = moves.dx(q);
				final int qy = moves.dy(q);
				final long det = (long) px * qy - (long) py * qx;
				if (det == 0)
				{
					continue;
				}
				// (dx, dy) = a p + b q, with a and b whole and at least one each
				final long a = (long) dx * qy - (long) dy * qx;
				final long b = (long) px * dy - (long) py * dx;
				if (a % det != 0 || b % det != 0 || a / det <= 0 || b / det <= 0)
				{
					continue;
				}
				candidates.add(new int[]{p, (int) (a / det), q, (int) (b / det)});
			}
		}

		// Cheapest first, by the distance they sail plus the turns into, between and out of the legs. A stretch already
		// in two legs is only replaced by a cheaper one; any other by one that costs no more
		final int[] costs = new int[candidates.size()];
		Integer[] order = new Integer[candidates.size()];
		for (int c = 0; c < costs.length; c++)
		{
			costs[c] = legCost(candidates.get(c), heading, nextHeading);
			order[c] = c;
		}
		Arrays.sort(order, (x, y) -> Integer.compare(costs[x], costs[y]));
		final int limit = ownLegs <= 2 ? ownCost - 1 : ownCost;
		int attempts = 0;
		for (int c : order)
		{
			if (costs[c] > limit || attempts >= ATTEMPTS)
			{
				break;
			}
			int[] legs = candidates.get(c);
			final int lastMove = legs[2] >= 0 ? legs[2] : legs[0];
			if (end >= 0 && arrival != null && !arrival.arrives(end, moves.heading(lastMove)))
			{
				continue;
			}
			attempts++;
			if (canSail(corner[i], legs, i == 0))
			{
				return expand(legs);
			}
		}
		// Nothing cheaper the boat can make: a stretch that is already at most two legs stays as the search found it,
		// which it checked move by move and which arrives as it did
		return ownLegs <= 2 ? Arrays.copyOfRange(route, i, j) : null;
	}

	// What legs {move, count, move, count} cost: the distance they sail, plus turning from heading first and into
	// nextHeading after
	private int legCost(int[] legs, int heading, int nextHeading)
	{
		int cost = distance(legs) + turnCost(heading, moves.heading(legs[0]));
		int last = moves.heading(legs[0]);
		if (legs[2] >= 0)
		{
			cost += turnCost(last, moves.heading(legs[2]));
			last = moves.heading(legs[2]);
		}
		return cost + turnCost(last, nextHeading);
	}

	// The distance the legs sail
	private int distance(int[] legs)
	{
		return legs[1] * moves.length(legs[0]) + (legs[2] >= 0 ? legs[3] * moves.length(legs[2]) : 0);
	}

	// Whether the boat can sail the legs from the tile at from, move by move
	private boolean canSail(int from, int[] legs, boolean firstMove)
	{
		int at = from;
		for (int leg = 0; leg < 4 && legs[leg] >= 0; leg += 2)
		{
			for (int k = 0; k < legs[leg + 1]; k++)
			{
				if (!canSail(at, legs[leg], firstMove))
				{
					return false;
				}
				firstMove = false;
				at = step(at, legs[leg]);
			}
		}
		return true;
	}

	private boolean canSail(int from, int move, boolean firstMove)
	{
		long key = ((long) from << 6) | ((long) move << 1) | (firstMove ? 1 : 0);
		Boolean known = checked.get(key);
		if (known == null)
		{
			known = check.canSail(from, move, firstMove);
			checked.put(key, known);
		}
		return known;
	}

	private int[] expand(int[] legs)
	{
		int[] out = new int[legs[1] + (legs[2] >= 0 ? legs[3] : 0)];
		Arrays.fill(out, 0, legs[1], legs[0]);
		if (legs[2] >= 0)
		{
			Arrays.fill(out, legs[1], out.length, legs[2]);
		}
		return out;
	}

	private int step(int packed, int move)
	{
		return WorldPointUtil.packWorldPoint(WorldPointUtil.unpackWorldX(packed) + moves.dx(move),
			WorldPointUtil.unpackWorldY(packed) + moves.dy(move), WorldPointUtil.unpackWorldPlane(packed));
	}

	private int turnCost(int from, int to)
	{
		return turnPenalty * turnSteps(from, to);
	}

	/** How many 22.5 degree steps turn heading {@code from} to heading {@code to}; 0 if either is unknown (-1). */
	static int turnSteps(int from, int to)
	{
		if (from < 0 || to < 0)
		{
			return 0;
		}
		int d = Math.abs(from - to) % HEADINGS;
		return Math.min(d, HEADINGS - d);
	}
}

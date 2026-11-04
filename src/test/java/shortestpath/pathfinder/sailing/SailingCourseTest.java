package shortestpath.pathfinder.sailing;

import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import shortestpath.WorldPointUtil;

public class SailingCourseTest
{
	private static final SailingMoves MOVES = SailingMoves.forSpeed(1.5);
	private static final int N = MOVES.indexOf(0, 3);
	private static final int NNE = MOVES.indexOf(1, 3);
	private static final int NE = MOVES.indexOf(1, 1);
	private static final int E = MOVES.indexOf(3, 0);
	private static final int START = WorldPointUtil.packWorldPoint(3000, 3000, 0);
	// Half a tick of turning per 22.5 degrees at speed 1.5, as the pathfinder uses
	private static final int TURN_PENALTY = 750;

	// Open sea everywhere: the boat can make every move
	private static final SailingCourse.MoveCheck OPEN_SEA = (from, move, first) -> true;

	@Test
	public void testAZigzagBecomesTwoLegs()
	{
		int[] route = {N, NNE, N, NNE, N, NNE, N, NNE};

		int[] straight = SailingCourse.straighten(MOVES, START, route, -1, TURN_PENALTY, OPEN_SEA, null);

		assertEquals("Two legs", 2, legs(straight));
		assertEquals("Ends on the same tile", end(route), end(straight));
		assertEquals("Sails no further", distance(route), distance(straight));
	}

	@Test
	public void testAStraightRouteStaysAsItIs()
	{
		int[] route = {NNE, NNE, NNE, NNE, NNE};

		assertArrayEquals(route, SailingCourse.straighten(MOVES, START, route, -1, TURN_PENALTY, OPEN_SEA, null));
	}

	@Test
	public void testLegsGoRoundWhatTheBoatCantSail()
	{
		// N, N, E, E: east first would run into something at the start tile
		int[] route = {N, N, E, N, E, N};
		SailingCourse.MoveCheck blockedEastOfStart = (from, move, first) -> !(from == START && move == E);

		int[] straight = SailingCourse.straighten(MOVES, START, route, -1, TURN_PENALTY, blockedEastOfStart, null);

		assertTrue("Never sails the blocked move", straight[0] != E);
		assertEquals(end(route), end(straight));
		assertTrue("Fewer legs than the zigzag", legs(straight) < legs(route));
	}

	@Test
	public void testTheLastLegStillArrives()
	{
		// The target is only covered facing NNE, so the route must still end on an NNE move
		int[] route = {NNE, N, NNE, N, NNE};
		int nne = MOVES.heading(NNE);

		int[] straight = SailingCourse.straighten(MOVES, START, route, -1, TURN_PENALTY, OPEN_SEA,
			(packed, heading) -> heading == nne);

		assertEquals(MOVES.heading(NNE), MOVES.heading(straight[straight.length - 1]));
		assertEquals(end(route), end(straight));
	}

	@Test
	public void testTurningIntoTheRouteCounts()
	{
		// Already facing N, a stretch of N and NNE moves starts with its N moves rather than turning first
		int[] route = {NNE, N, NNE, N};

		int[] straight = SailingCourse.straighten(MOVES, START, route, MOVES.heading(N), TURN_PENALTY, OPEN_SEA, null);

		assertEquals(N, straight[0]);
		assertEquals(2, legs(straight));
	}

	private static int legs(int[] route)
	{
		int legs = 0;
		for (int i = 0; i < route.length; i++)
		{
			if (i == 0 || MOVES.heading(route[i]) != MOVES.heading(route[i - 1]))
			{
				legs++;
			}
		}
		return legs;
	}

	private static int distance(int[] route)
	{
		return Arrays.stream(route).map(MOVES::length).sum();
	}

	private static int end(int[] route)
	{
		int at = START;
		for (int move : route)
		{
			at = step(at, move);
		}
		return at;
	}

	private static int step(int packed, int move)
	{
		return WorldPointUtil.packWorldPoint(WorldPointUtil.unpackWorldX(packed) + MOVES.dx(move),
			WorldPointUtil.unpackWorldY(packed) + MOVES.dy(move), 0);
	}
}

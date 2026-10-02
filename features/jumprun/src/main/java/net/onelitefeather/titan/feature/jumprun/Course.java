/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.jumprun;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;

/**
 * One player's run: the blocks made so far, where the player stands in them and the window the
 * player can see. Block 0 is the start block, a real block of the world that is never faked.
 */
final class Course {

    private static final int BEHIND = 2;
    private static final int AHEAD = 2;

    /**
     * Blocks of the course that are on screen while the next one is made: the new block joins the
     * window, so it has to keep clear of these.
     */
    static final int VISIBLE_BEFORE_NEW = BEHIND + AHEAD;

    /** Block 0 is a real block of the world and never faked, so its material is never shown. */
    private static final Block START_MATERIAL = Block.STONE;

    private static final int FALL_DEPTH = 3;

    /**
     * A landing reported by the client is a hair off the exact top; this much is still "standing on
     * it".
     */
    private static final double LANDING_TOLERANCE = 0.05;

    /** A player standing on an edge still has the block under part of the hitbox. */
    private static final double PLAYER_HALF_WIDTH = 0.3;

    private final Pos startPoint;
    private final CourseGenerator generator;
    private final List<CourseBlock> blocks;
    private Phase nextPhase;
    private int ascentJumps;
    private int current;

    private Course(Pos startPoint, CourseGenerator generator, List<CourseBlock> blocks, Phase nextPhase) {
        this.startPoint = startPoint;
        this.generator = generator;
        this.blocks = blocks;
        this.nextPhase = nextPhase;
    }

    /**
     * Starts a course at the block under the player. Makes the whole ascent, which ends at a block
     * in the open and away from the spawn, and one more block up front. Returns empty when that
     * does not fit, so nothing is shown for a run that cannot work.
     */
    static Optional<Course> start(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, RandomGenerator random, Palettes palettes) {
        return start(startPoint, startBlock, heading, spawn, probe, random, palettes, PortalClearance.NONE);
    }

    /** As above, and no block or flight path comes near a portal. */
    static Optional<Course> start(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, RandomGenerator random, Palettes palettes, PortalClearance portals) {
        return start(startPoint, startBlock, heading, spawn, probe, random, palettes, portals, Steering.none());
    }

    /**
     * As above, and the scored part snakes around the spawn. The sense and phase of the snake are
     * drawn from {@code random} first, before anything else uses it.
     */
    static Optional<Course> startSteered(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, RandomGenerator random, Palettes palettes, PortalClearance portals) {
        return start(startPoint, startBlock, heading, spawn, probe, random, palettes, portals, Steering.around(spawn, random));
    }

    private static Optional<Course> start(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, RandomGenerator random, Palettes palettes, PortalClearance portals, Steering steering) {
        CourseGenerator generator = new CourseGenerator(probe, spawn, random, palettes, portals, steering);
        List<CourseBlock> blocks = new ArrayList<>(List.of(new CourseBlock(startBlock, Surface.FULL, START_MATERIAL)));
        Course course = new Course(startPoint, generator, blocks, Phase.start(heading));
        boolean fits = course.generateAscent() && course.generateThrough(course.blocks.size());
        return fits ? Optional.of(course) : Optional.empty();
    }

    /** What changed in the visible window after a landing. */
    record Advance(int jumps, int scored, List<CourseBlock> removed, List<CourseBlock> added,
                   boolean exhausted) {

        private static final Advance NONE = new Advance(0, 0, List.of(), List.of(), false);
    }

    Pos startPoint() {
        return startPoint;
    }

    /** The main heading: where the course leads overall, bent a little by every block made. */
    Heading heading() {
        return nextPhase.heading();
    }

    CourseBlock current() {
        return blocks.get(current);
    }

    /**
     * Oldest first: up to two blocks behind the current one, the current one and up to two ahead.
     */
    List<CourseBlock> window() {
        return List.copyOf(blocks.subList(windowStart(), windowEnd() + 1));
    }

    /** Scored jumps made: the ascent does not count. */
    int score() {
        return Math.max(0, current - ascentJumps);
    }

    /** Index of the block the player was last seen landing on; block 0 is the start block. */
    int currentIndex() {
        return current;
    }

    /**
     * Below this y the player has fallen off the course: three under the lowest of the current and
     * the visible blocks ahead. A landing seen late must not turn a player who stands on a lower
     * block ahead into a fallen one.
     */
    double fallThreshold() {
        double lowest = blocks.subList(current, blocks.size()).stream().mapToDouble(CourseBlock::topY).min().orElseThrow();
        return lowest - FALL_DEPTH;
    }

    boolean hasFallen(double y) {
        return y < fallThreshold();
    }

    /**
     * Moves on when the feet stand on one of the blocks ahead (the edge counts). Landing on the
     * k-th counts k jumps. When no further block fits, the advance reports it as exhausted
     * and the run is over.
     */
    Advance advanceTo(Point feet) {
        int landed = landedIndex(feet);
        if (landed < 0) {
            return Advance.NONE;
        }
        int oldStart = windowStart();
        int oldEnd = windowEnd();
        int jumps = landed - current;
        int scoreBefore = score();
        current = landed;
        generateThrough(current + AHEAD);
        boolean exhausted = current == blocks.size() - 1;
        List<CourseBlock> removed = List.copyOf(blocks.subList(oldStart, Math.min(oldEnd + 1, windowStart())));
        List<CourseBlock> added = List.copyOf(blocks.subList(oldEnd + 1, windowEnd() + 1));
        return new Advance(jumps, score() - scoreBefore, removed, added, exhausted);
    }

    /** The furthest of the blocks ahead the feet stand on, or -1. */
    private int landedIndex(Point feet) {
        for (int index = blocks.size() - 1; index > current; index--) {
            if (isStandingOn(blocks.get(index), feet)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isStandingOn(CourseBlock block, Point feet) {
        boolean atHeight = Math.abs(feet.y() - block.topY()) <= LANDING_TOLERANCE;
        return atHeight && overlapsBlock(feet.x(), block.pos().x()) && overlapsBlock(feet.z(), block.pos().z());
    }

    /**
     * Whether a hitbox centred on {@code center} reaches over the block cell at {@code cell}. The
     * same half width serves every shape: the height check gates it, and being lenient on thin
     * posts and panes is intended.
     */
    private static boolean overlapsBlock(double center, int cell) {
        return center >= cell - PLAYER_HALF_WIDTH && center <= cell + 1 + PLAYER_HALF_WIDTH;
    }

    private int windowStart() {
        return Math.max(0, current - BEHIND);
    }

    private int windowEnd() {
        return Math.min(blocks.size() - 1, current + AHEAD);
    }

    /** Makes the ascent blocks; false when the ascent does not reach the open in time. */
    private boolean generateAscent() {
        while (nextPhase instanceof Phase.Ascent) {
            if (!generateNext()) {
                return false;
            }
        }
        ascentJumps = blocks.size() - 1;
        return true;
    }

    /** Makes blocks until {@code lastIndex} exists; false when one does not fit. */
    private boolean generateThrough(int lastIndex) {
        while (blocks.size() <= lastIndex) {
            if (!generateNext()) {
                return false;
            }
        }
        return true;
    }

    private boolean generateNext() {
        Optional<CourseBlock> next = generator.next(blocks, nextPhase);
        next.ifPresent(block -> {
            blocks.add(block);
            nextPhase = generator.after(blocks, nextPhase);
        });
        return next.isPresent();
    }
}

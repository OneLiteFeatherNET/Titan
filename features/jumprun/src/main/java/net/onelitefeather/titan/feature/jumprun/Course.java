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
import java.util.Collection;
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

    /**
     * Blocks under the lowest block ahead at which a run ends; the height band keeps this much
     * clear.
     */
    static final int FALL_DISTANCE = 3;

    private final Pos startPoint;
    private final CourseGenerator generator;
    private final List<CourseBlock> blocks;
    /** Entry {@code i} is the phase for the block after block {@code i}. */
    private final List<Phase> phases;
    private int ascentJumps;
    private int current;

    private Course(Pos startPoint, CourseGenerator generator, List<CourseBlock> blocks, List<Phase> phases) {
        this.startPoint = startPoint;
        this.generator = generator;
        this.blocks = blocks;
        this.phases = phases;
    }

    /**
     * Starts a course at the block under the player. Makes the whole ascent, which ends at a block
     * in the open and away from the spawn, and one more block up front. Returns empty when that
     * does not fit, so nothing is shown for a run that cannot work.
     */
    static Optional<Course> start(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, HeightBand band, RandomGenerator random, Palettes palettes) {
        return start(startPoint, startBlock, heading, spawn, probe, band, random, palettes, PortalClearance.NONE);
    }

    /** As above, and no block or flight path comes near a portal. */
    static Optional<Course> start(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, HeightBand band, RandomGenerator random, Palettes palettes, PortalClearance portals) {
        return start(startPoint, startBlock, heading, spawn, probe, band, random, palettes, portals, Steering.none(), Mode.MEDIUM);
    }

    /**
     * As above, and the scored part snakes around the spawn. The sense and phase of the snake are
     * drawn from {@code random} first, before anything else uses it.
     */
    static Optional<Course> startSteered(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, HeightBand band, RandomGenerator random, Palettes palettes, PortalClearance portals, Mode mode) {
        return start(startPoint, startBlock, heading, spawn, probe, band, random, palettes, portals, Steering.around(spawn, random), mode);
    }

    private static Optional<Course> start(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, HeightBand band, RandomGenerator random, Palettes palettes, PortalClearance portals, Steering steering, Mode mode) {
        CourseGenerator generator = new CourseGenerator(probe, band, spawn, random, palettes, portals, steering);
        CourseBlock first = new CourseBlock(startBlock, Surface.FULL, START_MATERIAL);
        if (!band.allows(first)) {
            return Optional.empty();
        }
        List<CourseBlock> blocks = new ArrayList<>(List.of(first));
        Course course = new Course(startPoint, generator, blocks, new ArrayList<>(List.of(Phase.start(heading, mode))));
        boolean fits = course.generateAscent() && course.generateThrough(course.blocks.size());
        return fits ? Optional.of(course) : Optional.empty();
    }

    /** What changed in the visible window after a landing. */
    record Advance(int jumps, int scored, List<CourseBlock> removed, List<CourseBlock> added,
                   boolean exhausted) {

        private static final Advance NONE = new Advance(0, 0, List.of(), List.of(), false);
    }

    /**
     * The blocks ahead that a reroll took away and the ones that replace them, as the player sees
     * them.
     */
    record Reroll(List<CourseBlock> removed, List<CourseBlock> added) {
    }

    Pos startPoint() {
        return startPoint;
    }

    /** The main heading: where the course leads overall, bent a little by every block made. */
    Heading heading() {
        return phases.getLast().heading();
    }

    CourseBlock current() {
        return blocks.get(current);
    }

    /**
     * The block after the current one, which the player has to reach next; empty at the very end.
     */
    Optional<CourseBlock> next() {
        return current + 1 < blocks.size() ? Optional.of(blocks.get(current + 1)) : Optional.empty();
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
        double lowest = blocks.subList(current, blocks.size()).stream().mapToDouble(CourseBlock::lowTopY).min().orElseThrow();
        return lowest - FALL_DISTANCE;
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

    /**
     * Makes the blocks after the current one anew, from the same phase: same rules, score, main
     * heading and steering, and as far as the course reached before. Empty, and nothing changed,
     * when there is nothing ahead or no new block fits.
     */
    Optional<Reroll> rerollAhead() {
        int last = blocks.size() - 1;
        List<CourseBlock> removed = List.copyOf(blocks.subList(current + 1, windowEnd() + 1));
        if (removed.isEmpty()) {
            return Optional.empty();
        }
        List<CourseBlock> keptBlocks = List.copyOf(blocks.subList(current + 1, blocks.size()));
        List<Phase> keptPhases = List.copyOf(phases.subList(current + 1, phases.size()));
        int keptAscentJumps = ascentJumps;
        dropAfterCurrent();
        if (generateThrough(last)) {
            return Optional.of(new Reroll(removed, List.copyOf(blocks.subList(current + 1, windowEnd() + 1))));
        }
        dropAfterCurrent();
        blocks.addAll(keptBlocks);
        phases.addAll(keptPhases);
        ascentJumps = keptAscentJumps;
        return Optional.empty();
    }

    private void dropAfterCurrent() {
        blocks.subList(current + 1, blocks.size()).clear();
        phases.subList(current + 1, phases.size()).clear();
    }

    /**
     * Draws another material for each of the given blocks, keeping place and shape. The start block
     * is real and keeps its material.
     *
     * @return the blocks with their new material
     */
    List<CourseBlock> recolor(Collection<CourseBlock> chosen) {
        List<CourseBlock> recolored = new ArrayList<>();
        for (CourseBlock block : chosen) {
            int index = blocks.indexOf(block);
            if (index > 0) {
                CourseBlock redrawn = generator.redrawn(block, CourseGenerator.lastSkin(blocks.subList(0, index)), CourseGenerator.firstSkin(blocks.subList(index + 1, blocks.size())));
                blocks.set(index, redrawn);
                recolored.add(redrawn);
            }
        }
        return recolored;
    }

    /** Whether the feet stand on the current block, edge included. */
    boolean standsOnCurrent(Point feet) {
        return current().supports(feet);
    }

    /** The furthest of the blocks ahead the feet stand on, or -1. */
    private int landedIndex(Point feet) {
        for (int index = blocks.size() - 1; index > current; index--) {
            if (blocks.get(index).supports(feet)) {
                return index;
            }
        }
        return -1;
    }

    private int windowStart() {
        return Math.max(0, current - BEHIND);
    }

    private int windowEnd() {
        return Math.min(blocks.size() - 1, current + AHEAD);
    }

    /** Makes the ascent blocks; false when the ascent does not reach the open in time. */
    private boolean generateAscent() {
        while (phases.getLast() instanceof Phase.Ascent) {
            if (!generateNext()) {
                return false;
            }
        }
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
        Phase phase = phases.getLast();
        Optional<CourseBlock> next = generator.next(blocks, phase);
        next.ifPresent(block -> {
            blocks.add(block);
            Phase after = generator.after(blocks, phase);
            phases.add(after);
            if (phase instanceof Phase.Ascent && after instanceof Phase.Scored) {
                ascentJumps = blocks.size() - 1;
            }
        });
        return next.isPresent();
    }
}

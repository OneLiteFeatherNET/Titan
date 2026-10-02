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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/** Picks the next block of a course: the valid jump that best fits the wanted difficulty. */
final class CourseGenerator {

    /**
     * What a fully walled-in place costs against a fully open one, in cost units: a tie-breaker
     * between similar jumps, not a replacement for the wanted difficulty.
     */
    static final double OPEN_WEIGHT = 1.5;

    /**
     * What a step against the main heading (cosine -1) costs against one along it, in cost units.
     * As heavy as the cost deviation, so the course keeps its direction instead of curling up.
     */
    static final double DIRECTION_WEIGHT = 2.0;

    /** Blocks the dead-end check looks ahead. */
    private static final int LOOKAHEAD = 4;

    /** Places the dead-end check visits at most per candidate. */
    private static final int LOOKAHEAD_BUDGET = 200;

    private final SpaceProbe probe;
    private final SpawnZone spawn;
    private final RandomGenerator random;

    CourseGenerator(SpaceProbe probe, SpawnZone spawn, RandomGenerator random) {
        this.probe = probe;
        this.spawn = spawn;
        this.random = random;
    }

    /**
     * The block after the last of {@code course}, generated for {@code phase}, or empty when no
     * jump fits or every fitting one leads into a dead end.
     */
    Optional<CourseBlock> next(List<CourseBlock> course, Phase phase) {
        CourseBlock from = course.getLast();
        Space space = rulesFor(course);
        List<Spot> free = candidatesFor(from, phase, space).stream().filter(candidate -> space.isFree(from, candidate)).toList();
        return ranked(free, phase, from, space).stream().filter(candidate -> hasFollowUp(course, candidate, phase)).findFirst().map(this::withDrawnMaterial);
    }

    /**
     * The phase for the jump after the last block of {@code course}, which is the one just placed,
     * with the main heading bent towards the step that led there. The ascent ends at a block far
     * from the spawn with plenty of air below it, so the course does not run along ways and roofs,
     * or around the spawn, from then on.
     */
    Phase after(List<CourseBlock> course, Phase phase) {
        return after(course.get(course.size() - 2), course.getLast(), phase, rulesFor(course));
    }

    private Phase after(Placement from, Placement placed, Phase phase, Space space) {
        Phase next = switch (phase) {
            case Phase.Ascent ascent ->
                ascent.next(isInTheOpen(placed.pos(), space, Openness.ASCENT_AIR_BELOW));
            case Phase.Scored scored -> scored.next();
        };
        return next.steered(new Jump(from, placed).direction());
    }

    /** The material is drawn once the position is settled, so it cannot steer the choice. */
    private CourseBlock withDrawnMaterial(Spot spot) {
        return spot.withMaterial(spot.surface().draw(random));
    }

    /**
     * Candidates in the open enough for the phase. After the ascent they need air below, because a
     * block over a way would make the course run along it, and distance to the spawn, so the
     * course does not come back to it. Whether the jump itself is free is left to the caller, which
     * checks it before ranking so openness is only read for free jumps. No candidate leads against
     * the main heading.
     */
    private List<Spot> candidatesFor(Placement from, Phase phase, Space space) {
        if (phase instanceof Phase.Ascent ascent && ascent.isOutOfJumps()) {
            return List.of();
        }
        boolean needsAirBelow = phase instanceof Phase.Scored;
        return candidates(from, phase).stream().filter(candidate -> phase.heading().dot(new Jump(from, candidate).direction()) >= 0.0).filter(candidate -> !needsAirBelow || isInTheOpen(candidate.pos(), space, Openness.SCORED_AIR_BELOW)).toList();
    }

    /** Far from the spawn, with {@code airBelow} air blocks under it. */
    private boolean isInTheOpen(BlockPos pos, Space space, int airBelow) {
        return spawn.isFarEnough(pos) && space.openness().hasAirBelow(pos, airBelow);
    }

    /**
     * Dead-end check: the course must be able to go on for {@link #LOOKAHEAD} more blocks. One
     * block is not enough, because the main heading cannot turn back: a course running along a wall
     * must start turning before the corner, and only a longer look sees that. Beyond the candidate
     * itself the look only follows full blocks, which keeps the search small, and gives up
     * optimistically when it reaches {@link #LOOKAHEAD_BUDGET} places.
     */
    private boolean hasFollowUp(List<CourseBlock> course, Spot candidate, Phase phase) {
        List<Placement> extended = new ArrayList<>(course);
        extended.add(candidate);
        Space space = rulesFor(extended);
        Phase onward = after(course.getLast(), candidate, phase, space);
        return canContinue(extended, onward, LOOKAHEAD, new int[]{LOOKAHEAD_BUDGET});
    }

    private boolean canContinue(List<Placement> course, Phase phase, int depth, int[] budget) {
        if (depth == 0 || budget[0]-- <= 0) {
            return true;
        }
        Placement from = course.getLast();
        Space space = rulesFor(course);
        for (Spot next : candidatesFor(from, phase, space)) {
            if (next.surface() != Surface.FULL && depth < LOOKAHEAD || !space.isFree(from, next)) {
                continue;
            }
            List<Placement> extended = new ArrayList<>(course);
            extended.add(next);
            if (canContinue(extended, after(from, next, phase, rulesFor(extended)), depth - 1, budget)) {
                return true;
            }
        }
        return false;
    }

    /** What a jump has to respect: the world, the visible blocks in it and their surroundings. */
    private record Space(JumpRules rules, Openness openness, List<? extends Placement> visible) {

        boolean isFree(Placement from, Placement to) {
            Jump jump = new Jump(from, to);
            return rules.isFree(jump) && Clearance.isKept(jump, visible);
        }
    }

    /** The real world plus the blocks of the course the player could see. */
    private Space rulesFor(List<? extends Placement> course) {
        List<? extends Placement> visible = course.subList(Math.max(0, course.size() - Course.VISIBLE_BEFORE_NEW), course.size());
        SpaceProbe seen = new OccupiedProbe(probe, occupiedBy(visible));
        return new Space(new JumpRules(seen), new Openness(seen), visible);
    }

    /**
     * What the visible blocks take up: the blocks themselves and the room a player standing on
     * each needs, so a new block cannot land inside someone's headroom.
     */
    private static Set<BlockPos> occupiedBy(List<? extends Placement> visible) {
        Set<BlockPos> occupied = new HashSet<>();
        for (Placement block : visible) {
            for (int y = block.pos().y(); y <= block.headroomTopY(); y++) {
                occupied.add(new BlockPos(block.pos().x(), y, block.pos().z()));
            }
        }
        return occupied;
    }

    /** Every reachable jump the phase allows from the block, before looking at the world. */
    private static List<Spot> candidates(Placement from, Phase phase) {
        int[] gaps = phase.gaps().toArray();
        int[] rises = phase.rises().toArray();
        List<Spot> candidates = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            for (int gap : gaps) {
                for (int rise : rises) {
                    BlockPos pos = from.pos().offset(direction.dx() * (gap + 1), rise, direction.dz() * (gap + 1));
                    phase.surfaces().forEach(surface -> candidates.add(new Spot(pos, surface)));
                }
            }
        }
        return candidates.stream().filter(candidate -> JumpRules.isReachable(new Jump(from, candidate))).toList();
    }

    /** Best candidate first. */
    private List<Spot> ranked(List<Spot> candidates, Phase phase, Placement from, Space space) {
        return switch (phase) {
            case Phase.Scored scored ->
                closestToTargetCost(candidates, scored, from, space.openness());
            case Phase.Ascent ascent ->
                sorted(candidates, candidate -> headingKey(from, candidate, ascent.heading()));
        };
    }

    /**
     * Cost deviation plus a penalty for tight surroundings and one for turning away from the main
     * heading, so among similarly hard candidates the open one in the heading's direction wins.
     * Shuffling before the stable sort lets the random source break ties.
     */
    private List<Spot> closestToTargetCost(List<Spot> candidates, Phase.Scored phase, Placement from, Openness openness) {
        List<Spot> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        double target = Difficulty.targetCost(phase.score(), random);
        Map<BlockPos, Double> opennessAt = new HashMap<>();
        return sorted(shuffled, candidate -> {
            Jump jump = new Jump(from, candidate);
            double turn = (1.0 - phase.heading().dot(jump.direction())) / 2.0;
            return Math.abs(jump.cost() - target) + OPEN_WEIGHT * (1.0 - opennessAt.computeIfAbsent(candidate.pos(), openness::of)) + DIRECTION_WEIGHT * turn;
        });
    }

    /**
     * Weighted random order by the cosine to the heading (Efraimidis-Spirakis: lower key first).
     * Candidates that do not point along the heading come after all that do.
     */
    private double headingKey(Placement from, Spot candidate, Heading heading) {
        double weight = heading.dot(new Jump(from, candidate).direction());
        double draw = 1.0 - random.nextDouble();
        return weight > 0 ? -Math.pow(draw, 1.0 / weight) : 1.0 + draw;
    }

    private static List<Spot> sorted(List<Spot> blocks, ToDoubleFunction<Spot> key) {
        record Keyed(Spot block, double key) {
        }
        return blocks.stream().map(block -> new Keyed(block, key.applyAsDouble(block))).sorted(Comparator.comparingDouble(Keyed::key)).map(Keyed::block).toList();
    }
}

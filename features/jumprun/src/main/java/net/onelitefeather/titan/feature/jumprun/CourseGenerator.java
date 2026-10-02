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

    /**
     * Places the dead-end check visits at most per {@link #next} call, shared by all candidates:
     * the best candidates come first and usually pass within a handful of places, so one budget
     * bounds the worst case, a ring of dead ends, instead of multiplying it per candidate.
     */
    private static final int LOOKAHEAD_BUDGET = 400;

    /** The deepest air column anything asks for, so one read per position serves every check. */
    private static final int MAX_AIR_BELOW = Math.max(Openness.SCORED_AIR_BELOW, Openness.ASCENT_AIR_BELOW);

    private final SpaceProbe probe;
    private final SpawnZone spawn;
    private final RandomGenerator random;
    private final Palettes palettes;
    private final PortalClearance portals;
    private final Steering steering;

    CourseGenerator(SpaceProbe probe, SpawnZone spawn, RandomGenerator random, Palettes palettes, PortalClearance portals, Steering steering) {
        this.probe = probe;
        this.spawn = spawn;
        this.random = random;
        this.palettes = palettes;
        this.portals = portals;
        this.steering = steering;
    }

    /**
     * The block after the last of {@code course}, generated for {@code phase}, or empty when no
     * jump fits or every fitting one leads into a dead end.
     */
    Optional<CourseBlock> next(List<CourseBlock> course, Phase phase) {
        return new Search().next(course, phase);
    }

    /**
     * The phase for the jump after the last block of {@code course}, which is the one just placed,
     * with the main heading bent towards the step that led there during the ascent, and towards
     * the {@link Steering}'s choice once the course is scored. The ascent ends at a block far
     * from the spawn with plenty of air below it, so the course does not run along ways and roofs,
     * or around the spawn, from then on.
     *
     * @throws IllegalArgumentException when the course has fewer than two blocks, so no jump led
     *                                  to the last one
     */
    Phase after(List<CourseBlock> course, Phase phase) {
        if (course.size() < 2) {
            throw new IllegalArgumentException("a course of " + course.size() + " blocks has no jump to steer by");
        }
        return new Search().after(course.get(course.size() - 2), course.getLast(), phase);
    }

    /** The material is drawn once the position is settled, so it cannot steer the choice. */
    private CourseBlock withDrawnMaterial(Spot spot) {
        return spot.withMaterial(palettes.draw(spot.surface(), random));
    }

    /**
     * The work of one {@link #next} call. It holds what is worth sharing between the many places
     * the dead-end check visits: the air below a position, read once, and the place budget.
     */
    private final class Search {

        private final Openness world = new Openness(probe);
        private final Map<BlockPos, Integer> airBelow = new HashMap<>();
        private int budget = LOOKAHEAD_BUDGET;

        Optional<CourseBlock> next(List<CourseBlock> course, Phase phase) {
            CourseBlock from = course.getLast();
            Space space = new Space(visibleTail(course));
            List<Spot> free = candidatesFor(from, phase, false).stream().filter(candidate -> space.isFree(from, candidate)).toList();
            return ranked(free, phase, from, space).stream().filter(candidate -> hasFollowUp(course, candidate, phase)).findFirst().map(CourseGenerator.this::withDrawnMaterial);
        }

        Phase after(Placement from, Placement placed, Phase phase) {
            return switch (phase) {
                case Phase.Ascent ascent -> {
                    Direction step = new Jump(from, placed).direction();
                    yield ascent.next(isInTheOpen(placed.pos(), Openness.ASCENT_AIR_BELOW)).withHeading(ascent.heading().steered(step));
                }
                case Phase.Scored scored -> {
                    Direction step = new Jump(from, placed).direction();
                    yield scored.next().withHeading(steering.follow(scored.heading(), step, placed.pos(), scored.score() + 1));
                }
            };
        }

        /**
         * Candidates in the open enough for the phase. After the ascent they need air below,
         * because
         * a block over a way would make the course run along it, and distance to the spawn, so the
         * course does not come back to it. Whether the jump itself is free is left to the caller,
         * which checks it before ranking so openness is only read for free jumps. No candidate
         * leads against the main heading. {@code fullOnly} narrows the shapes to full blocks, the
         * only ones the dead-end check follows.
         */
        private List<Spot> candidatesFor(Placement from, Phase phase, boolean fullOnly) {
            if (phase instanceof Phase.Ascent ascent && ascent.isOutOfJumps()) {
                return List.of();
            }
            boolean needsAirBelow = phase instanceof Phase.Scored;
            List<Surface> surfaces = fullOnly ? List.of(Surface.FULL) : phase.surfaces();
            return candidates(from, phase, surfaces).stream().filter(candidate -> phase.heading().dot(new Jump(from, candidate).direction()) >= 0.0).filter(candidate -> !needsAirBelow || isInTheOpen(candidate.pos(), Openness.SCORED_AIR_BELOW)).toList();
        }

        /**
         * Far from the spawn, with {@code blocks} air blocks under it. Read from the real world:
         * the
         * visible blocks keep their distance to a target, so none stands in the column below it.
         */
        private boolean isInTheOpen(BlockPos pos, int blocks) {
            return spawn.isFarEnough(pos) && airBelow.computeIfAbsent(pos, at -> world.airDepthBelow(at, MAX_AIR_BELOW)) >= blocks;
        }

        /**
         * Dead-end check: the course must be able to go on for {@link #LOOKAHEAD} more blocks. One
         * block is not enough, because the main heading cannot turn back: a course running along a
         * wall must start turning before the corner, and only a longer look sees that. The look
         * only follows full blocks, which keeps the search small. When the budget is used up it
         * says yes: ending a run needlessly is worse than meeting a dead end later, which the spec
         * allows.
         */
        private boolean hasFollowUp(List<CourseBlock> course, Spot candidate, Phase phase) {
            return canContinue(visibleTail(course, candidate), after(course.getLast(), candidate, phase), LOOKAHEAD);
        }

        private boolean canContinue(List<Placement> window, Phase phase, int depth) {
            if (depth == 0 || budget-- <= 0) {
                return true;
            }
            Placement from = window.getLast();
            Space space = new Space(window);
            for (Spot next : candidatesFor(from, phase, true)) {
                if (space.isFree(from, next) && canContinue(visibleTail(window, next), after(from, next, phase), depth - 1)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** The last blocks of the course, the ones the player could see. */
    private static List<Placement> visibleTail(List<? extends Placement> course) {
        return List.copyOf(course.subList(Math.max(0, course.size() - Course.VISIBLE_BEFORE_NEW), course.size()));
    }

    /** The visible blocks once {@code added} has been placed after the course. */
    private static List<Placement> visibleTail(List<? extends Placement> course, Placement added) {
        List<Placement> visible = new ArrayList<>(course.subList(Math.max(0, course.size() - Course.VISIBLE_BEFORE_NEW + 1), course.size()));
        visible.add(added);
        return visible;
    }

    /**
     * What a jump has to respect: the visible blocks and, around the real world, the room they
     * take. The world side is built on first use, because most candidates fail on geometry alone.
     */
    private final class Space {

        private final List<Placement> visible;
        private SpaceProbe seen;
        private JumpRules rules;

        Space(List<Placement> visible) {
            this.visible = visible;
        }

        /** Geometry first, then the world: the cheap check decides most candidates. */
        boolean isFree(Placement from, Placement to) {
            Jump jump = new Jump(from, to);
            return Clearance.isKept(jump, visible) && portals.isKept(jump) && rules().isFree(jump);
        }

        Openness openness() {
            return new Openness(seen());
        }

        private JumpRules rules() {
            if (rules == null) {
                rules = new JumpRules(seen());
            }
            return rules;
        }

        private SpaceProbe seen() {
            if (seen == null) {
                seen = new OccupiedProbe(probe, occupiedBy(visible));
            }
            return seen;
        }
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
    private static List<Spot> candidates(Placement from, Phase phase, List<Surface> surfaces) {
        int[] gaps = phase.gaps().toArray();
        int[] rises = phase.rises().toArray();
        List<Spot> candidates = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            for (int gap : gaps) {
                for (int rise : rises) {
                    BlockPos pos = from.pos().offset(direction.dx() * (gap + 1), rise, direction.dz() * (gap + 1));
                    surfaces.forEach(surface -> candidates.add(new Spot(pos, surface)));
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

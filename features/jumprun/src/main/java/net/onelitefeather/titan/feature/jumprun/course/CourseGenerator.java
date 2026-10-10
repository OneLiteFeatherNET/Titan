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
package net.onelitefeather.titan.feature.jumprun.course;

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
import java.util.stream.Stream;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.head.HeadSkin;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.Direction;
import net.onelitefeather.titan.feature.jumprun.space.Heading;
import net.onelitefeather.titan.feature.jumprun.space.OccupiedProbe;
import net.onelitefeather.titan.feature.jumprun.space.Openness;
import net.onelitefeather.titan.feature.jumprun.space.SpaceProbe;
import net.onelitefeather.titan.feature.jumprun.space.SpawnZone;
import net.onelitefeather.titan.feature.jumprun.space.Steering;

/** Picks the next block of a course: the valid jump that best fits the wanted difficulty. */
public final class CourseGenerator {

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

    /** The block of every team head; the skin comes with the course block. */
    private static final Block TEAM_HEAD = Block.PLAYER_HEAD;

    private final SpaceProbe probe;
    private final HeightBand band;
    private final SpawnZone spawn;
    private final RandomGenerator random;
    private final Palettes palettes;
    private final PortalClearance portals;
    private final Steering steering;

    CourseGenerator(SpaceProbe probe, HeightBand band, SpawnZone spawn, RandomGenerator random, Palettes palettes, PortalClearance portals, Steering steering) {
        this.probe = probe;
        this.band = band;
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

    /**
     * The block at the same place with the same shape and look and another material of the
     * palette; a team head keeps its block and shows another skin, never that of the head before
     * or after it in the course.
     */
    public CourseBlock redrawn(CourseBlock block, Optional<HeadSkin> before, Optional<HeadSkin> after) {
        if (block.skin().isPresent()) {
            List<HeadSkin> neighbours = Stream.concat(before.stream(), after.stream()).toList();
            return new CourseBlock(block.pos(), block.surface(), block.material(), palettes.drawHead(neighbours, block.skin().stream().toList(), random), block.tower());
        }
        Block drawn = palettes.of(block.surface()).drawOther(block.material(), random);
        return new CourseBlock(block.pos(), block.surface(), block.surface().withLookOf(block.material(), drawn), Optional.empty(), block.tower());
    }

    /**
     * The material, its look and the team skin are drawn once the position is settled, so they
     * cannot steer the choice. Another skin than the last head shown comes first.
     */
    private CourseBlock withDrawnMaterial(Spot spot, List<CourseBlock> course) {
        Surface surface = spot.surface();
        Optional<HeadSkin> skin = surface == Surface.HEAD ? palettes.drawHead(lastSkin(course).stream().toList(), List.of(), random) : Optional.empty();
        Block material = skin.isPresent() ? TEAM_HEAD : palettes.draw(surface, random);
        Block climbing = spot.climb().map(climb -> palettes.drawClimb(climb.kind(), random)).orElse(Block.AIR);
        return spot.withMaterial(surface.varied(material, random), skin, climbing);
    }

    static Optional<HeadSkin> lastSkin(List<CourseBlock> course) {
        return course.reversed().stream().flatMap(block -> block.skin().stream()).findFirst();
    }

    static Optional<HeadSkin> firstSkin(List<CourseBlock> course) {
        return course.stream().flatMap(block -> block.skin().stream()).findFirst();
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
            return ranked(free, phase, from, space).stream().filter(candidate -> hasFollowUp(course, candidate, phase)).findFirst().map(candidate -> withDrawnMaterial(candidate, course));
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
            return candidates(from, phase, surfaces, !fullOnly).stream().filter(candidate -> phase.heading().dot(new Jump(from, candidate).direction()) >= 0.0).filter(candidate -> !needsAirBelow || isInTheOpen(candidate.pos(), Openness.SCORED_AIR_BELOW)).toList();
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

        /** The visible blocks in course order; the last one is the block every jump leaves from. */
        private final List<Placement> visible;
        private SpaceProbe seen;
        private JumpRules rules;
        private JumpRules climbRules;

        Space(List<Placement> visible) {
            this.visible = visible;
        }

        /** Geometry first, then the world: the cheap check decides most candidates. */
        boolean isFree(Placement from, Placement to) {
            Jump jump = new Jump(from, to);
            return Clearance.isKept(jump, visible) && portals.isKept(jump) && rulesFor(jump).isFree(jump);
        }

        Openness openness() {
            return new Openness(seen());
        }

        /**
         * A tower climbs in the room of the block it leaves from, which that block keeps free for
         * its own jump, so the tower is checked without that room.
         */
        private JumpRules rulesFor(Jump jump) {
            if (jump.to().climb().isPresent()) {
                if (climbRules == null) {
                    climbRules = new JumpRules(new OccupiedProbe(probe, occupiedBy(visible.subList(0, visible.size() - 1))), band);
                }
                return climbRules;
            }
            if (rules == null) {
                rules = new JumpRules(seen(), band);
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
     * each needs to jump, so a new block cannot land inside someone's jump room.
     */
    private static Set<BlockPos> occupiedBy(List<? extends Placement> visible) {
        Set<BlockPos> occupied = new HashSet<>();
        for (Placement block : visible) {
            for (int y = block.pos().y(); y <= block.jumpRoomTopY(); y++) {
                occupied.add(new BlockPos(block.pos().x(), y, block.pos().z()));
            }
            block.attachments().forEach(cell -> occupied.add(cell.pos()));
        }
        return occupied;
    }

    /** Every reachable jump the phase allows from the block, before looking at the world. */
    private List<Spot> candidates(Placement from, Phase phase, List<Surface> surfaces, boolean withTowers) {
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
        if (withTowers && surfaces.contains(Surface.FULL)) {
            candidates.addAll(towers(from, phase));
        }
        return candidates.stream().filter(candidate -> JumpRules.isReachable(new Jump(from, candidate), phase.mode())).toList();
    }

    /**
     * A tower on each axis for each climbing block the phase unlocks, as high as the score asks for
     * (see {@link Difficulty#towerHeight}). A tower that would not fit the height band is shortened
     * step by step, down to the lowest allowed height.
     */
    private List<Spot> towers(Placement from, Phase phase) {
        if (!(phase instanceof Phase.Scored scored)) {
            return List.of();
        }
        int wanted = Difficulty.towerHeight(scored.mode(), scored.score(), palettes.minClimbHeight(), palettes.maxClimbHeight());
        List<Spot> towers = new ArrayList<>();
        for (Climb.Kind kind : phase.climbs()) {
            for (Direction direction : Climb.DIRECTIONS) {
                for (int height = wanted; height >= palettes.minClimbHeight(); height--) {
                    Spot tower = tower(from, direction, height, kind);
                    if (band.allows(tower)) {
                        towers.add(tower);
                        break;
                    }
                }
            }
        }
        return towers;
    }

    private static Spot tower(Placement from, Direction direction, int height, Climb.Kind kind) {
        BlockPos target = from.pos().offset(direction.dx(), height, direction.dz());
        return new Spot(target, Surface.FULL, Optional.of(new Climb(direction, height, kind)));
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
        double target = Difficulty.targetCost(phase.mode(), phase.score(), random);
        Map<BlockPos, Double> opennessAt = new HashMap<>();
        return sorted(shuffled, candidate -> {
            Jump jump = new Jump(from, candidate);
            double turn = (1.0 - phase.heading().dot(jump.direction())) / 2.0;
            return Math.abs(jump.cost(phase.mode()) - target) + OPEN_WEIGHT * (1.0 - opennessAt.computeIfAbsent(candidate.pos(), openness::of)) + DIRECTION_WEIGHT * turn;
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

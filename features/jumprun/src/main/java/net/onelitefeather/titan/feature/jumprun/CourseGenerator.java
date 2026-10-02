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

    private final SpaceProbe probe;
    private final RandomGenerator random;

    CourseGenerator(SpaceProbe probe, RandomGenerator random) {
        this.probe = probe;
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
     * The phase for the jump after the last block of {@code course}, which is the one just placed.
     * The ascent ends at a block with air below it, so the course does not run along ways and
     * roofs from then on.
     */
    Phase after(List<CourseBlock> course, Phase phase) {
        return after(course.getLast(), phase, rulesFor(course));
    }

    private static Phase after(Placement placed, Phase phase, Space space) {
        return switch (phase) {
            case Phase.Ascent ascent -> ascent.next(space.openness().hasAirBelow(placed.pos()));
            case Phase.Scored scored -> scored.next();
        };
    }

    /** The material is drawn once the position is settled, so it cannot steer the choice. */
    private CourseBlock withDrawnMaterial(Spot spot) {
        return spot.withMaterial(spot.surface().draw(random));
    }

    /**
     * Candidates in the open enough for the phase. After the ascent they need air below, because a
     * block over a way would make the course run along it. Whether the jump itself is free is
     * left to the caller, which checks it before ranking so openness is only read for free jumps.
     */
    private List<Spot> candidatesFor(Placement from, Phase phase, Space space) {
        if (phase instanceof Phase.Ascent ascent && ascent.isOutOfJumps()) {
            return List.of();
        }
        boolean needsAirBelow = phase instanceof Phase.Scored;
        return candidates(from, phase).stream().filter(candidate -> !needsAirBelow || space.openness().hasAirBelow(candidate.pos())).toList();
    }

    /** Dead-end check of depth one: some free jump must leave the candidate. */
    private boolean hasFollowUp(List<CourseBlock> course, Spot candidate, Phase phase) {
        List<Placement> extended = new ArrayList<>(course);
        extended.add(candidate);
        Space space = rulesFor(extended);
        return candidatesFor(candidate, after(candidate, phase, space), space).stream().anyMatch(next -> space.isFree(candidate, next));
    }

    private record Space(JumpRules rules, Openness openness) {

        boolean isFree(Placement from, Placement to) {
            return rules.isFree(new Jump(from, to));
        }
    }

    /** The real world plus the blocks of the course the player could see. */
    private Space rulesFor(List<? extends Placement> course) {
        SpaceProbe seen = new OccupiedProbe(probe, occupiedBy(course));
        return new Space(new JumpRules(seen), new Openness(seen));
    }

    /**
     * What the visible blocks take up: the blocks themselves and the room a player standing on
     * each needs, so a new block cannot land inside someone's headroom.
     */
    private static Set<BlockPos> occupiedBy(List<? extends Placement> course) {
        List<? extends Placement> visible = course.subList(Math.max(0, course.size() - Course.VISIBLE_BEFORE_NEW), course.size());
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
     * Cost deviation plus a penalty for tight surroundings, so among similarly hard candidates the
     * open one wins. Shuffling before the stable sort lets the random source break ties.
     */
    private List<Spot> closestToTargetCost(List<Spot> candidates, Phase.Scored phase, Placement from, Openness openness) {
        List<Spot> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        double target = Difficulty.targetCost(phase.score(), random);
        Map<BlockPos, Double> opennessAt = new HashMap<>();
        return sorted(shuffled, candidate -> Math.abs(new Jump(from, candidate).cost() - target) + OPEN_WEIGHT * (1.0 - opennessAt.computeIfAbsent(candidate.pos(), openness::of)));
    }

    /**
     * Weighted random order by the cosine to the heading (Efraimidis-Spirakis: lower key first).
     * Candidates that do not point along the heading come after all that do.
     */
    private double headingKey(Placement from, Spot candidate, Heading heading) {
        Direction direction = Direction.toward(candidate.pos().x() - from.pos().x(), candidate.pos().z() - from.pos().z());
        double weight = heading.dot(direction);
        double draw = 1.0 - random.nextDouble();
        return weight > 0 ? -Math.pow(draw, 1.0 / weight) : 1.0 + draw;
    }

    private static List<Spot> sorted(List<Spot> blocks, ToDoubleFunction<Spot> key) {
        record Keyed(Spot block, double key) {
        }
        return blocks.stream().map(block -> new Keyed(block, key.applyAsDouble(block))).sorted(Comparator.comparingDouble(Keyed::key)).map(Keyed::block).toList();
    }
}

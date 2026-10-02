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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/** Picks the next block of a course: the valid jump that best fits the wanted difficulty. */
final class CourseGenerator {

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
        JumpRules rules = rulesFor(course);
        Phase following = phase.next();
        return ranked(candidates(from, phase), phase, from).stream().filter(candidate -> rules.isFree(new Jump(from, candidate))).filter(candidate -> hasFollowUp(course, candidate, following)).findFirst().map(this::withDrawnMaterial);
    }

    /** The material is drawn once the position is settled, so it cannot steer the choice. */
    private CourseBlock withDrawnMaterial(CourseBlock block) {
        return block.withMaterial(block.surface().draw(random));
    }

    /** Dead-end check of depth one: some free jump must leave the candidate. */
    private boolean hasFollowUp(List<CourseBlock> course, CourseBlock candidate, Phase following) {
        List<CourseBlock> extended = new ArrayList<>(course);
        extended.add(candidate);
        JumpRules rules = rulesFor(extended);
        return candidates(candidate, following).stream().anyMatch(next -> rules.isFree(new Jump(candidate, next)));
    }

    /** The real world plus the blocks of the course the player could see. */
    private JumpRules rulesFor(List<CourseBlock> course) {
        return new JumpRules(new OccupiedProbe(probe, occupiedBy(course)));
    }

    /**
     * What the visible blocks take up: the blocks themselves and the room a player standing on
     * each needs, so a new block cannot land inside someone's headroom.
     */
    private static Set<BlockPos> occupiedBy(List<CourseBlock> course) {
        List<CourseBlock> visible = course.subList(Math.max(0, course.size() - Course.VISIBLE_BEFORE_NEW), course.size());
        Set<BlockPos> occupied = new HashSet<>();
        for (CourseBlock block : visible) {
            for (int y = block.pos().y(); y <= block.headroomTopY(); y++) {
                occupied.add(new BlockPos(block.pos().x(), y, block.pos().z()));
            }
        }
        return occupied;
    }

    /** Every reachable jump the phase allows from the block, before looking at the world. */
    private static List<CourseBlock> candidates(CourseBlock from, Phase phase) {
        int[] gaps = phase.gaps().toArray();
        int[] rises = phase.rises().toArray();
        List<CourseBlock> candidates = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            for (int gap : gaps) {
                for (int rise : rises) {
                    BlockPos pos = from.pos().offset(direction.dx() * (gap + 1), rise, direction.dz() * (gap + 1));
                    phase.surfaces().forEach(surface -> candidates.add(new CourseBlock(pos, surface)));
                }
            }
        }
        return candidates.stream().filter(candidate -> JumpRules.isReachable(new Jump(from, candidate))).toList();
    }

    /** Best candidate first. */
    private List<CourseBlock> ranked(List<CourseBlock> candidates, Phase phase, CourseBlock from) {
        return switch (phase) {
            case Phase.Scored scored -> closestToTargetCost(candidates, scored, from);
            case Phase.Ascent ascent ->
                sorted(candidates, candidate -> headingKey(from, candidate, ascent.heading()));
        };
    }

    /** Shuffling before the stable sort lets the random source break ties between equal costs. */
    private List<CourseBlock> closestToTargetCost(List<CourseBlock> candidates, Phase.Scored phase, CourseBlock from) {
        List<CourseBlock> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        double target = Difficulty.targetCost(phase.score(), random);
        return sorted(shuffled, candidate -> Math.abs(new Jump(from, candidate).cost() - target));
    }

    /**
     * Weighted random order by the cosine to the heading (Efraimidis-Spirakis: lower key first).
     * Candidates that do not point along the heading come after all that do.
     */
    private double headingKey(CourseBlock from, CourseBlock candidate, Heading heading) {
        Direction direction = Direction.toward(candidate.pos().x() - from.pos().x(), candidate.pos().z() - from.pos().z());
        double weight = heading.dot(direction);
        double draw = 1.0 - random.nextDouble();
        return weight > 0 ? -Math.pow(draw, 1.0 / weight) : 1.0 + draw;
    }

    private static List<CourseBlock> sorted(List<CourseBlock> blocks, ToDoubleFunction<CourseBlock> key) {
        record Keyed(CourseBlock block, double key) {
        }
        return blocks.stream().map(block -> new Keyed(block, key.applyAsDouble(block))).sorted(Comparator.comparingDouble(Keyed::key)).map(Keyed::block).toList();
    }
}

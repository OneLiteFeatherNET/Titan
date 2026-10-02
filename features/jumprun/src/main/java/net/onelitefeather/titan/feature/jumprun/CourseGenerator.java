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

    /** Blocks a player can see at once: two behind, the current one and two ahead, plus the new one. */
    private static final int OCCUPIED_SPAN = 4;

    private static final List<int[]> DIRECTIONS = List.of(
            new int[] {1, 0}, new int[] {-1, 0}, new int[] {0, 1}, new int[] {0, -1},
            new int[] {1, 1}, new int[] {1, -1}, new int[] {-1, 1}, new int[] {-1, -1});
    private static final List<Integer> ASCENT_GAPS = List.of(1, 2);
    private static final List<Integer> SCORED_GAPS = List.of(1, 2, 3, 4);
    private static final List<Integer> ASCENT_RISES = List.of(1);
    private static final List<Integer> SCORED_RISES = List.of(-1, 0, 1);

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
        return rank(candidates(from, phase), phase, from).stream()
                .filter(candidate -> rules.isValid(new Jump(from, candidate)))
                .filter(candidate -> hasFollowUp(course, candidate, phase.next()))
                .findFirst();
    }

    /** Dead-end check of depth one: some valid jump must leave the candidate. */
    private boolean hasFollowUp(List<CourseBlock> course, CourseBlock candidate, Phase following) {
        List<CourseBlock> extended = new ArrayList<>(course);
        extended.add(candidate);
        JumpRules rules = rulesFor(extended);
        return candidates(candidate, following).stream().anyMatch(next -> rules.isValid(new Jump(candidate, next)));
    }

    /** The real world plus the blocks of the course the player could see, which are not in the world. */
    private JumpRules rulesFor(List<CourseBlock> course) {
        Set<BlockPos> window = new HashSet<>();
        course.subList(Math.max(0, course.size() - OCCUPIED_SPAN), course.size())
                .forEach(block -> window.add(block.pos()));
        return new JumpRules(new SpaceProbe() {
            @Override
            public boolean isAir(BlockPos pos) {
                return !window.contains(pos) && probe.isAir(pos);
            }

            @Override
            public boolean inBounds(BlockPos pos) {
                return probe.inBounds(pos);
            }
        });
    }

    /** Every jump the phase allows from the block, before looking at the world. */
    private static List<CourseBlock> candidates(CourseBlock from, Phase phase) {
        List<Integer> gaps = switch (phase) {
            case Phase.Ascent _ -> ASCENT_GAPS;
            case Phase.Scored _ -> SCORED_GAPS;
        };
        List<Integer> rises = switch (phase) {
            case Phase.Ascent _ -> ASCENT_RISES;
            case Phase.Scored _ -> SCORED_RISES;
        };
        List<Surface> surfaces = switch (phase) {
            case Phase.Ascent _ -> List.of(Surface.FULL);
            case Phase.Scored _ -> List.of(Surface.values());
        };
        List<CourseBlock> candidates = new ArrayList<>();
        for (int[] direction : DIRECTIONS) {
            for (int gap : gaps) {
                for (int rise : rises) {
                    BlockPos pos = from.pos().offset(direction[0] * (gap + 1), rise, direction[1] * (gap + 1));
                    for (Surface surface : surfaces) {
                        candidates.add(new CourseBlock(pos, surface));
                    }
                }
            }
        }
        return candidates.stream().filter(c -> JumpRules.isReachable(new Jump(from, c))).toList();
    }

    /** Best candidate first. Shuffling before the stable sort lets the random source break ties. */
    private List<CourseBlock> rank(List<CourseBlock> candidates, Phase phase, CourseBlock from) {
        List<CourseBlock> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        return switch (phase) {
            case Phase.Scored scored -> {
                double target = Difficulty.targetCost(scored.score(), random);
                yield sorted(shuffled, c -> Math.abs(new Jump(from, c).cost() - target));
            }
            case Phase.Ascent ascent -> sorted(shuffled, c -> headingKey(from, c, ascent.heading()));
        };
    }

    /**
     * Weighted random order by the cosine to the heading (Efraimidis-Spirakis: lower key first).
     * Candidates that do not point along the heading come after all that do.
     */
    private double headingKey(CourseBlock from, CourseBlock candidate, Heading heading) {
        int dx = Integer.signum(candidate.pos().x() - from.pos().x());
        int dz = Integer.signum(candidate.pos().z() - from.pos().z());
        double weight = heading.dot(dx, dz);
        double draw = 1.0 - random.nextDouble();
        return weight > 0 ? -Math.pow(draw, 1.0 / weight) : 1.0 + draw;
    }

    private static List<CourseBlock> sorted(List<CourseBlock> blocks, ToDoubleFunction<CourseBlock> key) {
        record Keyed(CourseBlock block, double key) {
        }
        return blocks.stream()
                .map(block -> new Keyed(block, key.applyAsDouble(block)))
                .sorted(Comparator.comparingDouble(Keyed::key))
                .map(Keyed::block)
                .toList();
    }
}

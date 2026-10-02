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

import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;
import net.minestom.server.instance.block.Block;

/** Materials of one shape with a weight each; a heavier one is drawn proportionally more often. */
final class Palette {

    /** A material and how often it is drawn relative to the others. */
    record Weighted(Block block, int weight) {
    }

    private final List<Block> blocks;
    /** Running sum of the weights, so the entry for a draw is a binary search. */
    private final long[] cumulative;

    private Palette(List<Block> blocks, long[] cumulative) {
        this.blocks = blocks;
        this.cumulative = cumulative;
    }

    /**
     * @throws IllegalArgumentException when there is no entry or a weight is not positive
     */
    static Palette of(List<Weighted> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("a palette needs at least one material");
        }
        long[] cumulative = new long[entries.size()];
        long total = 0;
        for (int i = 0; i < cumulative.length; i++) {
            if (entries.get(i).weight() <= 0) {
                throw new IllegalArgumentException("weight of " + entries.get(i).block().name() + " must be positive");
            }
            total += entries.get(i).weight();
            cumulative[i] = total;
        }
        return new Palette(entries.stream().map(Weighted::block).toList(), cumulative);
    }

    List<Block> blocks() {
        return blocks;
    }

    Block draw(RandomGenerator random) {
        long pick = random.nextLong(cumulative[cumulative.length - 1]);
        int found = Arrays.binarySearch(cumulative, pick);
        // The first running sum above the pick owns it; a hit on a sum belongs to the next entry.
        return blocks.get(found >= 0 ? found + 1 : -found - 1);
    }

    /**
     * A material other than {@code excluded}, weighted like {@link #draw}; the only one a
     * single-entry palette has is returned as it is.
     */
    Block drawOther(Block excluded, RandomGenerator random) {
        int skipped = blocks.indexOf(excluded);
        if (skipped < 0 || blocks.size() == 1) {
            return draw(random);
        }
        long pick = random.nextLong(cumulative[cumulative.length - 1] - weightAt(skipped));
        long running = 0;
        for (int i = 0; i < blocks.size(); i++) {
            if (i != skipped) {
                running += weightAt(i);
                if (pick < running) {
                    return blocks.get(i);
                }
            }
        }
        throw new IllegalStateException("pick " + pick + " is beyond the weights");
    }

    private long weightAt(int index) {
        return cumulative[index] - (index == 0 ? 0 : cumulative[index - 1]);
    }
}

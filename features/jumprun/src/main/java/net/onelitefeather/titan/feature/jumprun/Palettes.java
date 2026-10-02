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

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minestom.server.instance.block.Block;

/**
 * The materials a {@link Surface} can show. Every entry of a palette collides up to the same
 * height, so the material never changes what a jump demands.
 */
final class Palettes {

    private static final List<String> COLORS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped");

    private Palettes() {
    }

    static List<Block> full() {
        return Stream.of(colored("%s_concrete"), colored("%s_wool"), Stream.of(block("terracotta"))).flatMap(blocks -> blocks).toList();
    }

    static List<Block> trapdoor() {
        return Stream.concat(wooden("%s_trapdoor"), Stream.of(block("iron_trapdoor"))).map(block -> block.withProperty("half", "bottom").withProperty("open", "false")).toList();
    }

    static List<Block> slab() {
        Stream<Block> stone = Stream.of("stone_slab", "cobblestone_slab", "stone_brick_slab", "brick_slab", "nether_brick_slab", "quartz_slab").map(Palettes::block);
        return Stream.concat(stone, wooden("%s_slab")).map(block -> block.withProperty("type", "bottom")).toList();
    }

    static List<Block> fence() {
        Stream<Block> other = Stream.of("nether_brick_fence", "cobblestone_wall", "brick_wall", "stone_brick_wall").map(Palettes::block);
        return Stream.concat(wooden("%s_fence"), other).toList();
    }

    static List<Block> pane() {
        return Stream.concat(colored("%s_stained_glass_pane"), Stream.of(block("iron_bars"))).toList();
    }

    static List<Block> post() {
        return List.of(block("end_rod").withProperty("facing", "up"), block("iron_chain").withProperty("axis", "y"), block("lightning_rod").withProperty("facing", "up"));
    }

    private static Stream<Block> colored(String pattern) {
        return COLORS.stream().map(color -> block(pattern.formatted(color)));
    }

    private static Stream<Block> wooden(String pattern) {
        return WOODS.stream().map(wood -> block(pattern.formatted(wood)));
    }

    /** Fails loudly when this Minestom version has no block of that name. */
    private static Block block(String name) {
        return Objects.requireNonNull(Block.fromKey("minecraft:" + name), "unknown block " + name);
    }
}

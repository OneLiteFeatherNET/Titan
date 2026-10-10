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

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.head.HeadSkin;

/**
 * The materials a {@link Surface} can show, one {@link Palette} per shape. Every entry of a
 * palette collides up to the shape's height, so the material never changes what a jump demands.
 * Built from the config by {@link JumprunSettings}. The skins of the team heads, when there are
 * any, replace the materials of the head shape.
 */
public final class Palettes {

    private final Map<Surface, Palette> byShape;
    private final Map<Climb.Kind, Palette> climbing;
    private final List<HeadSkin> heads;
    private final int minClimbHeight;
    private final int maxClimbHeight;

    /**
     * @throws IllegalArgumentException when a shape or a climbing block has no palette
     */
    public Palettes(Map<Surface, Palette> byShape, Map<Climb.Kind, Palette> climbing) {
        this(byShape, climbing, List.of(), Climb.MIN_HEIGHT, Climb.MAX_HEIGHT);
    }

    private Palettes(Map<Surface, Palette> byShape, Map<Climb.Kind, Palette> climbing, List<HeadSkin> heads, int minClimbHeight, int maxClimbHeight) {
        for (Surface surface : Surface.values()) {
            if (!byShape.containsKey(surface)) {
                throw new IllegalArgumentException("no palette for " + surface);
            }
        }
        for (Climb.Kind kind : Climb.Kind.values()) {
            if (!climbing.containsKey(kind)) {
                throw new IllegalArgumentException("no palette for " + kind);
            }
        }
        this.byShape = new EnumMap<>(byShape);
        this.climbing = new EnumMap<>(climbing);
        this.heads = List.copyOf(heads);
        this.minClimbHeight = minClimbHeight;
        this.maxClimbHeight = maxClimbHeight;
    }

    /** The team heads that replace the plain heads; empty when there are none. */
    public List<HeadSkin> heads() {
        return heads;
    }

    /** The same palettes with the given team heads. */
    public Palettes withHeads(List<HeadSkin> heads) {
        return new Palettes(byShape, climbing, heads, minClimbHeight, maxClimbHeight);
    }

    /** The same palettes with the given range of tower heights, in blocks. */
    public Palettes withClimbHeights(int min, int max) {
        return new Palettes(byShape, climbing, heads, min, max);
    }

    /** The lowest tower the generator makes, in blocks. */
    public int minClimbHeight() {
        return minClimbHeight;
    }

    /** The highest tower the generator makes, in blocks. */
    public int maxClimbHeight() {
        return maxClimbHeight;
    }

    public Palette climbing(Climb.Kind kind) {
        return climbing.get(kind);
    }

    Block drawClimb(Climb.Kind kind, RandomGenerator random) {
        return climbing(kind).draw(random);
    }

    public Palette of(Surface surface) {
        return byShape.get(surface);
    }

    /**
     * A skin of the team that is none of {@code mustAvoid} and, while another one is left, none of
     * {@code preferToAvoid}; any skin when every one is to be avoided. Empty when there are no team
     * heads and the plain heads of the palette show.
     */
    Optional<HeadSkin> drawHead(Collection<HeadSkin> mustAvoid, Collection<HeadSkin> preferToAvoid, RandomGenerator random) {
        List<HeadSkin> allowed = heads.stream().filter(head -> !isAmong(head, mustAvoid)).toList();
        List<HeadSkin> preferred = allowed.stream().filter(head -> !isAmong(head, preferToAvoid)).toList();
        List<HeadSkin> choice = !preferred.isEmpty() ? preferred : !allowed.isEmpty() ? allowed : heads;
        return choice.isEmpty() ? Optional.empty() : Optional.of(choice.get(random.nextInt(choice.size())));
    }

    private static boolean isAmong(HeadSkin head, Collection<HeadSkin> skins) {
        return skins.stream().anyMatch(skin -> skin.id().equals(head.id()));
    }

    public Block draw(Surface surface, RandomGenerator random) {
        return of(surface).draw(random);
    }
}

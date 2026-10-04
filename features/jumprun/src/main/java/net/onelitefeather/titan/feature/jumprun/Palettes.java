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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minestom.server.instance.block.Block;

/**
 * The materials a {@link Surface} can show, one {@link Palette} per shape. Every entry of a
 * palette collides up to the shape's height, so the material never changes what a jump demands.
 * Built from the config by {@link JumprunSettings}. The skins of the team heads, when there are
 * any, replace the materials of the head shape.
 */
final class Palettes {

    private final Map<Surface, Palette> byShape;
    private final List<HeadSkin> heads;

    /**
     * @throws IllegalArgumentException when a shape has no palette
     */
    Palettes(Map<Surface, Palette> byShape) {
        this(byShape, List.of());
    }

    private Palettes(Map<Surface, Palette> byShape, List<HeadSkin> heads) {
        for (Surface surface : Surface.values()) {
            if (!byShape.containsKey(surface)) {
                throw new IllegalArgumentException("no palette for " + surface);
            }
        }
        this.byShape = new EnumMap<>(byShape);
        this.heads = List.copyOf(heads);
    }

    /** The team heads that replace the plain heads; empty when there are none. */
    List<HeadSkin> heads() {
        return heads;
    }

    /** The same palettes with the given team heads. */
    Palettes withHeads(List<HeadSkin> heads) {
        return new Palettes(byShape, heads);
    }

    Palette of(Surface surface) {
        return byShape.get(surface);
    }

    /**
     * A skin of the team, never the {@code previous} one while another is there; empty when there
     * are no team heads and the plain heads of the palette show.
     */
    Optional<HeadSkin> drawHead(Optional<HeadSkin> previous, RandomGenerator random) {
        List<HeadSkin> others = heads.stream().filter(head -> !previous.map(HeadSkin::id).equals(Optional.of(head.id()))).toList();
        List<HeadSkin> choice = others.isEmpty() ? heads : others;
        return choice.isEmpty() ? Optional.empty() : Optional.of(choice.get(random.nextInt(choice.size())));
    }

    Block draw(Surface surface, RandomGenerator random) {
        return of(surface).draw(random);
    }
}

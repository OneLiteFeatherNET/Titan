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

import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

/**
 * How a course block is drawn as a display: a block display, or for a team head an item display,
 * because only an item can carry a profile. A block display sits with its corner at the entity, an
 * item display with its centre, so the item display is shifted to the middle of the cell.
 */
final class BlockLook {

    /**
     * An item model is drawn around the entity, as the block space [0,1] moved by -0.5. A head
     * fills only the lower half of that space (y 0 to 0.5, x and z 0.25 to 0.75), so moving the
     * display by half a cell on every axis puts the head on the floor of the cell. Taken from the
     * vanilla skull model and confirmed by the smoke test.
     */
    private static final Vec ITEM_CENTRE = new Vec(0.5, 0.5, 0.5);

    private static final Vec BLOCK_ORIGIN = Vec.ZERO;

    private BlockLook() {
    }

    static EntityType entityType(CourseBlock block) {
        return block.skin().isPresent() ? EntityType.ITEM_DISPLAY : EntityType.BLOCK_DISPLAY;
    }

    /** Sets what the display shows: the block state, or the head item with its profile. */
    static void show(AbstractDisplayMeta meta, CourseBlock block) {
        switch (meta) {
            case ItemDisplayMeta item ->
                item.setItemStack(ItemStack.builder(Material.PLAYER_HEAD).set(DataComponents.PROFILE, block.skin().orElseThrow().profile()).build());
            case BlockDisplayMeta display ->
                display.setBlockState(block.material());
            default ->
                throw new IllegalArgumentException("not a block or item display: " + meta.getClass());
        }
    }

    /** Where the display's own translation puts it so it covers the cell of the block. */
    static Vec origin(CourseBlock block) {
        return block.skin().isPresent() ? ITEM_CENTRE : BLOCK_ORIGIN;
    }

    /** The scale of an outline: a hair larger than what it wraps. */
    static Vec outlineScale(CourseBlock block) {
        double scale = block.skin().isPresent() ? 1.04 : 1.02;
        return new Vec(scale, scale, scale);
    }

    /**
     * The translation of an outline: a block display grows from its corner and is shifted back by
     * half the excess to wrap evenly; an item display grows from its centre and needs no shift.
     */
    static Vec outlineTranslation(CourseBlock block) {
        return block.skin().isPresent() ? ITEM_CENTRE : new Vec(-0.01, -0.01, -0.01);
    }
}

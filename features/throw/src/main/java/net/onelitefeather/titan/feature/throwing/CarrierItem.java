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
package net.onelitefeather.titan.feature.throwing;

import java.util.List;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.Consumable;
import net.minestom.server.component.DataComponents;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

/**
 * The charge item held during a carry: a stick that the client draws like a bow. Vanilla only, no
 * resource pack. Its consume time is effectively unbounded, so the item is never eaten; the
 * client's release is the throw.
 */
final class CarrierItem {

    static final Tag<Boolean> MARK = Tag.Boolean("titan:throw/carrier");

    /** Silent: the consume sound would otherwise play while the charge is held. */
    private static final SoundEvent SILENT = SoundEvent.of("minecraft:intentionally_empty", null);
    private static final Consumable CHARGE = new Consumable(3600f, ItemAnimation.BOW, SILENT, false, List.of());

    private CarrierItem() {
    }

    static ItemStack create() {
        return ItemStack.builder(Material.STICK).set(DataComponents.CONSUMABLE, CHARGE).build().withTag(MARK, true);
    }

    static boolean isCarrier(ItemStack stack) {
        return Boolean.TRUE.equals(stack.getTag(MARK));
    }
}

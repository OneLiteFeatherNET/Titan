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
package net.onelitefeather.titan.feature.dance;

import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogTags;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;

/**
 * Builds the {@code minecraft:quick_actions} tag of one player. A tag is replaced as a whole per
 * registry, so the packet names exactly the one list this player gets; the registry itself holds
 * every list. Public Minestom API only.
 */
final class QuickActionTags {

    private static final String DIALOG_REGISTRY = "minecraft:dialog";
    private static final String QUICK_ACTIONS = DialogTags.QUICK_ACTIONS.key().asString();

    private QuickActionTags() {
    }

    /**
     * @param dialogs   the dialog registry the list is registered in
     * @param dialogKey the key of the list, as {@link DanceList#key} names it
     * @return the packet that makes {@code dialogKey} the only quick action of the player
     * @throws IllegalArgumentException if no dialog is registered under {@code dialogKey}
     */
    static TagsPacket onlyQuickAction(DynamicRegistry<Dialog> dialogs, Key dialogKey) {
        RegistryKey<Dialog> key = dialogs.getKey(dialogKey);
        if (key == null) {
            throw new IllegalArgumentException("no dance list is registered under " + dialogKey);
        }
        int id = dialogs.getId(key);
        TagsPacket.Tag tag = new TagsPacket.Tag(QUICK_ACTIONS, new int[]{id});
        return new TagsPacket(List.of(new TagsPacket.Registry(DIALOG_REGISTRY, List.of(tag))));
    }
}

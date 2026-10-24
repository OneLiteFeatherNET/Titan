/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.tag;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/**
 * {@code /tag}: a shortcut for the tag setting in the settings menu. Commands are English on every
 * server; the replies follow the player's language.
 */
final class TagCommand extends Command {

    static final String NAME = "tag";

    TagCommand(TagOptIn optIn) {
        super(NAME);
        setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player) {
                player.sendMessage(TagMessages.text(keyOf(optIn.toggle(player.getUuid())), player.getLocale()));
            } else {
                sender.sendMessage(Component.text("Only players can play tag."));
            }
        });
    }

    private static String keyOf(TagOptIn.Toggle toggle) {
        return switch (toggle) {
            case SWITCHED_ON -> TagMessages.TOGGLED_ON;
            case SWITCHED_OFF -> TagMessages.TOGGLED_OFF;
            case UNAVAILABLE -> TagMessages.TOGGLE_UNAVAILABLE;
        };
    }
}

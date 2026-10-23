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

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/**
 * {@code /tanz} (alias {@code /dance}): the fallback for clients without quick actions. Opens the
 * same per-player choice as the quick action.
 */
final class DanceCommand extends Command {

    static final String NAME = "tanz";
    static final String ALIAS = "dance";

    DanceCommand(DanceModule module) {
        super(NAME, ALIAS);
        setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player) {
                module.openMenu(player);
            } else {
                sender.sendMessage(Component.text("Only players can dance."));
            }
        });
    }
}

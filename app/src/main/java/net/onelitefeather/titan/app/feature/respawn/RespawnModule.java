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
package net.onelitefeather.titan.app.feature.respawn;

import net.kyori.adventure.text.Component;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;

/**
 * Handles a lobby player's death and respawn.
 *
 * <p>On {@link PlayerDeathEvent}, this module blanks the death text and respawns the player right
 * away - there is no death screen in the lobby. On {@link PlayerRespawnEvent}, it hands the player
 * back the platform's standard loadout through {@link ModuleContext#items()}, the same call the
 * spawn module makes on join.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/specs/lobby-modules/spec.md}, scenario
 * "Tod ohne Nachricht", and {@code specs/lobby-hotbar/spec.md}, scenario "Ausstattung nach
 * Respawn". This module has no configuration of its own.
 */
public final class RespawnModule implements LobbyModule {

    @Override
    public String id() {
        return "respawn";
    }

    @Override
    public void enable(ModuleContext context) {
        context.listen(PlayerDeathEvent.class, RespawnModule::onDeath);
        context.listen(PlayerRespawnEvent.class, event -> context.items().equip(event.getPlayer()));
    }

    private static void onDeath(PlayerDeathEvent event) {
        event.setDeathText(Component.empty());
        event.getPlayer().respawn();
    }
}

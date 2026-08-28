/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.common.utils.ThreadHelper;
import net.theevilreaper.aves.util.Components;

import java.util.List;

record AppConfigImpl(long tickleDuration, Vec sitOffset, List<Key> allowedSitBlocks,
                     int simulationDistance,
                     int fireworkBoostSlot, double elytraBoostMultiplier,
                     int minHeightBeforeTeleport,
                     int maxHeightBeforeTeleport) implements AppConfig, ThreadHelper {

    @Override
    public Component displayConfig() {
        return MiniMessage.miniMessage().deserialize("""
                <prefix> App Config Display:
                    <dark_aqua>Tickle duration: <yellow><tickle_duration>
                    <dark_aqua>Sit offset: <yellow><sit_offset>
                    <dark_aqua>Allowed sit blocks: <yellow><allowed_sit_blocks>
                    <dark_aqua>Simulation distance: <yellow><simulation_distance>
                    <dark_aqua>Firework boost slot: <yellow><firework_boost_slot>
                    <dark_aqua>Elytra boost multiplier: <yellow><elytra_boost_multiplier>
                    <dark_aqua>Max height before teleport: <yellow><max_height_before_teleport>
                    <dark_aqua>Min height before teleport: <yellow><min_height_before_teleport>""", Placeholder.parsed("tickle_duration", String.valueOf(tickleDuration)), Placeholder.component("sit_offset", Components.convertPoint(sitOffset)), Placeholder.parsed("allowed_sit_blocks", allowedSitBlocks.toString()), Placeholder.parsed("simulation_distance", String.valueOf(simulationDistance)), Placeholder.parsed("firework_boost_slot", String.valueOf(fireworkBoostSlot)), Placeholder.parsed("elytra_boost_multiplier", String.valueOf(elytraBoostMultiplier)), Placeholder.parsed("max_height_before_teleport", String.valueOf(maxHeightBeforeTeleport)), Placeholder.parsed("min_height_before_teleport", String.valueOf(minHeightBeforeTeleport)));
    }
}

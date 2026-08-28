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
import net.minestom.server.coordinate.Vec;

import java.util.List;

final class AppConfigBuilder implements AppConfig.Builder {

    private long tickleDuration;
    private Vec sitOffset;
    private List<Key> allowedSitBlocks;
    private int simulationDistance;
    private int fireworkBoostSlot;
    private double elytraBoostMultiplier;
    private int maxHeightBeforeTeleport;
    private int minHeightBeforeTeleport;

    @Override
    public AppConfig.Builder tickleDuration(long tickleDuration) {
        this.tickleDuration = tickleDuration;
        return this;
    }

    @Override
    public AppConfig.Builder sitOffset(Vec sitOffset) {
        this.sitOffset = sitOffset;
        return this;
    }

    @Override
    public AppConfig.Builder allowedSitBlocks(List<Key> allowedSitBlocks) {
        this.allowedSitBlocks = allowedSitBlocks;
        return this;
    }

    @Override
    public AppConfig.Builder simulationDistance(int simulationDistance) {
        this.simulationDistance = simulationDistance;
        return this;
    }

    @Override
    public AppConfig.Builder fireworkBoostSlot(int fireworkBoostSlot) {
        this.fireworkBoostSlot = fireworkBoostSlot;
        return this;
    }

    @Override
    public AppConfig.Builder elytraBoostMultiplier(double elytraBoostMultiplier) {
        this.elytraBoostMultiplier = elytraBoostMultiplier;
        return this;
    }

    @Override
    public AppConfig.Builder addAllowedSitBlock(Key namespaceID) {
        this.allowedSitBlocks.add(namespaceID);
        return this;
    }

    @Override
    public AppConfig.Builder removeAllowedSitBlock(Key namespaceID) {
        this.allowedSitBlocks.remove(namespaceID);
        return this;
    }

    @Override
    public AppConfig.Builder minHeightBeforeTeleport(int minHeightBeforeTeleport) {
        this.minHeightBeforeTeleport = minHeightBeforeTeleport;
        return this;
    }

    @Override
    public AppConfig.Builder maxHeightBeforeTeleport(int maxHeightBeforeTeleport) {
        this.maxHeightBeforeTeleport = maxHeightBeforeTeleport;
        return this;
    }

    @Override
    public AppConfig build() {
        return new AppConfigImpl(tickleDuration, sitOffset, allowedSitBlocks, simulationDistance, fireworkBoostSlot, elytraBoostMultiplier, minHeightBeforeTeleport, maxHeightBeforeTeleport);
    }
}

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
        return new AppConfigImpl(tickleDuration, sitOffset, allowedSitBlocks, simulationDistance, fireworkBoostSlot, elytraBoostMultiplier,minHeightBeforeTeleport, maxHeightBeforeTeleport);
    }
}

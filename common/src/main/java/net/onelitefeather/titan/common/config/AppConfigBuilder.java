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

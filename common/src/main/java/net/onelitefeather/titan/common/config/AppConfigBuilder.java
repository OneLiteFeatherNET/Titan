package net.onelitefeather.titan.common.config;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.NamespaceID;

import java.util.List;

final class AppConfigBuilder implements AppConfig.Builder {

    private long tickleDuration;
    private Vec sitOffset;
    private List<NamespaceID> allowedSitBlocks;
    private int simulationDistance;
    private int fireworkBoostSlot;
    private double elytraBoostMultiplier;
    private long updateRateAgones;

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
    public AppConfig.Builder allowedSitBlocks(List<NamespaceID> allowedSitBlocks) {
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
    public AppConfig.Builder updateRateAgones(long updateRateAgones) {
        this.updateRateAgones = updateRateAgones;
        return this;
    }

    @Override
    public AppConfig build() {
        return new AppConfigImpl(tickleDuration, sitOffset, allowedSitBlocks, simulationDistance, fireworkBoostSlot, elytraBoostMultiplier, updateRateAgones);
    }
}

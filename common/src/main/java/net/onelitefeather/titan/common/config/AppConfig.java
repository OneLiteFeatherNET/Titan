package net.onelitefeather.titan.common.config;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.NamespaceID;

import java.util.List;

public sealed interface AppConfig permits AppConfigImpl, InternalAppConfig {
    String MAP_FILE_NAME = "map.json";

    /**
     * The delay between each tickle
     * @return The duration of the delay
     */
    long tickleDuration();

    /**
     * The offset for the sit position
     * @return The offset for the sit position
     */
    Vec sitOffset();

    /**
     * The list of allowed sit blocks
     * @return The list of allowed sit blocks
     */
    List<NamespaceID> allowedSitBlocks();

    /**
     * The distance for the simulation
     * @return The distance for the simulation
     */
    int simulationDistance();

    /**
     * The firework boost slot
     * @return The firework boost slot
     */
    int fireworkBoostSlot();

    /**
     * The multiplier for the elytra boost
     * @return The multiplier for the elytra boost
     */
    double elytraBoostMultiplier();

    /**
     * Create a new {@link AppConfig} builder
     * @return The builder
     */
    static Builder builder() {
        return new AppConfigBuilder();
    }

    /**
     * The builder for the {@link AppConfig} interface
     */
    static Builder builder(AppConfig appConfig) {
        return new AppConfigBuilder()
                .tickleDuration(appConfig.tickleDuration())
                .sitOffset(appConfig.sitOffset())
                .allowedSitBlocks(appConfig.allowedSitBlocks())
                .simulationDistance(appConfig.simulationDistance())
                .fireworkBoostSlot(appConfig.fireworkBoostSlot())
                .elytraBoostMultiplier(appConfig.elytraBoostMultiplier());
    }

    sealed interface Builder permits AppConfigBuilder {
        /**
         * Set the duration of the delay between each tickle
         * @param tickleDuration The duration of the delay
         * @return The builder
         */
        Builder tickleDuration(long tickleDuration);

        /**
         * Set the offset for the sit position
         * @param sitOffset The offset for the sit position
         * @return The builder
         */
        Builder sitOffset(Vec sitOffset);

        /**
         * Set the list of allowed sit blocks
         * @param allowedSitBlocks The list of allowed sit blocks
         * @return The builder
         */
        Builder allowedSitBlocks(List<NamespaceID> allowedSitBlocks);

        /**
         * Set the distance for the simulation
         * @param simulationDistance The distance for the simulation
         * @return The builder
         */
        Builder simulationDistance(int simulationDistance);

        /**
         * Set the firework boost slot
         * @param fireworkBoostSlot The firework boost slot
         * @return The builder
         */
        Builder fireworkBoostSlot(int fireworkBoostSlot);

        /**
         * Set the multiplier for the elytra boost
         * @param elytraBoostMultiplier The multiplier for the elytra boost
         * @return The builder
         */
        Builder elytraBoostMultiplier(double elytraBoostMultiplier);

        /**
         * Build the {@link AppConfig} instance
         * @return The {@link AppConfig} instance
         */
        AppConfig build();
    }
}

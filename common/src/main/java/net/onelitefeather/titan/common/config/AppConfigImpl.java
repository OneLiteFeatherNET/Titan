package net.onelitefeather.titan.common.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.common.utils.ThreadHelper;
import net.theevilreaper.aves.util.Components;

import java.util.List;

record AppConfigImpl(long tickleDuration,
                     Vec sitOffset,
                     List<Key> allowedSitBlocks,
                     int simulationDistance,
                     int fireworkBoostSlot,
                     double elytraBoostMultiplier,
                     int minHeightBeforeTeleport,
                     int maxHeightBeforeTeleport) implements AppConfig, ThreadHelper {

    @Override
    public Component displayConfig() {
        return MiniMessage.miniMessage()
                .deserialize("""
        <prefix> App Config Display:
            <dark_aqua>Tickle duration: <yellow><tickle_duration>
            <dark_aqua>Sit offset: <yellow><sit_offset>
            <dark_aqua>Allowed sit blocks: <yellow><allowed_sit_blocks>
            <dark_aqua>Simulation distance: <yellow><simulation_distance>
            <dark_aqua>Firework boost slot: <yellow><firework_boost_slot>
            <dark_aqua>Elytra boost multiplier: <yellow><elytra_boost_multiplier>
            <dark_aqua>Max height before teleport: <yellow><max_height_before_teleport>
            <dark_aqua>Min height before teleport: <yellow><min_height_before_teleport>""",
                        Placeholder.parsed("tickle_duration", String.valueOf(tickleDuration)),
                        Placeholder.component("sit_offset", Components.convertPoint(sitOffset)),
                        Placeholder.parsed("allowed_sit_blocks", allowedSitBlocks.toString()),
                        Placeholder.parsed("simulation_distance", String.valueOf(simulationDistance)),
                        Placeholder.parsed("firework_boost_slot", String.valueOf(fireworkBoostSlot)),
                        Placeholder.parsed("elytra_boost_multiplier", String.valueOf(elytraBoostMultiplier)),
                        Placeholder.parsed("max_height_before_teleport", String.valueOf(maxHeightBeforeTeleport)),
                        Placeholder.parsed("min_height_before_teleport", String.valueOf(minHeightBeforeTeleport))
                );
    }
}

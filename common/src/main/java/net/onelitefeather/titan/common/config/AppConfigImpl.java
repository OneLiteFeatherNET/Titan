package net.onelitefeather.titan.common.config;

import de.icevizion.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.NamespaceID;
import net.onelitefeather.titan.common.utils.ThreadHelper;

import java.util.List;

record AppConfigImpl(long tickleDuration,
                     Vec sitOffset,
                     List<NamespaceID> allowedSitBlocks,
                     int simulationDistance,
                     int fireworkBoostSlot,
                     double elytraBoostMultiplier,
                     long updateRateAgones) implements AppConfig, ThreadHelper {

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
            <dark_aqua>Update rate Agones: <yellow><update_rate_agones>""",
                        Placeholder.parsed("tickle_duration", String.valueOf(tickleDuration)),
                        Placeholder.component("sit_offset", Components.convertPoint(sitOffset)),
                        Placeholder.parsed("allowed_sit_blocks", allowedSitBlocks.toString()),
                        Placeholder.parsed("simulation_distance", String.valueOf(simulationDistance)),
                        Placeholder.parsed("firework_boost_slot", String.valueOf(fireworkBoostSlot)),
                        Placeholder.parsed("elytra_boost_multiplier", String.valueOf(elytraBoostMultiplier)),
                        Placeholder.parsed("update_rate_agones", String.valueOf(updateRateAgones))
                );
    }
}

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
package net.onelitefeather.titan.setup.commands;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.common.argument.ArgumentMaterialType;
import net.onelitefeather.titan.setup.config.ElytraSectionConfig;
import net.onelitefeather.titan.setup.config.SetupConfigEditor;
import net.onelitefeather.titan.setup.config.SitSectionConfig;
import net.onelitefeather.titan.setup.config.SpawnSectionConfig;
import net.onelitefeather.titan.setup.config.TickleSectionConfig;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Parses {@code /setup app ...} arguments and delegates every edit to a {@link
 * SetupConfigEditor}. This class only knows how to read command arguments and format feedback
 * messages; the actual field-by-field editing of {@code app.json} lives in {@link
 * SetupConfigEditor} so it can be unit tested without a running Minestom server.
 */
public class AppCommand extends Command {

    private static final Argument<Integer> ELYTRA_BURN_DURATION_TICKS = ArgumentType.Integer("elytraBurnDurationTicksValue").setDefaultValue(30);
    private static final Argument<Integer> ELYTRA_COOLDOWN_TICKS = ArgumentType.Integer("elytraCooldownTicksValue").setDefaultValue(40);
    private static final Argument<Integer> SIMULATION_DISTANCE = ArgumentType.Integer("simulationDistanceValue").setDefaultValue(2);
    private static final Argument<CommandContext> SIT_OFFSET = ArgumentType.Group("sitOffsetValue", ArgumentType.Double("x"), ArgumentType.Double("y"), ArgumentType.Double("z"));
    private static final Argument<Long> TICKLE_DURATION = ArgumentType.Long("tickleDurationValue").setDefaultValue(4000L);
    private final ArgumentMaterialType materialTypeAdd;
    private final ArgumentMaterialType materialTypeRemove;

    private final SetupConfigEditor configEditor;

    public AppCommand(SetupConfigEditor configEditor) {
        super("app");
        this.configEditor = configEditor;
        addSyntax(this::display, ArgumentType.Literal("display"));
        addSyntax(this::updateElytraBurnDurationTicks, ArgumentType.Literal("elytraBurnDurationTicks"), ELYTRA_BURN_DURATION_TICKS);
        addSyntax(this::updateElytraCooldownTicks, ArgumentType.Literal("elytraCooldownTicks"), ELYTRA_COOLDOWN_TICKS);
        addSyntax(this::updateSimulationDistance, ArgumentType.Literal("simulationDistance"), SIMULATION_DISTANCE);
        addSyntax(this::updateSitOffset, ArgumentType.Literal("sitOffset"), SIT_OFFSET);
        addSyntax(this::updateTickleDuration, ArgumentType.Literal("tickleDuration"), TICKLE_DURATION);
        materialTypeAdd = new ArgumentMaterialType("materialType");
        materialTypeAdd.setSuggestionCallback((sender, context, suggestion) -> {
            Material.values().stream().map(Material::key).filter(Predicate.not(this.configEditor.sit().allowedBlocks()::contains)).map(Key::asString).map(SuggestionEntry::new).forEach(suggestion::addEntry);
        });
        materialTypeRemove = new ArgumentMaterialType("materialType");
        materialTypeRemove.setSuggestionCallback((sender, context, suggestion) -> {
            Material.values().stream().map(Material::key).filter(this.configEditor.sit().allowedBlocks()::contains).map(Key::asString).map(SuggestionEntry::new).forEach(suggestion::addEntry);
        });
        addSyntax(this::addAllowedSitBlock, ArgumentType.Literal("allowedSitType"), ArgumentType.Literal("add"), materialTypeAdd);
        addSyntax(this::removeAllowedSitBlock, ArgumentType.Literal("allowedSitType"), ArgumentType.Literal("remove"), materialTypeRemove);
    }

    private void removeAllowedSitBlock(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Material material = Material.fromKey(commandContext.get(materialTypeRemove));
        this.configEditor.removeAllowedSitBlock(material.key());
        commandSender.sendMessage(
                MiniMessage.miniMessage().deserialize("<prefix> Allowed sit block <material> has been removed", Placeholder.parsed("material", material.key().asString())));
    }

    private void addAllowedSitBlock(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Material material = Material.fromKey(commandContext.get(materialTypeAdd));
        this.configEditor.addAllowedSitBlock(material.key());
        commandSender.sendMessage(
                MiniMessage.miniMessage().deserialize("<prefix> Allowed sit block <material> has been added", Placeholder.parsed("material", material.key().asString())));
    }

    private void updateTickleDuration(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Long tickleDuration = commandContext.get(TICKLE_DURATION);
        this.configEditor.setTickleCooldownMillis(tickleDuration);
        commandSender.sendMessage(
                MiniMessage.miniMessage().deserialize("<prefix> Tickle duration has been updated to <duration>", Placeholder.parsed("duration", String.valueOf(tickleDuration))));
    }

    private void updateSitOffset(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        CommandContext sitOffset = commandContext.get(SIT_OFFSET);
        Vec offset = new Vec(sitOffset.get("x"), sitOffset.get("y"), sitOffset.get("z"));
        this.configEditor.setSitOffset(offset);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Sit offset has been updated to <offset>", Placeholder.parsed("offset", offset.toString())));
    }

    private void updateSimulationDistance(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Integer simulationDistance = commandContext.get(SIMULATION_DISTANCE);
        this.configEditor.setSimulationDistance(simulationDistance);
        commandSender.sendMessage(
                MiniMessage.miniMessage().deserialize("<prefix> Simulation distance has been updated to <distance>", Placeholder.parsed("distance", String.valueOf(simulationDistance))));
    }

    private void updateElytraBurnDurationTicks(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Integer burnDurationTicks = commandContext.get(ELYTRA_BURN_DURATION_TICKS);
        this.configEditor.setElytraBurnDurationTicks(burnDurationTicks);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<prefix> Elytra burn duration has been updated to <ticks> ticks", Placeholder.parsed("ticks", String.valueOf(burnDurationTicks))));
    }

    private void updateElytraCooldownTicks(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Integer cooldownTicks = commandContext.get(ELYTRA_COOLDOWN_TICKS);
        this.configEditor.setElytraCooldownTicks(cooldownTicks);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<prefix> Elytra cooldown has been updated to <ticks> ticks", Placeholder.parsed("ticks", String.valueOf(cooldownTicks))));
    }

    private void display(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        SpawnSectionConfig spawn = this.configEditor.spawn();
        SitSectionConfig sit = this.configEditor.sit();
        TickleSectionConfig tickle = this.configEditor.tickle();
        ElytraSectionConfig elytra = this.configEditor.elytra();
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("""
                <prefix> App Config Display:
                    <dark_aqua>spawn.minHeight: <yellow><min_height>
                    <dark_aqua>spawn.maxHeight: <yellow><max_height>
                    <dark_aqua>spawn.simulationDistance: <yellow><simulation_distance>
                    <dark_aqua>sit.offset: <yellow><sit_offset>
                    <dark_aqua>sit.allowedBlocks: <yellow><allowed_sit_blocks>
                    <dark_aqua>tickle.cooldownMillis: <yellow><cooldown_millis>
                    <dark_aqua>elytra.burnDurationTicks: <yellow><elytra_burn_duration_ticks>
                    <dark_aqua>elytra.cooldownTicks: <yellow><elytra_cooldown_ticks>""", Placeholder.parsed("min_height", String.valueOf(spawn.minHeight())), Placeholder.parsed("max_height", String.valueOf(spawn.maxHeight())), Placeholder.parsed("simulation_distance", String.valueOf(spawn.simulationDistance())), Placeholder.parsed("sit_offset", sit.offset().toString()), Placeholder.parsed("allowed_sit_blocks", sit.allowedBlocks().toString()), Placeholder.parsed("cooldown_millis", String.valueOf(tickle.cooldownMillis())), Placeholder.parsed("elytra_burn_duration_ticks", String.valueOf(elytra.burnDurationTicks())), Placeholder.parsed("elytra_cooldown_ticks", String.valueOf(elytra.cooldownTicks()))));
    }
}

package net.onelitefeather.titan.setup.commands;

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
import net.minestom.server.utils.NamespaceID;
import net.onelitefeather.titan.common.commands.ArgumentMaterialType;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.config.AppConfigProvider;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class AppCommand extends Command {

    private static final Argument<Long> UPDATE_RATE_AGONES = ArgumentType.Long("updateRateAgonesValue").setDefaultValue(4000L);
    private static final Argument<Double> ELYTRA_BOOST_MULTIPLIER = ArgumentType.Double("elytraBoostMultiplierValue").setDefaultValue(35.0);
    private static final Argument<Integer> FIREWORK_BOOST_SLOT = ArgumentType.Integer("fireworkBoostSlotValue").setDefaultValue(45);
    private static final Argument<Integer> SIMULATION_DISTANCE = ArgumentType.Integer("simulationDistanceValue").setDefaultValue(2);
    private static final Argument<CommandContext> SIT_OFFSET = ArgumentType.Group("sitOffsetValue", ArgumentType.Double("x"), ArgumentType.Double("y"), ArgumentType.Double("z"));
    private static final Argument<Long> TICKLE_DURATION = ArgumentType.Long("tickleDurationValue").setDefaultValue(1000L);
    private final ArgumentMaterialType materialTypeAdd;
    private final ArgumentMaterialType materialTypeRemove;

    private final AppConfigProvider appConfigProvider;

    public AppCommand(AppConfigProvider appConfigProvider) {
        super("app");
        addSyntax(this::display, ArgumentType.Literal("display"));
        addSyntax(this::updateUpdateRate, ArgumentType.Literal("updateRateAgones"), UPDATE_RATE_AGONES);
        addSyntax(this::updateElytraMultiplier, ArgumentType.Literal("elytraBoostMultiplier"), ELYTRA_BOOST_MULTIPLIER);
        addSyntax(this::updateFireworkBoostSlot, ArgumentType.Literal("fireworkBoostSlot"), FIREWORK_BOOST_SLOT);
        addSyntax(this::updateSimulationDistance, ArgumentType.Literal("simulationDistance"), SIMULATION_DISTANCE);
        addSyntax(this::updateSitOffset, ArgumentType.Literal("sitOffset"), SIT_OFFSET);
        addSyntax(this::updateTickleDuration, ArgumentType.Literal("tickleDuration"), TICKLE_DURATION);
        materialTypeAdd = new ArgumentMaterialType("materialType");
        materialTypeAdd.setSuggestionCallback((sender, context, suggestion) -> {
            Material.values().stream().map(Material::namespace).filter(Predicate.not(appConfigProvider.getAppConfig().allowedSitBlocks()::contains)).map(NamespaceID::asString).map(SuggestionEntry::new).forEach(suggestion::addEntry);
        });
        materialTypeRemove = new ArgumentMaterialType("materialType");
        materialTypeRemove.setSuggestionCallback((sender, context, suggestion) -> {
            Material.values().stream().map(Material::namespace).filter(appConfigProvider.getAppConfig().allowedSitBlocks()::contains).map(NamespaceID::asString).map(SuggestionEntry::new).forEach(suggestion::addEntry);
        });
        addSyntax(this::addAllowedSitBlock, ArgumentType.Literal("allowedSitType"), ArgumentType.Literal("add"), materialTypeAdd);
        addSyntax(this::removeAllowedSitBlock, ArgumentType.Literal("allowedSitType"), ArgumentType.Literal("remove"), materialTypeRemove);
        this.appConfigProvider = appConfigProvider;
    }

    private void removeAllowedSitBlock(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Material material = Material.fromNamespaceId(commandContext.get(materialTypeRemove));
        AppConfig appConfig = AppConfig.builder(this.appConfigProvider.getAppConfig()).removeAllowedSitBlock(material).build();
        this.appConfigProvider.saveConfig(appConfig);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Allowed sit block <material> has been removed", Placeholder.parsed("material", material.namespace().asString())));
    }

    private void addAllowedSitBlock(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Material material = Material.fromNamespaceId(commandContext.get(materialTypeAdd));
        AppConfig appConfig = AppConfig.builder(this.appConfigProvider.getAppConfig()).addAllowedSitBlock(material).build();
        this.appConfigProvider.saveConfig(appConfig);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Allowed sit block <material> has been added", Placeholder.parsed("material", material.namespace().asString())));

    }

    private void updateTickleDuration(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Long tickleDuration = commandContext.get(TICKLE_DURATION);
        AppConfig appConfig = AppConfig.builder(this.appConfigProvider.getAppConfig()).tickleDuration(tickleDuration).build();
        this.appConfigProvider.saveConfig(appConfig);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Tickle duration has been updated to <duration>", Placeholder.parsed("duration", String.valueOf(tickleDuration))));
    }

    private void updateSitOffset(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        CommandContext sitOffset = commandContext.get(SIT_OFFSET);
        AppConfig appConfig = AppConfig.builder(this.appConfigProvider.getAppConfig()).sitOffset(new Vec(sitOffset.get("x"), sitOffset.get("y"), sitOffset.get("z"))).build();
        this.appConfigProvider.saveConfig(appConfig);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Sit offset has been updated to <offset>", Placeholder.parsed("offset", sitOffset.toString())));
    }

    private void updateSimulationDistance(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Integer simulationDistance = commandContext.get(SIMULATION_DISTANCE);
        AppConfig appConfig = AppConfig.builder(this.appConfigProvider.getAppConfig()).simulationDistance(simulationDistance).build();
        this.appConfigProvider.saveConfig(appConfig);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Simulation distance has been updated to <distance>", Placeholder.parsed("distance", String.valueOf(simulationDistance))));
    }

    private void updateFireworkBoostSlot(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Integer fireworkBoostSlot = commandContext.get(FIREWORK_BOOST_SLOT);
        AppConfig appConfig = AppConfig.builder(this.appConfigProvider.getAppConfig()).fireworkBoostSlot(fireworkBoostSlot).build();
        this.appConfigProvider.saveConfig(appConfig);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Firework boost slot has been updated to <slot>", Placeholder.parsed("slot", String.valueOf(fireworkBoostSlot))));
    }

    private void updateElytraMultiplier(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Double elytraBoostMultiplier = commandContext.get(ELYTRA_BOOST_MULTIPLIER);
        AppConfig appConfig = AppConfig.builder(this.appConfigProvider.getAppConfig()).elytraBoostMultiplier(elytraBoostMultiplier).build();
        this.appConfigProvider.saveConfig(appConfig);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Elytra boost multiplier has been updated to <multiplier>", Placeholder.parsed("multiplier", String.valueOf(elytraBoostMultiplier))));
    }

    private void updateUpdateRate(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        Long updateRate = commandContext.get(UPDATE_RATE_AGONES);
        AppConfig appConfig = AppConfig.builder(this.appConfigProvider.getAppConfig()).updateRateAgones(updateRate).build();
        this.appConfigProvider.saveConfig(appConfig);
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> Agones update rate has been updated to <rate>", Placeholder.parsed("rate", String.valueOf(updateRate))));
    }

    private void display(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        final AppConfig appConfig = this.appConfigProvider.getAppConfig();
        commandSender.sendMessage(appConfig.displayConfig());
    }
}

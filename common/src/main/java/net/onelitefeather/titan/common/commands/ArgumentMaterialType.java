package net.onelitefeather.titan.common.commands;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentRegistry.INVALID_NAME;

public final class ArgumentMaterialType extends ArgumentString {

    public ArgumentMaterialType(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull String parse(@NotNull CommandSender sender, @NotNull String input) throws ArgumentSyntaxException {
        final Material registryValue =  Material.values().stream()
                .filter(material -> material.namespace().asString().equalsIgnoreCase(input))
                .findAny()
                .orElseThrow(() -> new ArgumentSyntaxException("Registry value is invalid", input, INVALID_NAME));
        return registryValue.namespace().asString();
    }
}

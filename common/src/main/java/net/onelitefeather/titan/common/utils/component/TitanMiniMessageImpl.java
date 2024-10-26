package net.onelitefeather.titan.common.utils.component;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TitanMiniMessageImpl implements MiniMessage.Provider {
    @Override
    public @NotNull MiniMessage miniMessage() {
        return MiniMessage.builder().tags(TagResolver.resolver(TagResolver.standard(), Placeholder.component("prefix", TitanMiniMessageImpl::prefix))).build();
    }

    private static @NotNull Component prefix() {
        return MiniMessage.builder().tags(TagResolver.resolver(TagResolver.standard())).build().deserialize("<gradient:#00ddff:#ffffff>Titan</gradient>");
    }

    @Override
    public @NotNull Consumer<MiniMessage.Builder> builder() {
        return builder -> {};
    }
}

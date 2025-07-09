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

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
package net.onelitefeather.titan.apps.cloudnet.feature.example;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import net.kyori.adventure.key.Key;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;

/**
 * Template for a feature's own item factory: contributes the {@code example} template's one
 * hotbar item to the platform-wide {@code LobbyItems} as a {@code @Bean}.
 *
 * <p>Stays package-private, like a real feature's {@code @Factory} class; never built as a bean
 * since Avaje's processor skips test sources.
 */
@Factory
final class ExampleGreetingItems {

    private static final String FEATURE_ID = "example";
    private static final Key ITEM_KEY = Key.key("titan:example");
    private static final int HOTBAR_SLOT = ItemSlot.MAX_HOTBAR_SLOT;

    /**
     * Fixed to the last hotbar slot, one no real feature claims, so this never collides with a test
     * starting both.
     */
    @Bean
    LobbyItem greetingToken(ExampleModule module) {
        return new LobbyItem(FEATURE_ID, ITEM_KEY, ExampleItems.GREETING_TOKEN, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> module.greet(player));
    }
}

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
package net.onelitefeather.titan.app.feature.example;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import net.kyori.adventure.key.Key;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;

/**
 * Template for a feature's own item factory (see {@code NavigatorItems}/{@code ElytraLobbyItems}
 * for the real ones): contributes the {@code example} template's one hotbar item to the
 * platform-wide {@code LobbyItems} as a {@code @Bean}.
 *
 * <p>This is the shape a real feature's own {@code @Factory} class takes; it stays a plain,
 * package-private class here, like the rest of this template, because Avaje Inject's annotation
 * processor never runs for test sources (see {@link ExampleModule}'s class Javadoc) - this class is
 * never actually built as a bean, only read as a template.
 */
@Factory
final class ExampleGreetingItems {

    private static final String FEATURE_ID = "example";
    private static final Key ITEM_KEY = Key.key("titan:example");
    private static final int HOTBAR_SLOT = ItemSlot.MAX_HOTBAR_SLOT;

    /**
     * @param module the feature a use of the returned item greets through, via
     *               {@link ExampleModule#greet(net.minestom.server.entity.Player)}
     * @return the greeting token, fixed to hotbar slot {@value #HOTBAR_SLOT} - deliberately the
     *         last slot, one no real feature claims, so this template never collides with one in a
     *         test that happens to start both
     */
    @Bean
    LobbyItem greetingToken(ExampleModule module) {
        return new LobbyItem(FEATURE_ID, ITEM_KEY, ExampleItems.GREETING_TOKEN, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> module.greet(player));
    }
}

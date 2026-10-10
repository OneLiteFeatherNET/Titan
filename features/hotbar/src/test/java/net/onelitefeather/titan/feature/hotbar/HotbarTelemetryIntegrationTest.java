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
package net.onelitefeather.titan.feature.hotbar;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Spans and counter of {@link HotbarLobbyItems} on a real {@link Player} from a Cyano
 * {@code Env}: equipping, a use of a lobby item, and a click that is no lobby item.
 */
@ExtendWith(MicrotusExtension.class)
class HotbarTelemetryIntegrationTest {

    private static final String NAVIGATOR = "titan:navigator";
    private static final AttributeKey<String> ITEM_METRIC = AttributeKey.stringKey("item");

    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void close() {
        testTelemetry.close();
    }

    private static LobbyItem item(String key, Material material, ItemSlot placement) {
        return new LobbyItem("navigator", Key.key(key), ItemStack.of(material), placement, (player, event) -> {
        });
    }

    private HotbarLobbyItems lobbyItems(EventNode<Event> titan, List<LobbyItem> items) {
        return new HotbarLobbyItems(items, titan, testTelemetry.telemetry());
    }

    @DisplayName("Equipping a player opens one hotbar.equip span with the placed item count and the player's UUID")
    @Test
    void equipOpensAnEquipSpanWithTheCountAndPlayer(Env env) {
        EventNode<Event> titan = EventNode.all("hotbar-telemetry-equip");
        HotbarLobbyItems lobbyItems = lobbyItems(titan, List.of(item(NAVIGATOR, Material.FEATHER, ItemSlot.hotbar(4)), item("titan:elytra", Material.ELYTRA, ItemSlot.equipment(EquipmentSlot.CHESTPLATE)), item("titan:firework", Material.FIREWORK_ROCKET, ItemSlot.unplaced())));
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        lobbyItems.equip(player);

        SpanData span = testTelemetry.span(HotbarTelemetry.EQUIP_SPAN);
        Assertions.assertEquals(2L, testTelemetry.attribute(span, AttributeKey.longKey("hotbar.items")), "the hotbar and the equipment slot are placed, the unplaced item is not");
        Assertions.assertEquals(player.getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the span carries the player's UUID");
    }

    @DisplayName("Using a lobby item opens a hotbar.item.use span with its key and counts the use")
    @Test
    void usingALobbyItemOpensAUseSpanAndCountsIt(Env env) {
        EventNode<Event> titan = EventNode.all("hotbar-telemetry-use");
        HotbarLobbyItems lobbyItems = lobbyItems(titan, List.of(item(NAVIGATOR, Material.FEATHER, ItemSlot.hotbar(4))));
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        titan.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, lobbyItems.stack(NAVIGATOR), 0L));

        SpanData span = testTelemetry.span(HotbarTelemetry.USE_SPAN);
        Assertions.assertEquals(NAVIGATOR, testTelemetry.attribute(span, HotbarTelemetry.ITEM), "the span names the item");
        Assertions.assertEquals(player.getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the span carries the player's UUID");
        Assertions.assertEquals(1, testTelemetry.counter("titan.hotbar.item.uses", Attributes.of(ITEM_METRIC, NAVIGATOR)), "the use counter for the item is incremented");
    }

    @DisplayName("A click with something that is no lobby item opens no span and counts nothing")
    @Test
    void aClickWithoutALobbyItemOpensNoSpan(Env env) {
        EventNode<Event> titan = EventNode.all("hotbar-telemetry-plain");
        lobbyItems(titan, List.of(item(NAVIGATOR, Material.FEATHER, ItemSlot.hotbar(4))));
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        titan.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, ItemStack.of(Material.FEATHER), 0L));

        Assertions.assertTrue(testTelemetry.spans().stream().noneMatch(span -> span.getName().equals(HotbarTelemetry.USE_SPAN)), "a plain feather must not open a use span");
        Assertions.assertEquals(0, testTelemetry.counter("titan.hotbar.item.uses", Attributes.of(ITEM_METRIC, NAVIGATOR)), "a plain feather must not count as a use");
    }
}

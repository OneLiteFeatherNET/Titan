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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The clock in hotbar slot 8 exists whenever the flag is on. */
@ExtendWith(MicrotusExtension.class)
class LobbySwitcherItemsTest {

    @DisplayName("With the flag and an identity a joining player gets the clock in slot 8")
    @Test
    void flagAndIdentityGiveTheClockInSlotEight(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Instance instance = env.createFlatInstance();

            Player player = fixture.join(instance);

            ItemStack slot = player.getInventory().getItemStack(8);
            Assertions.assertEquals(Material.CLOCK, slot.material(), "slot 8 must hold the clock");
            Assertions.assertEquals(new ItemSlot.Hotbar(8), fixture.item().placement(), "the item must claim hotbar slot 8");
            Assertions.assertEquals("titan:lobbyswitcher", fixture.item().key().asString(), "the item key");
            Assertions.assertEquals("lobbyswitcher", fixture.item().featureId(), "the feature id");
        }
    }

    @DisplayName("Without the flag there is no item and the slot stays empty")
    @Test
    void withoutTheFlagThereIsNoItem(Env env) {
        try (SwitcherFixture fixture = new SwitcherFixture(env, new FakeFeatureFlags(), Optional.of(SwitcherFixture.OWN))) {
            Instance instance = env.createFlatInstance();

            Player player = fixture.join(instance);

            Assertions.assertNull(fixture.item(), "the flag is off, so no item bean");
            Assertions.assertEquals(ItemStack.AIR, player.getInventory().getItemStack(8), "slot 8 must stay empty");
        }
    }

    @DisplayName("An identity that appears after the beans are built still gives the clock, and the list opens")
    @Test
    void identityArrivingLaterStillWorks(Env env) {
        AtomicReference<Optional<LobbyIdentity>> identity = new AtomicReference<>(Optional.empty());
        try (SwitcherFixture fixture = new SwitcherFixture(env, new FakeFeatureFlags(LobbySwitcherModule.FLAG), identity::get, Telemetry.noop())) {
            identity.set(Optional.of(SwitcherFixture.OWN));
            Instance instance = env.createFlatInstance();

            Player player = fixture.join(instance);
            fixture.use(player);
            fixture.settle();

            Assertions.assertNotNull(fixture.item(), "the clock is contributed whenever the flag is on");
            Assertions.assertEquals(Material.CLOCK, player.getInventory().getItemStack(8).material(), "slot 8 must hold the clock");
            Assertions.assertNotNull(player.getOpenInventory(), "the list must open once the identity is known");
        }
    }

    @DisplayName("An identity that is known before the beans are built gives the clock, and the list opens")
    @Test
    void identityKnownBeforeTheScopeStillWorks(Env env) {
        AtomicReference<Optional<LobbyIdentity>> identity = new AtomicReference<>(Optional.of(SwitcherFixture.OWN));
        try (SwitcherFixture fixture = new SwitcherFixture(env, new FakeFeatureFlags(LobbySwitcherModule.FLAG), identity::get, Telemetry.noop())) {
            Instance instance = env.createFlatInstance();

            Player player = fixture.join(instance);
            fixture.use(player);
            fixture.settle();

            Assertions.assertNotNull(fixture.item(), "the clock is contributed whenever the flag is on");
            Assertions.assertEquals(Material.CLOCK, player.getInventory().getItemStack(8).material(), "slot 8 must hold the clock");
            Assertions.assertNotNull(player.getOpenInventory(), "the list must open when the identity was there from the start");
        }
    }

    @DisplayName("A clock built before the bundle is registered is still named once it is handed out")
    @Test
    void clockBuiltBeforeRegistrationIsNamedWhenGiven(Env env) {
        try (SwitcherFixture fixture = new SwitcherFixture(env, new FakeFeatureFlags(), Optional.of(SwitcherFixture.OWN))) {
            fixture.messages().close();
            LobbyItem item = new LobbySwitcherItems().lobbySwitcherItem(fixture.module(), new FakeFeatureFlags(LobbySwitcherModule.FLAG), fixture.messages()).orElseThrow();
            fixture.messages().register();
            Player player = fixture.joinWithLocale(env.createFlatInstance(), Locale.GERMAN, List.of(item));

            Component name = player.getInventory().getItemStack(8).get(DataComponents.CUSTOM_NAME);

            Assertions.assertEquals("Lobbys", plain(name), "the German clock must read Lobbys, not the raw key");
            Assertions.assertEquals(TextDecoration.State.FALSE, name.decoration(TextDecoration.ITALIC), "the clock name must not be italic");
        }
    }

    @DisplayName("A German player holds the clock named in German, an English player in English")
    @Test
    void clockNameFollowsThePlayersLocale(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player german = fixture.join(env.createFlatInstance(), Locale.GERMAN);
            Player english = fixture.join(env.createFlatInstance(), Locale.ENGLISH);

            Assertions.assertEquals("Lobbys", plain(german.getInventory().getItemStack(8).get(DataComponents.CUSTOM_NAME)), "German clock name");
            Assertions.assertEquals("Lobbies", plain(english.getInventory().getItemStack(8).get(DataComponents.CUSTOM_NAME)), "English clock name");
        }
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}

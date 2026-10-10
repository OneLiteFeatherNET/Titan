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
package net.onelitefeather.titan.feature.navigator;

import java.util.List;
import java.util.NoSuchElementException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage for {@link NavigatorModule} against a real {@code Env}: opening the feather
 * shows the four fixed destinations synchronously (no tick needed), clicking a destination forwards
 * through {@code Deliver} and closes the inventory, and clicking a blank glass pane does neither.
 *
 * <p>Every test here declares {@code NAVIGATOR_SLENDER} active on its {@link FakeFeatureFlags},
 * since these tests are not about the feature-flag gate itself (see
 * {@code NavigatorFeatureFlagTest}
 * for that) - they only need Slender to behave like any other, always-visible destination.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorModuleTest {

    private static FakeFeatureFlags slenderActive() {
        return new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true);
    }

    private static AbstractInventory openNavigator(NavigatorFixture fixture, Player player) {
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    @DisplayName("Opening the navigator via the feather shows the four fixed destinations, synchronously")
    @Test
    void openingTheNavigatorShowsTheFourDestinations(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);

            AbstractInventory openInventory = openNavigator(fixture, player);

            Assertions.assertNotNull(openInventory, "the navigator must open synchronously, without a tick");
            Assertions.assertInstanceOf(Inventory.class, openInventory);
            Assertions.assertEquals(InventoryType.CHEST_1_ROW, ((Inventory) openInventory).getInventoryType());
            Assertions.assertEquals(Material.ELYTRA, openInventory.getItemStack(0).material());
            Assertions.assertEquals(Material.COMPASS, openInventory.getItemStack(2).material());
            Assertions.assertEquals(Material.GRASS_BLOCK, openInventory.getItemStack(4).material());
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, openInventory.getItemStack(5).material());
            Assertions.assertEquals(Material.WOODEN_AXE, openInventory.getItemStack(8).material());
            for (int slot : List.of(1, 3, 6, 7)) {
                Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, openInventory.getItemStack(slot).material(), "slot " + slot + " should be a blank glass pane");
            }
        }
    }

    @DisplayName("Clicking ElytraRace forwards to the ElytraRace task and closes the inventory")
    @Test
    void clickingElytraRaceForwardsToElytraRace(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            AbstractInventory openInventory = openNavigator(fixture, player);

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(0));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled(), "the click must be cancelled so the icon stays in place");
            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery must be recorded");
            Assertions.assertEquals("ElytraRace", deliver.deliveries().get(0).taskName());
            Assertions.assertNotSame(openInventory, player.getOpenInventory(), "the navigator must close after a click");
        }
    }

    @DisplayName("Clicking Survival forwards to the Survival task and closes the inventory")
    @Test
    void clickingSurvivalForwardsToSurvival(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            AbstractInventory openInventory = openNavigator(fixture, player);

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(4));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled());
            Assertions.assertEquals(1, deliver.deliveries().size());
            Assertions.assertEquals("Survival", deliver.deliveries().get(0).taskName());
            Assertions.assertNotSame(openInventory, player.getOpenInventory());
        }
    }

    @DisplayName("Clicking Slender forwards to the cygnus task and closes the inventory")
    @Test
    void clickingSlenderForwardsToCygnus(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            AbstractInventory openInventory = openNavigator(fixture, player);

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(5));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled());
            Assertions.assertEquals(1, deliver.deliveries().size());
            Assertions.assertEquals("cygnus", deliver.deliveries().get(0).taskName());
            Assertions.assertNotSame(openInventory, player.getOpenInventory());
        }
    }

    @DisplayName("Clicking Creative forwards to the MemberBuild task and closes the inventory")
    @Test
    void clickingCreativeForwardsToMemberBuild(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            AbstractInventory openInventory = openNavigator(fixture, player);

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(8));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled());
            Assertions.assertEquals(1, deliver.deliveries().size());
            Assertions.assertEquals("MemberBuild", deliver.deliveries().get(0).taskName());
            Assertions.assertNotSame(openInventory, player.getOpenInventory());
        }
    }

    @DisplayName("Clicking a blank glass pane triggers no delivery and leaves the navigator open")
    @Test
    void clickingABlankSlotTriggersNoDeliveryAndKeepsTheNavigatorOpen(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            AbstractInventory openInventory = openNavigator(fixture, player);

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(1));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "clicking a blank glass pane must not trigger a delivery");
            Assertions.assertSame(openInventory, player.getOpenInventory(), "the navigator must stay open after clicking a blank slot");
        }
    }

    @DisplayName("Clicking Spawn returns the player once, closes the navigator, delivers nowhere and keeps the icon")
    @Test
    void clickingSpawnReturnsThePlayerWithoutADelivery(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakeSpawnReturn spawnReturn = new FakeSpawnReturn();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService(), spawnReturn)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            AbstractInventory openInventory = openNavigator(fixture, player);

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(2));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled(), "the click must be cancelled so the icon stays in the navigator");
            Assertions.assertEquals(1, spawnReturn.tellCalls(), "the player must be sent to spawn exactly once");
            Assertions.assertEquals(0, spawnReturn.sendCalls(), "the click must use sendToSpawnAndTell, not sendToSpawn directly");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "Spawn must not trigger a Deliver redirect");
            Assertions.assertNotSame(openInventory, player.getOpenInventory(), "the navigator must close after the click");
            Assertions.assertEquals(Material.COMPASS, openInventory.getItemStack(2).material(), "the icon must stay in the navigator");
            Assertions.assertEquals(Component.text("Spawn", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false), openInventory.getItemStack(2).get(DataComponents.CUSTOM_NAME), "the compass must be named Spawn, language-neutral");
        }
    }

    @DisplayName("Starting fails clearly when the SpawnReturn provider throws")
    @Test
    void startFailsClearlyWhenTheProviderThrows(Env env) {
        TestTitanNode titan = TestTitanNode.attach(env);
        try {
            NavigatorModule module = new NavigatorModule(titan.node(), new RecordingDeliver(), slenderActive(), new FakePermissionService(), () -> {
                throw new NoSuchElementException("no bean");
            }, Telemetry.noop());

            IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, module::start, "an unresolvable provider must fail the start");
            Assertions.assertTrue(thrown.getMessage().contains("SpawnReturn") && thrown.getMessage().contains("spawn column"), "the message must name SpawnReturn and the spawn column, was: " + thrown.getMessage());
        } finally {
            titan.close();
        }
    }

    @DisplayName("Starting fails clearly when the SpawnReturn provider yields null")
    @Test
    void startFailsClearlyWhenTheProviderYieldsNull(Env env) {
        TestTitanNode titan = TestTitanNode.attach(env);
        try {
            NavigatorModule module = new NavigatorModule(titan.node(), new RecordingDeliver(), slenderActive(), new FakePermissionService(), () -> null, Telemetry.noop());

            IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, module::start, "a provider yielding null must fail the start");
            Assertions.assertTrue(thrown.getMessage().contains("SpawnReturn") && thrown.getMessage().contains("spawn column"), "the message must name SpawnReturn and the spawn column, was: " + thrown.getMessage());
        } finally {
            titan.close();
        }
    }

    @DisplayName("Once the module is stopped, a navigator click no longer forwards or cancels")
    @Test
    void clicksNoLongerForwardOnceTheModuleIsStopped(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            AbstractInventory openInventory = openNavigator(fixture, player);

            fixture.stopModule();

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(4));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertFalse(clickEvent.isCancelled(), "no feature code may run once the module has stopped");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "no delivery may happen once the module has stopped");
        }
    }
}

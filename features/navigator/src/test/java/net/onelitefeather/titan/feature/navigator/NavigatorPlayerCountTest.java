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

import static net.onelitefeather.titan.feature.navigator.NavigatorFixture.tick;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.ServerFlag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Time passes only through {@code env.tick()}; the refresh period is one second of ticks. */
@ExtendWith(MicrotusExtension.class)
class NavigatorPlayerCountTest {

    private static final int PERIOD_TICKS = NavigatorFixture.REFRESH_SECONDS * ServerFlag.SERVER_TICKS_PER_SECOND;
    // Survival has no portal in these tests, so only its left click is named.
    private static final String HINT = "Left click: connect";
    private static final String GERMAN_HINT = "Linksklick: verbinden";

    private static List<String> lore(ItemStack item) {
        return item.get(DataComponents.LORE).stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    private static ItemStack survival(Inventory inventory) {
        return inventory.getItemStack(Destination.SURVIVAL.slot());
    }

    @DisplayName("The first refresh shows the count under the mode, in both menus")
    @Test
    void firstRefreshShowsTheCount(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 4);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone("titan.navigator.slender"), counts)) {
            tick(env, 2);

            Assertions.assertEquals(List.of("4 online", HINT), lore(survival(fixture.module().publicInventory())));
            Assertions.assertEquals(List.of("4 online", HINT), lore(survival(fixture.module().teamInventory())));
        }
    }

    @DisplayName("The count changes after the next refresh period, not before")
    @Test
    void countUpdatesAfterThePeriod(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 4);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone("titan.navigator.slender"), counts)) {
            tick(env, 2);
            counts.serve("Survival", 9);
            tick(env, 5);
            Inventory inventory = fixture.module().publicInventory();
            Assertions.assertEquals(List.of("4 online", HINT), lore(survival(inventory)), "no refresh is due yet");

            tick(env, PERIOD_TICKS);

            Assertions.assertEquals(List.of("9 online", HINT), lore(survival(inventory)));
        }
    }

    @DisplayName("A mode whose task is not running shows only its click hint")
    @Test
    void notRunningTaskShowsOnlyTheClickHint(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone("titan.navigator.slender"), new FakePlayerCounts())) {
            tick(env, 2);

            Assertions.assertEquals(List.of(HINT), lore(survival(fixture.module().publicInventory())), "a task that runs nowhere must not show a count line");
        }
    }

    @DisplayName("German inventories show the count after a refresh, in both menus")
    @Test
    void germanInventoriesShowTheCount(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 4);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone("titan.navigator.slender"), counts)) {
            tick(env, 2);

            Assertions.assertEquals(List.of("4 online", GERMAN_HINT), lore(survival(fixture.module().publicInventory(Locale.GERMAN))), "the German public menu must get the count refresh");
            Assertions.assertEquals(List.of("4 online", GERMAN_HINT), lore(survival(fixture.module().teamInventory(Locale.GERMAN))), "the German team menu must get the count refresh");
        }
    }

    @DisplayName("Once the module is stopped, no refresh changes the inventory any more")
    @Test
    void stoppedModuleDoesNotRefresh(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 4);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone("titan.navigator.slender"), counts)) {
            tick(env, 2);
            fixture.stopModule();
            counts.serve("Survival", 9);

            tick(env, PERIOD_TICKS * 2);

            Assertions.assertEquals(List.of("4 online", HINT), lore(survival(fixture.module().publicInventory())));
        }
    }

    @DisplayName("A click still forwards to the task after a refresh updated the stacks")
    @Test
    void clickStillForwardsAfterRefresh(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 4);
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts)) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);
            tick(env, 2);
            fixture.useFeather(player);

            env.process().eventHandler().call(new InventoryPreClickEvent(player.getOpenInventory(), player, new Click.Left(Destination.SURVIVAL.slot())));

            Assertions.assertEquals("Survival", deliver.deliveries().get(0).taskName());
        }
    }
}

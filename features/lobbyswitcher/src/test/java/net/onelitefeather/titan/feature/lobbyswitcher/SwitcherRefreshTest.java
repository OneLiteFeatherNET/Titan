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

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The list refreshes every refreshSeconds while someone looks at it, and not otherwise. */
@ExtendWith(MicrotusExtension.class)
class SwitcherRefreshTest {

    private static final int PERIOD_TICKS = SwitcherFixture.REFRESH_SECONDS * ViewerCounter.TICKS_PER_SECOND;

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }

    @DisplayName("An open list is refreshed with the new numbers after the period")
    @Test
    void openListIsRefreshed(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.joinAndOpen();
            fixture.counts().serve(new ServiceCount("Lobby-1", 49, 50), SwitcherFixture.LOBBY_2, SwitcherFixture.LOBBY_3);

            tick(env, PERIOD_TICKS + 10);

            String lore = PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().getItemStack(0).get(DataComponents.LORE).getFirst());
            Assertions.assertEquals(2, fixture.counts().reads(), "the open read plus one periodic read");
            Assertions.assertTrue(lore.contains("49"), "the view must show the new count: " + lore);
        }
    }

    @DisplayName("The list is not read before the period has passed")
    @Test
    void noReadBeforeThePeriod(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            fixture.joinAndOpen();

            tick(env, PERIOD_TICKS - 10);

            Assertions.assertEquals(1, fixture.counts().reads(), "only the read at opening");
        }
    }

    @DisplayName("After the last viewer closed the list nothing is read any more")
    @Test
    void noReadsWithoutViewers(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.joinAndOpen();
            player.closeInventory();

            tick(env, PERIOD_TICKS * 3);

            Assertions.assertEquals(1, fixture.counts().reads(), "no periodic read without a viewer");
        }
    }

    @DisplayName("The period keeps running while one of two viewers has the list open")
    @Test
    void periodSurvivesWhileAViewerRemains(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player first = fixture.joinAndOpen();
            fixture.joinAndOpen();
            first.closeInventory();
            int before = fixture.counts().reads();

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertEquals(before + 1, fixture.counts().reads(), "the remaining viewer still gets refreshes");
        }
    }

    @DisplayName("Opening again after everyone left reads immediately and starts the period anew")
    @Test
    void reopeningRestartsTheReads(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            fixture.joinAndOpen().closeInventory();

            fixture.joinAndOpen();
            tick(env, PERIOD_TICKS + 10);

            Assertions.assertEquals(3, fixture.counts().reads(), "first open, second open, one periodic read");
        }
    }

    @DisplayName("After the inventory is stopped no period is running any more")
    @Test
    void noReadsAfterStop(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            fixture.joinAndOpen();
            fixture.inventory().stop();
            int before = fixture.counts().reads();

            tick(env, PERIOD_TICKS * 3);

            Assertions.assertEquals(before, fixture.counts().reads(), "stop() must cancel the period");
        }
    }

    @DisplayName("A failing read keeps the last list on screen")
    @Test
    void failingRefreshKeepsTheLastList(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.joinAndOpen();
            fixture.counts().failWith(new IllegalStateException("provider down"));

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertEquals(Material.LIME_CONCRETE, player.getOpenInventory().getItemStack(0).material(), "Lobby-1 is still listed as joinable");
        }
    }
}

/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.navigator;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MicrotusExtension.class)
class GuardedDeliverTest {

    private static final int ROW = 9;

    private final BuildServerAccess access = new BuildServerAccess("Build", BuildServerAccess.PERMISSION);
    private final TestAudience audience = new TestAudience();
    private final RecordingDeliver delegate = new RecordingDeliver();

    @DisplayName("A team member holding the permission reaches the build server")
    @Test
    void testSwitchAllowedWithPermission(Env env) {
        Player player = player(env);
        this.audience.grant(player.getUuid(), BuildServerAccess.PERMISSION);
        Deliver deliver = GuardedDeliver.wrap(this.delegate, this.audience, this.access);

        deliver.sendPlayer(player, server(player, "Build-1"));

        Assertions.assertEquals(1, this.delegate.delivered.size(), "The switch should have been performed");
    }

    @DisplayName("A permission withdrawn between opening the menu and clicking refuses the switch")
    @Test
    void testSwitchRefusedAfterPermissionRevoked(Env env) {
        Player player = player(env);
        this.audience.grant(player.getUuid(), BuildServerAccess.PERMISSION);
        Deliver deliver = GuardedDeliver.wrap(this.delegate, this.audience, this.access);

        // The menu is drawn while the player still holds the permission, so the entry is there.
        List<NavigatorEntry> entries = List.of(NavigatorEntry.restrictedServer(icon(), "Build-1", BuildServerAccess.PERMISSION));
        List<NavigatorLayout.Placement> menu = NavigatorLayout.plan(entries, player.getUuid(), this.audience, ROW);
        Assertions.assertEquals(1, menu.size(), "The entry should be offered while the permission is held");

        // The permission is taken away, then the click arrives. The drawn menu proves nothing.
        this.audience.revoke(player.getUuid(), BuildServerAccess.PERMISSION);
        deliver.sendPlayer(player, menu.getFirst().entry().toComponent(player.getUuid()));

        Assertions.assertTrue(this.delegate.delivered.isEmpty(), "A revoked permission must refuse the switch (US-5.03)");
    }

    @DisplayName("A tampered click naming a build server the menu never offered is refused")
    @Test
    void testTamperedRequestRefused(Env env) {
        Player player = player(env);
        Deliver deliver = GuardedDeliver.wrap(this.delegate, this.audience, this.access);

        deliver.sendPlayer(player, server(player, "Build-1"));
        deliver.sendPlayer(player, server(player, "Build-99"));
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("Build").build());

        Assertions.assertTrue(this.delegate.delivered.isEmpty(), "No build server may be reached without the permission");
    }

    @DisplayName("Public destinations pass through untouched")
    @Test
    void testUnguardedDestinationPassesThrough(Env env) {
        Player player = player(env);
        Deliver deliver = GuardedDeliver.wrap(this.delegate, this.audience, this.access);

        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("Survival").build());

        Assertions.assertEquals(1, this.delegate.delivered.size(), "The guard covers the build servers only");
    }

    private Player player(Env env) {
        Instance instance = env.createFlatInstance();
        return env.createPlayer(instance);
    }

    private DeliverComponent server(Player player, String serviceName) {
        return DeliverComponent.serverBuilder().player(player).serverName(serviceName).build();
    }

    private static ItemStack icon() {
        return ItemStack.builder(Material.SCAFFOLDING).customName(Component.text("Build-1")).build();
    }

    /** Stands in for the real delivery and only records what it was asked to do. */
    private static final class RecordingDeliver implements Deliver {

        private final List<DeliverComponent> delivered = new ArrayList<>();

        @Override
        public void sendPlayer(Player player, DeliverComponent component) {
            this.delivered.add(component);
        }
    }
}

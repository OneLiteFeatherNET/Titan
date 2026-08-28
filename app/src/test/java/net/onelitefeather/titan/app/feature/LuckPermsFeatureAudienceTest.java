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
package net.onelitefeather.titan.app.feature;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.onelitefeather.titan.common.feature.FeatureAudience;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the one thing the gate and the {@code /season} command must agree on: a permission is
 * answered from the player's <em>current context</em>, not from the holder's stored query options.
 */
class LuckPermsFeatureAudienceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    /** The options LuckPerms resolves for an online player - a server-scoped context. */
    private static final QueryOptions CONTEXTUAL = mock(QueryOptions.class);

    /** The holder's stored options, which carry no server, world or dimension context. */
    private static final QueryOptions STORED = mock(QueryOptions.class);

    @Test
    @DisplayName("a permission is answered by the online player's own permission checker")
    void permissionComesFromTheOnlinePlayersChecker() {
        // The command condition consults exactly this checker through PermissionChecker.POINTER.
        // If the audience reached past it into LuckPerms' stored data, the supplier below would
        // be used and the answer would flip.
        FeatureAudience audience = LuckPermsFeatureAudience.of(failingLuckPerms(), checker(ReleaseStage.INTERNAL_PERMISSION));

        assertTrue(audience.hasPermission(PLAYER, ReleaseStage.INTERNAL_PERMISSION));
        assertFalse(audience.hasPermission(PLAYER, "titan.feature.something.else"));
    }

    @Test
    @DisplayName("an offline player is read with contextual options, never with the stored ones")
    void offlinePermissionUsesResolvedQueryOptions() {
        LuckPerms luckPerms = luckPerms(user -> {
            CachedPermissionData contextual = mock(CachedPermissionData.class);
            when(contextual.checkPermission(ReleaseStage.INTERNAL_PERMISSION)).thenReturn(Tristate.TRUE);
            CachedPermissionData stored = mock(CachedPermissionData.class);
            when(stored.checkPermission(ReleaseStage.INTERNAL_PERMISSION)).thenReturn(Tristate.UNDEFINED);
            CachedDataManager data = mock(CachedDataManager.class);
            when(data.getPermissionData(CONTEXTUAL)).thenReturn(contextual);
            when(data.getPermissionData(STORED)).thenReturn(stored);
            when(user.getCachedData()).thenReturn(data);
        });
        FeatureAudience audience = LuckPermsFeatureAudience.of(() -> luckPerms, nobodyOnline());

        assertTrue(audience.hasPermission(PLAYER, ReleaseStage.INTERNAL_PERMISSION), "The permission was read from the holder's stored query options. Those carry no server " + "context, so a grant scoped to server=titan-lobby-1 disappears and the gate denies a " + "team member the command lets through.");
    }

    @Test
    @DisplayName("group membership is resolved with contextual options, never with the stored ones")
    void groupMembershipUsesResolvedQueryOptions() {
        Group lite = mock(Group.class);
        when(lite.getName()).thenReturn(ReleaseStage.LITE_GROUP);
        LuckPerms luckPerms = luckPerms(user -> {
            when(user.getInheritedGroups(CONTEXTUAL)).thenReturn(List.of(lite));
            when(user.getInheritedGroups(STORED)).thenReturn(List.of());
        });
        FeatureAudience audience = LuckPermsFeatureAudience.of(() -> luckPerms, nobodyOnline());

        assertTrue(audience.inGroup(PLAYER, ReleaseStage.LITE_GROUP), "Group membership was resolved against the holder's stored query options instead of " + "the context LuckPerms resolves for the player.");
        assertFalse(audience.inGroup(PLAYER, "some-other-group"));
    }

    @Test
    @DisplayName("an unknown player holds nothing")
    void unknownPlayerHoldsNothing() {
        LuckPerms luckPerms = luckPerms(null);
        FeatureAudience audience = LuckPermsFeatureAudience.of(() -> luckPerms, nobodyOnline());

        assertFalse(audience.hasPermission(PLAYER, ReleaseStage.INTERNAL_PERMISSION));
        assertFalse(audience.inGroup(PLAYER, ReleaseStage.LITE_GROUP));
    }

    @Test
    @DisplayName("an unavailable permission backend denies instead of throwing")
    void unavailableBackendFailsClosed() {
        // LuckPermsProvider.get() throws NotLoadedException (an IllegalStateException) while
        // LuckPerms is still loading or after it failed to load. The gate is asked on every
        // navigator open, so this must not escape into a listener - and it must deny, not admit.
        FeatureAudience audience = LuckPermsFeatureAudience.of(failingLuckPerms(), nobodyOnline());

        assertFalse(audience.hasPermission(PLAYER, ReleaseStage.INTERNAL_PERMISSION));
        assertFalse(audience.inGroup(PLAYER, ReleaseStage.LITE_GROUP));
        assertFalse(ReleaseStage.INTERNAL.admits(PLAYER, audience));
        assertFalse(ReleaseStage.LITE.admits(PLAYER, audience));
        // Fail-closed hides work in progress, never the lobby: ga never asks the audience.
        assertTrue(ReleaseStage.GA.admits(PLAYER, audience));
    }

    @Test
    @DisplayName("a checker that throws is treated as an outage, not as a grant")
    void throwingCheckerFailsClosed() {
        FeatureAudience audience = LuckPermsFeatureAudience.of(failingLuckPerms(), playerId -> permission -> {
            throw new IllegalStateException("LuckPerms is not loaded");
        });

        assertFalse(audience.hasPermission(PLAYER, ReleaseStage.INTERNAL_PERMISSION));
    }

    /**
     * Builds a LuckPerms whose context manager resolves {@link #CONTEXTUAL} for the player and
     * whose static options are {@link #STORED}, so a caller that skips the context resolution ends
     * up on the stored options and the assertion above catches it.
     */
    private static LuckPerms luckPerms(java.util.function.Consumer<User> stubUser) {
        LuckPerms luckPerms = mock(LuckPerms.class);
        UserManager users = mock(UserManager.class);
        ContextManager contexts = mock(ContextManager.class);
        when(luckPerms.getUserManager()).thenReturn(users);
        when(luckPerms.getContextManager()).thenReturn(contexts);
        when(contexts.getStaticQueryOptions()).thenReturn(STORED);
        if (stubUser == null) {
            when(users.getUser(PLAYER)).thenReturn(null);
            return luckPerms;
        }
        User user = mock(User.class);
        when(user.getQueryOptions()).thenReturn(STORED);
        when(users.getUser(PLAYER)).thenReturn(user);
        when(contexts.getQueryOptions(user)).thenReturn(Optional.of(CONTEXTUAL));
        stubUser.accept(user);
        return luckPerms;
    }

    private static Supplier<LuckPerms> failingLuckPerms() {
        return () -> {
            throw new IllegalStateException("LuckPerms is not loaded");
        };
    }

    private static Function<UUID, PermissionChecker> nobodyOnline() {
        return playerId -> null;
    }

    private static Function<UUID, PermissionChecker> checker(String granted) {
        PermissionChecker checker = permission -> granted.equals(permission) ? TriState.TRUE : TriState.FALSE;
        return playerId -> PLAYER.equals(playerId) ? checker : null;
    }
}

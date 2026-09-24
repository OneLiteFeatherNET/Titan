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
package net.onelitefeather.titan.app.navigator;

import com.github.benmanes.caffeine.cache.LoadingCache;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import net.onelitefeather.titan.app.testutils.DummyDeliver;
import net.onelitefeather.titan.app.testutils.EventListenerCounter;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves the navigator listener leak described in
 * {@code openspec/changes/lobby-feature-modules/design.md} (Context, "Navigator-Leck"):
 * {@link NavigationHelper} keeps a Caffeine {@link LoadingCache} of one Aves
 * {@link PersonalInventoryBuilder} per player. Every time the cache reloads a player's entry
 * (on {@code refreshAfterWrite(1 min)}, or after eviction and a re-open), a brand-new
 * {@link PersonalInventoryBuilder} is built and {@code register()}ed, but the replaced builder
 * is never {@code unregister()}ed. Its dedicated Aves click listener - and the {@link Player}
 * reference it captured - therefore stay registered on the old inventory's {@link EventNode}
 * forever.
 *
 * <p>Listener counting has no public Minestom API (see design.md, Open Questions), so this uses
 * {@link EventListenerCounter}, which reflects into Minestom's internal
 * {@code EventNodeImpl#listenerMap}.
 *
 * <p>Disabled until the navigator is rebuilt as a module with a single shared inventory and a
 * single click listener (task 6.4), which removes {@link NavigationHelper} and its Caffeine
 * cache entirely.
 */
@ExtendWith(MicrotusExtension.class)
@Disabled("Navigator listener leak - fixed by task 6.4 of lobby-feature-modules")
class NavigatorListenerLeakTest {

    // design.md suggests 100 players; a smaller number already proves the leak and keeps the
    // suite fast, since Env#createPlayer is not free.
    private static final int PLAYER_COUNT = 25;

    @Test
    @DisplayName("Refreshing the navigator cache must not leave stale click listeners behind")
    @SuppressWarnings("unchecked")
    void refreshingTheNavigatorCacheDoesNotLeakListeners(Env env) throws ReflectiveOperationException {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance());
        Instance flatInstance = env.createFlatInstance();

        Field cacheField = NavigationHelper.class.getDeclaredField("inventoryBuilderLoadingCache");
        cacheField.setAccessible(true);
        LoadingCache<UUID, PersonalInventoryBuilder> cache = (LoadingCache<UUID, PersonalInventoryBuilder>) cacheField.get(helper);

        List<Player> players = new ArrayList<>(PLAYER_COUNT);
        List<EventNode<InventoryEvent>> nodesBeforeRefresh = new ArrayList<>(PLAYER_COUNT);
        for (int i = 0; i < PLAYER_COUNT; i++) {
            Player player = env.createPlayer(flatInstance);
            players.add(player);

            // Simulates "a player opens the navigator": loads (and registers) one
            // PersonalInventoryBuilder per player, exactly like production code.
            helper.openNavigator(player);

            PersonalInventoryBuilder builder = cache.asMap().get(player.getUuid());
            nodesBeforeRefresh.add(builder.getInventory().eventNode());
        }

        int listenersBeforeRefresh = countAll(nodesBeforeRefresh);
        assertEquals(PLAYER_COUNT, listenersBeforeRefresh, "each opened navigator registers exactly one click listener");

        // Simulates what NavigationHelper's refreshAfterWrite(1 minute) does after players
        // disconnect and reconnect: the loader runs again and a new builder replaces the old
        // one in the cache. Driven directly via the public LoadingCache#refresh(K) instead of
        // waiting a minute of wall-clock time or reflecting into Caffeine's internal ticker.
        List<CompletableFuture<PersonalInventoryBuilder>> refreshes = new ArrayList<>(PLAYER_COUNT);
        for (Player player : players) {
            refreshes.add(cache.refresh(player.getUuid()));
        }
        CompletableFuture.allOf(refreshes.toArray(new CompletableFuture[0])).join();

        List<EventNode<InventoryEvent>> nodesAfterRefresh = new ArrayList<>(PLAYER_COUNT);
        for (Player player : players) {
            PersonalInventoryBuilder refreshedBuilder = cache.asMap().get(player.getUuid());
            nodesAfterRefresh.add(refreshedBuilder.getInventory().eventNode());
        }

        int staleListenersOnReplacedInventories = countAll(nodesBeforeRefresh);
        int listenersOnFreshInventories = countAll(nodesAfterRefresh);

        assertEquals(0, staleListenersOnReplacedInventories, "NavigationHelper never calls PersonalInventoryBuilder#unregister() on a replaced builder, " + "so the old, discarded inventory keeps its click listener (and the Player it captured) " + "registered forever");
        assertEquals(listenersBeforeRefresh, listenersOnFreshInventories + staleListenersOnReplacedInventories, "the total number of registered navigator click listeners must stay unchanged across a cache refresh");
    }

    private static int countAll(List<EventNode<InventoryEvent>> nodes) {
        int total = 0;
        for (EventNode<InventoryEvent> node : nodes) {
            total += EventListenerCounter.countListeners(node);
        }
        return total;
    }
}

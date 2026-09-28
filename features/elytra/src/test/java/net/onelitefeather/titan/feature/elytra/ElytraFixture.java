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
package net.onelitefeather.titan.feature.elytra;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.mockito.Mockito;

/**
 * Test-only fixture that builds {@link ElytraModule} exactly as production does, against a fresh
 * {@code titan} node, without a running {@code BeanScope}. {@link LobbyItems} is a Mockito mock
 * here, standing in for {@code HotbarLobbyItems} (which lives in the {@code hotbar} column, not
 * this one): a column depends only on the {@code core} interface, never on another column's
 * implementation, and its own tests must not either.
 *
 * <p>{@link #useFirework(Player)} calls the firework {@link LobbyItem}'s {@code onUse} handler
 * directly, standing in for {@code HotbarLobbyItems}' tag-based dispatch from a used stack to that
 * handler - that routing is {@code hotbar}'s own responsibility and already covered by its tests.
 *
 * <p>Closing (ideally via try-with-resources) tears down every attached node in production order;
 * {@link #stopModule()} stops just the module, leaving the rest for {@link #close()}.
 */
final class ElytraFixture implements AutoCloseable {

    /** The tag a handed-out stack carries in production, stamped by {@code HotbarLobbyItems}. */
    private static final Tag<String> IDENTITY_TAG = Tag.String("titan:item");

    private final TestTitanNode titan;
    private final ElytraModule module;
    private final LobbyItem fireworkItem;
    private final FireworkBoostTracker boosts;
    private boolean moduleStopped;
    private boolean closed;

    private ElytraFixture(TestTitanNode titan, ElytraModule module, LobbyItem fireworkItem, FireworkBoostTracker boosts) {
        this.titan = titan;
        this.module = module;
        this.fireworkItem = fireworkItem;
        this.boosts = boosts;
    }

    static ElytraFixture start(Env env) {
        TestTitanNode titan = TestTitanNode.attach(env);
        FireworkBoostTracker boosts = new FireworkBoostTracker();
        LobbyItem fireworkItem = new ElytraLobbyItems().firework(boosts);
        LobbyItems lobbyItems = stubLobbyItems(fireworkItem);
        Scheduler scheduler = env.process().scheduler();
        ElytraModule module = new ElytraModule(titan.node(), lobbyItems, boosts, scheduler);
        module.start();
        return new ElytraFixture(titan, module, fireworkItem, boosts);
    }

    /**
     * The stack {@link ElytraModule} hands into the offhand on start-flying, stamped as production
     * would.
     */
    ItemStack stampedFireworkStack() {
        return this.fireworkItem.itemStack().withTag(IDENTITY_TAG, ElytraLobbyItems.FIREWORK_KEY.asString());
    }

    /** Uses the firework directly against its own {@code onUse} handler; see the class Javadoc. */
    void useFirework(Player player) {
        this.fireworkItem.onUse().handle(player, new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFireworkStack(), 1));
    }

    FireworkBoostTracker boosts() {
        return this.boosts;
    }

    /**
     * Stops only the module, to prove nothing runs once it has, leaving the rest for
     * {@link #close()}.
     */
    void stopModule() {
        if (!this.moduleStopped) {
            this.moduleStopped = true;
            this.module.stop();
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        stopModule();
        this.titan.close();
    }

    private static LobbyItems stubLobbyItems(LobbyItem fireworkItem) {
        ItemStack stamped = fireworkItem.itemStack().withTag(IDENTITY_TAG, ElytraLobbyItems.FIREWORK_KEY.asString());
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);
        Mockito.when(lobbyItems.stack(ElytraLobbyItems.FIREWORK_KEY.asString())).thenReturn(stamped);
        return lobbyItems;
    }
}

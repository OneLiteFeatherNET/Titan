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
package net.onelitefeather.titan.app.feature.elytra;

import java.util.List;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.item.LobbyItems;

/**
 * Test-only fixture that builds {@link ElytraModule} and its two {@link LobbyItem}s exactly as
 * {@code PlatformBeans}/{@link ElytraLobbyItems} do in production, against a fresh {@code titan}
 * node attached to {@code env}'s global event handler and {@code env}'s own scheduler - so a test
 * can equip a player, fly, and drive the boost task with {@link Env#tick()} without a running
 * {@code BeanScope}.
 *
 * <p>Close it (or use try-with-resources) once the test is done; this detaches every node the
 * fixture attached, in the same order production tears down: the module's own node and its
 * per-tick task first (via {@link ElytraModule#stop()}), then the platform-wide {@link LobbyItems}
 * dispatcher, then the fixture's own {@code titan} node. {@link #stopModule()} lets a test stop
 * just the module - to prove nothing of the feature runs anymore once it has - while leaving the
 * rest for {@link #close()} to tear down safely.
 */
final class ElytraFixture implements AutoCloseable {

    private final Env env;
    private final EventNode<Event> titan;
    private final ElytraModule module;
    private final LobbyItems lobbyItems;
    private final FireworkBoostTracker boosts;
    private boolean moduleStopped;
    private boolean closed;

    private ElytraFixture(Env env, EventNode<Event> titan, ElytraModule module, LobbyItems lobbyItems, FireworkBoostTracker boosts) {
        this.env = env;
        this.titan = titan;
        this.module = module;
        this.lobbyItems = lobbyItems;
        this.boosts = boosts;
    }

    static ElytraFixture start(Env env) {
        EventNode<Event> titan = EventNode.all("test-titan-" + UUID.randomUUID());
        env.process().eventHandler().addChild(titan);
        FireworkBoostTracker boosts = new FireworkBoostTracker();
        ElytraLobbyItems factory = new ElytraLobbyItems();
        LobbyItem elytraItem = factory.elytraChestplate();
        LobbyItem fireworkItem = factory.firework(boosts);
        LobbyItems lobbyItems = new LobbyItems(List.of(elytraItem, fireworkItem), titan);
        Scheduler scheduler = env.process().scheduler();
        ElytraModule module = new ElytraModule(titan, lobbyItems, boosts, scheduler);
        module.start();
        return new ElytraFixture(env, titan, module, lobbyItems, boosts);
    }

    void equip(Player player) {
        this.lobbyItems.equip(player);
    }

    /** @return the tracker shared by the module and the firework's use handler, for assertions */
    FireworkBoostTracker boosts() {
        return this.boosts;
    }

    /**
     * Stops only {@link ElytraModule}, leaving {@link LobbyItems} and the fixture's own
     * {@code titan} node attached - for a test that checks behaviour once just the module has torn
     * down. Safe to call more than once, and safe to combine with {@link #close()} afterwards.
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
        this.lobbyItems.stop();
        this.env.process().eventHandler().removeChild(this.titan);
    }
}

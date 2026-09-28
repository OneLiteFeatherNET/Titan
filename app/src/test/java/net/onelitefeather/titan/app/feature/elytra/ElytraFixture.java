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
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.item.LobbyItems;
import net.onelitefeather.titan.app.testutils.TestTitanNode;

/**
 * Test-only fixture that builds {@link ElytraModule} and its two {@link LobbyItem}s exactly as
 * production does, against a fresh {@code titan} node, without a running {@code BeanScope}.
 *
 * <p>Closing (ideally via try-with-resources) tears down every attached node in production order;
 * {@link #stopModule()} stops just the module, leaving the rest for {@link #close()}.
 */
final class ElytraFixture implements AutoCloseable {

    private final TestTitanNode titan;
    private final ElytraModule module;
    private final LobbyItems lobbyItems;
    private final FireworkBoostTracker boosts;
    private boolean moduleStopped;
    private boolean closed;

    private ElytraFixture(TestTitanNode titan, ElytraModule module, LobbyItems lobbyItems, FireworkBoostTracker boosts) {
        this.titan = titan;
        this.module = module;
        this.lobbyItems = lobbyItems;
        this.boosts = boosts;
    }

    static ElytraFixture start(Env env) {
        TestTitanNode titan = TestTitanNode.attach(env);
        FireworkBoostTracker boosts = new FireworkBoostTracker();
        ElytraLobbyItems factory = new ElytraLobbyItems();
        LobbyItem elytraItem = factory.elytraChestplate();
        LobbyItem fireworkItem = factory.firework(boosts);
        LobbyItems lobbyItems = new LobbyItems(List.of(elytraItem, fireworkItem), titan.node());
        Scheduler scheduler = env.process().scheduler();
        ElytraModule module = new ElytraModule(titan.node(), lobbyItems, boosts, scheduler);
        module.start();
        return new ElytraFixture(titan, module, lobbyItems, boosts);
    }

    void equip(Player player) {
        this.lobbyItems.equip(player);
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
        this.lobbyItems.stop();
        this.titan.close();
    }
}

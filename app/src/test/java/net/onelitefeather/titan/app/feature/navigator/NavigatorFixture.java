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
package net.onelitefeather.titan.app.feature.navigator;

import java.util.List;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.item.HotbarLobbyItems;
import net.onelitefeather.titan.app.testutils.TestTitanNode;
import net.onelitefeather.titan.core.feature.FeatureFlags;

/**
 * Test-only fixture that builds {@link NavigatorModule} and its {@code titan:navigator} feather
 * exactly as production does, against a fresh {@code titan} node, without a running
 * {@code BeanScope}.
 *
 * <p>Closing (ideally via try-with-resources) tears down every attached node in production order;
 * {@link #stopModule()} stops just the module, leaving the rest for {@link #close()}.
 */
final class NavigatorFixture implements AutoCloseable {

    private final TestTitanNode titan;
    private final NavigatorModule module;
    private final HotbarLobbyItems lobbyItems;
    private boolean moduleStopped;
    private boolean closed;

    private NavigatorFixture(TestTitanNode titan, NavigatorModule module, HotbarLobbyItems lobbyItems) {
        this.titan = titan;
        this.module = module;
        this.lobbyItems = lobbyItems;
    }

    static NavigatorFixture start(Env env, Deliver deliver, FeatureFlags featureFlags) {
        TestTitanNode titan = TestTitanNode.attach(env);
        NavigatorModule module = new NavigatorModule(titan.node(), deliver, featureFlags);
        module.start();
        LobbyItem feather = new NavigatorItems().navigatorFeather(module);
        HotbarLobbyItems lobbyItems = new HotbarLobbyItems(List.of(feather), titan.node());
        return new NavigatorFixture(titan, module, lobbyItems);
    }

    NavigatorModule module() {
        return this.module;
    }

    void equip(Player player) {
        this.lobbyItems.equip(player);
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

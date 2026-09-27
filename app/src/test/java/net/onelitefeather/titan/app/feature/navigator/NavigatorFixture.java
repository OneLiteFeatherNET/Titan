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
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.item.LobbyItems;
import net.onelitefeather.titan.common.feature.FeatureFlags;

/**
 * Test-only fixture that builds {@link NavigatorModule} and its {@code titan:navigator} feather
 * exactly as {@code PlatformBeans}/{@link NavigatorItems} do in production, against a fresh
 * {@code titan} node attached to {@code env}'s global event handler - so a test can equip and use
 * the feather without a running {@code BeanScope}.
 *
 * <p>Close it (or use try-with-resources) once the test is done; this detaches every node the
 * fixture attached, in the same order production tears down: the module's own node first (via
 * {@link NavigatorModule#stop()}), then the platform-wide {@link LobbyItems} dispatcher, then the
 * fixture's own {@code titan} node. {@link #stopModule()} lets a test stop just the module - to
 * prove nothing of the feature runs anymore once it has - while leaving the rest for {@link
 * #close()} to tear down safely.
 */
final class NavigatorFixture implements AutoCloseable {

    private final Env env;
    private final EventNode<Event> titan;
    private final NavigatorModule module;
    private final LobbyItems lobbyItems;
    private boolean moduleStopped;
    private boolean closed;

    private NavigatorFixture(Env env, EventNode<Event> titan, NavigatorModule module, LobbyItems lobbyItems) {
        this.env = env;
        this.titan = titan;
        this.module = module;
        this.lobbyItems = lobbyItems;
    }

    static NavigatorFixture start(Env env, Deliver deliver, FeatureFlags featureFlags) {
        EventNode<Event> titan = EventNode.all("test-titan-" + UUID.randomUUID());
        env.process().eventHandler().addChild(titan);
        NavigatorModule module = new NavigatorModule(titan, deliver, featureFlags);
        module.start();
        LobbyItem feather = new NavigatorItems().navigatorFeather(module);
        LobbyItems lobbyItems = new LobbyItems(List.of(feather), titan);
        return new NavigatorFixture(env, titan, module, lobbyItems);
    }

    NavigatorModule module() {
        return this.module;
    }

    void equip(Player player) {
        this.lobbyItems.equip(player);
    }

    /**
     * Stops only {@link NavigatorModule}, leaving {@link LobbyItems} and the fixture's own
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

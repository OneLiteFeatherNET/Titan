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

import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.testing.Env;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;

/**
 * Test-only fixture that builds {@link NavigatorModule} and its {@code titan:navigator} feather
 * exactly as production does, against a fresh {@code titan} node, without a running
 * {@code BeanScope}.
 *
 * <p>{@link #useFeather(Player)} calls the feather {@link LobbyItem}'s {@code onUse} handler
 * directly, standing in for {@code HotbarLobbyItems}' tag-based dispatch from a used stack to that
 * handler - that routing is {@code hotbar}'s own responsibility and already covered by its tests.
 *
 * <p>Closing (ideally via try-with-resources) tears down every attached node in production order;
 * {@link #stopModule()} stops just the module, leaving the rest for {@link #close()}.
 */
final class NavigatorFixture implements AutoCloseable {

    private final TestTitanNode titan;
    private final NavigatorModule module;
    private final LobbyItem feather;
    private boolean moduleStopped;
    private boolean closed;

    private NavigatorFixture(TestTitanNode titan, NavigatorModule module, LobbyItem feather) {
        this.titan = titan;
        this.module = module;
        this.feather = feather;
    }

    static NavigatorFixture start(Env env, Deliver deliver, FeatureFlags featureFlags) {
        TestTitanNode titan = TestTitanNode.attach(env);
        NavigatorModule module = new NavigatorModule(titan.node(), deliver, featureFlags);
        module.start();
        LobbyItem feather = new NavigatorItems().navigatorFeather(module);
        return new NavigatorFixture(titan, module, feather);
    }

    NavigatorModule module() {
        return this.module;
    }

    /** Places the feather in its production hotbar slot, standing in for hotbar's equip(). */
    void equip(Player player) {
        if (this.feather.placement() instanceof ItemSlot.Hotbar hotbar) {
            player.getInventory().setItemStack(hotbar.slot(), this.feather.itemStack());
        }
    }

    /** Uses the feather directly against its own {@code onUse} handler; see the class Javadoc. */
    void useFeather(Player player) {
        this.feather.onUse().handle(player, new PlayerUseItemEvent(player, PlayerHand.MAIN, this.feather.itemStack(), 0L));
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
}

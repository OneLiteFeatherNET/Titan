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

import java.util.List;
import java.util.Optional;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.lobby.LobbyIdentities;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;

/**
 * The switcher wired by hand the way Avaje wires it: three services of the task, this one being
 * {@code Lobby-2}. A fresh instance per test; {@link #close()} undoes every registration.
 */
final class SwitcherFixture implements AutoCloseable {

    static final int REFRESH_SECONDS = 5;
    static final LobbyIdentity OWN = new LobbyIdentity("Lobby", "Lobby-2");
    static final ServiceCount LOBBY_1 = new ServiceCount("Lobby-1", 3, 50);
    static final ServiceCount LOBBY_2 = new ServiceCount("Lobby-2", 8, 50);
    static final ServiceCount LOBBY_3 = new ServiceCount("Lobby-3", 50, 50);

    private final Env env;
    private final TestTitanNode titan;
    private final RecordingDeliver deliver = new RecordingDeliver();
    private final LobbySwitcherMessages messages = new LobbySwitcherMessages();
    private final FakePlayerCounts counts = new FakePlayerCounts(LOBBY_3, LOBBY_1, LOBBY_2);
    private final SwitcherInventory inventory;
    private final LobbySwitcherModule module;
    private final LobbyItem item;
    private final PlacingLobbyItems lobbyItems;

    SwitcherFixture(Env env, FeatureFlags flags, Optional<LobbyIdentity> identity) {
        this(env, flags, identity, Telemetry.noop());
    }

    SwitcherFixture(Env env, FeatureFlags flags, Optional<LobbyIdentity> identity, Telemetry telemetry) {
        this(env, flags, () -> identity, telemetry);
    }

    /** For tests where the identity appears after the beans are built, as in the real startup. */
    SwitcherFixture(Env env, FeatureFlags flags, LobbyIdentities identities, Telemetry telemetry) {
        this.env = env;
        this.titan = TestTitanNode.attach(env);
        this.messages.register();
        this.inventory = new SwitcherInventory(this.messages, this.counts, env.process().scheduler(), Runnable::run, this.deliver, telemetry, new LobbySwitcherSettings(REFRESH_SECONDS));
        this.inventory.start();
        this.module = new LobbySwitcherModule(this.titan.node(), this.inventory, identities, this.messages, telemetry);
        this.module.start();
        this.item = new LobbySwitcherItems().lobbySwitcherItem(this.module, flags, this.messages).orElse(null);
        this.lobbyItems = new PlacingLobbyItems(this.item == null ? List.of() : List.of(this.item));
    }

    static SwitcherFixture active(Env env) {
        return active(env, Telemetry.noop());
    }

    static SwitcherFixture active(Env env, Telemetry telemetry) {
        return new SwitcherFixture(env, new FakeFeatureFlags(LobbySwitcherModule.FLAG), Optional.of(OWN), telemetry);
    }

    RecordingDeliver deliver() {
        return this.deliver;
    }

    /** Opens the list for a fresh player and runs the ticks that show it. */
    Player joinAndOpen() {
        Player player = join(this.env.createFlatInstance());
        use(player);
        settle();
        return player;
    }

    /** The read is applied on the next tick and Aves fills the list on the one after. */
    void settle() {
        this.env.tick();
        this.env.tick();
    }

    /** Clicks {@code slot} of the inventory the player has open, as the client would. */
    void click(Player player, int slot) {
        this.env.process().eventHandler().call(new InventoryPreClickEvent(player.getOpenInventory(), player, new Click.Left(slot)));
    }

    EventNode<Event> titanNode() {
        return this.titan.node();
    }

    FakePlayerCounts counts() {
        return this.counts;
    }

    SwitcherInventory inventory() {
        return this.inventory;
    }

    LobbySwitcherModule module() {
        return this.module;
    }

    LobbySwitcherMessages messages() {
        return this.messages;
    }

    /** {@code null} when the flag or the identity said no. */
    LobbyItem item() {
        return this.item;
    }

    Player join(Instance instance) {
        Player player = this.env.createConnection().connect(instance);
        equip(player);
        return player;
    }

    /** Gives the player the lobby items a joining player gets. */
    void equip(Player player) {
        this.lobbyItems.equip(player);
    }

    void use(Player player) {
        this.item.onUse().handle(player, new PlayerUseItemEvent(player, PlayerHand.MAIN, this.item.itemStack(), 0L));
    }

    @Override
    public void close() {
        this.module.stop();
        this.inventory.stop();
        this.messages.close();
        this.titan.close();
    }
}

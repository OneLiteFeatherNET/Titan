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
package net.onelitefeather.titan.app.module;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.testutils.EventListenerCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Covers the {@code lobby-modules} spec's "restart a single module at runtime" scenarios for
 * {@link ModuleRegistry#restart(String)}: no extra listeners after ten restarts, an online player
 * getting the restarted module's item back without rejoining, and a restart never touching any
 * other module. See {@code design.md}, decision 3.
 *
 * <p>Every test overrides the tick-thread check {@link ModuleRegistry#restart(String)} otherwise
 * enforces - see {@link ModuleRegistry.Builder#tickThreadCheck} - because Cyano's {@code Env} runs
 * every tick synchronously on the JUnit thread, never on a real
 * {@link net.minestom.server.thread.TickThread}. The guard itself is covered separately, by
 * {@link #restartOutsideTheTickThreadFailsFast()} below, which leaves the default check in place.
 */
@ExtendWith(MicrotusExtension.class)
class ModuleRestartTest {

    private record TestEvent() implements Event {
    }

    /**
     * A restartable module that registers one listener and one fixed hotbar item every time it
     * enables - enough to prove both "no leaked listeners" and "players keep their items" across a
     * restart.
     */
    private static final class HotbarModule implements LobbyModule {

        private final String id;
        private final int hotbarSlot;

        HotbarModule(String id, int hotbarSlot) {
            this.id = id;
            this.hotbarSlot = hotbarSlot;
        }

        @Override
        public String id() {
            return this.id;
        }

        @Override
        public void enable(ModuleContext context) {
            context.listen(TestEvent.class, event -> {
            });
            context.items().register(new LobbyItem(Key.key("titan:" + this.id), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(this.hotbarSlot), (player, event) -> {
            }));
        }
    }

    private static ModuleRegistry.Builder builder(Env env, EventNode<Event> parent, ItemRegistry items) {
        return ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command()).connectionManager(env.process().connection()).items(items)
                // See the class-level Javadoc: Env#tick() never runs on a real TickThread.
                .tickThreadCheck(() -> true);
    }

    private static EventNode<Event> moduleNode(EventNode<Event> parent, String moduleId) {
        List<EventNode<Event>> children = parent.findChildren("titan/" + moduleId);
        Assertions.assertEquals(1, children.size(), "expected exactly one 'titan/" + moduleId + "' child node");
        return children.get(0);
    }

    @DisplayName("Restarting a module ten times leaves the same number of listeners as after the first start")
    @Test
    void restartingTenTimesLeavesNoExtraListeners(Env env) {
        EventNode<Event> parent = EventNode.all("test-restart-listener-count");
        ItemRegistry items = new ItemRegistry(parent);
        ModuleRegistry registry = builder(env, parent, items).modules(new HotbarModule("sit", 0)).build();
        registry.enableAll();
        int listenersAfterFirstStart = EventListenerCounter.countListeners(moduleNode(parent, "sit"));

        for (int i = 0; i < 10; i++) {
            RestartOutcome outcome = registry.restart("sit");
            Assertions.assertInstanceOf(RestartOutcome.Restarted.class, outcome, "restart " + i + " must succeed");
        }

        Assertions.assertEquals(listenersAfterFirstStart, EventListenerCounter.countListeners(moduleNode(parent, "sit")), "ten restarts must leave exactly as many listeners as the first start did");
    }

    @DisplayName("An online player gets the module's hotbar item back after a restart, without rejoining")
    @Test
    void onlinePlayerGetsItemBackAfterRestart(Env env) {
        EventNode<Event> parent = EventNode.all("test-restart-hotbar-item");
        ItemRegistry items = new ItemRegistry(parent);
        ModuleRegistry registry = builder(env, parent, items).modules(new HotbarModule("sit", 0)).build();
        registry.enableAll();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        items.equip(player);
        Assertions.assertFalse(player.getInventory().getItemStack(0).isAir(), "the item must be in place before the restart");

        RestartOutcome outcome = registry.restart("sit");

        Assertions.assertInstanceOf(RestartOutcome.Restarted.class, outcome);
        Assertions.assertFalse(player.getInventory().getItemStack(0).isAir(), "the online player must have the item back after the restart, without rejoining");
    }

    @DisplayName("Restarting one module calls no disable/enable on any other module")
    @Test
    void restartingOneModuleDoesNotTouchAnyOtherModule(Env env) {
        List<String> log = new ArrayList<>();
        EventNode<Event> parent = EventNode.all("test-restart-isolation");
        ItemRegistry items = new ItemRegistry(parent);
        RecordingModule sit = new RecordingModule("sit", log);
        RecordingModule tickle = new RecordingModule("tickle", log);
        RecordingModule friends = new RecordingModule("friends", log);
        ModuleRegistry registry = builder(env, parent, items).modules(sit, tickle, friends).build();
        registry.enableAll();
        log.clear();

        RestartOutcome outcome = registry.restart("tickle");

        Assertions.assertInstanceOf(RestartOutcome.Restarted.class, outcome);
        Assertions.assertEquals(List.of("disable:tickle", "enable:tickle"), log, "only the restarted module's own disable/enable may run - no other module is touched");
    }

    @DisplayName("restart() outside the tick thread fails fast instead of silently running")
    @Test
    void restartOutsideTheTickThreadFailsFast() {
        EventNode<Event> parent = EventNode.all("test-restart-tick-thread-guard");
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(Scheduler.newScheduler()).commandManager(new CommandManager()).connectionManager(new ConnectionManager()).modules(new RecordingModule("sit", new ArrayList<>())).build();
        registry.enableAll();

        Assertions.assertThrows(IllegalStateException.class, () -> registry.restart("sit"), "the JUnit test thread is never a real TickThread, so the default guard must reject it");
    }

    @DisplayName("restart() of an unknown module id reports it clearly instead of doing nothing silently")
    @Test
    void restartOfAnUnknownModuleIdIsRejected(Env env) {
        EventNode<Event> parent = EventNode.all("test-restart-unknown-module");
        ItemRegistry items = new ItemRegistry(parent);
        ModuleRegistry registry = builder(env, parent, items).modules(new HotbarModule("sit", 0)).build();
        registry.enableAll();

        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> registry.restart("does-not-exist"));

        Assertions.assertTrue(thrown.getMessage().contains("does-not-exist"), "the message must name the unknown id");
    }
}

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

import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit-level coverage for the {@code lobby-modules} spec's "start with new values fails" scenario:
 * a module whose {@code enable} throws the second time it runs - i.e. on the restart, having
 * already enabled successfully once - must end {@link ModuleRegistry#restart(String)} as a
 * {@link RestartOutcome.Failed}, with the partial start torn down as completely as a normal
 * {@code disable} would: no lingering event node, no listener still firing, and no task or item
 * left behind. See {@code design.md}, decision 3.
 */
@ExtendWith(MicrotusExtension.class)
class ModuleRestartFailureTest {

    private record TestEvent() implements Event {
    }

    /** Enables successfully the first time, then throws on every enable after that. */
    private static final class FailsOnSecondEnableModule implements LobbyModule {

        private final AtomicInteger taskRuns;
        private int enableCalls;

        FailsOnSecondEnableModule(AtomicInteger taskRuns) {
            this.taskRuns = taskRuns;
        }

        @Override
        public String id() {
            return "flaky";
        }

        @Override
        public void enable(ModuleContext context) {
            this.enableCalls++;
            if (this.enableCalls == 2) {
                throw new IllegalStateException("boom on second enable");
            }
            context.listen(TestEvent.class, event -> {
            });
            context.tasks().schedule(this.taskRuns::incrementAndGet, TaskSchedule.immediate(), TaskSchedule.tick(1));
            context.items().register(new LobbyItem(Key.key("titan:flaky"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(0), (player, event) -> {
            }));
        }
    }

    @DisplayName("A module that fails its second enable() ends the restart as Failed, with no listeners, tasks or items left")
    @Test
    void restartFailsCleanlyWhenEnableThrowsTheSecondTime(Env env) {
        EventNode<Event> parent = EventNode.all("test-restart-failure");
        ItemRegistry items = new ItemRegistry(parent);
        AtomicInteger taskRuns = new AtomicInteger();
        FailsOnSecondEnableModule module = new FailsOnSecondEnableModule(taskRuns);
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command()).connectionManager(env.process().connection()).items(items)
                // See ModuleRestartTest's class-level Javadoc: Env#tick() never runs on a real TickThread.
                .tickThreadCheck(() -> true).modules(module).build();
        registry.enableAll();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        items.equip(player);
        Assertions.assertFalse(player.getInventory().getItemStack(0).isAir(), "the item must be in place after the first, successful enable");
        env.tick();
        env.tick();
        int taskRunsBeforeRestart = taskRuns.get();
        Assertions.assertTrue(taskRunsBeforeRestart > 0, "the task must have run at least once while the module was up");

        RestartOutcome outcome = registry.restart("flaky");

        Assertions.assertInstanceOf(RestartOutcome.Failed.class, outcome, "the second enable() throwing must surface as Failed, not an exception out of restart()");
        Assertions.assertInstanceOf(IllegalStateException.class, ((RestartOutcome.Failed) outcome).cause(), "the original failure must be preserved as the cause");
        Assertions.assertTrue(parent.findChildren("titan/flaky").isEmpty(), "no event node - and so no listener - may remain for the failed module");
        items.equip(player);
        Assertions.assertTrue(player.getInventory().getItemStack(0).isAir(), "the item must be gone once the failed restart tore the partial start down");
        env.tick();
        env.tick();
        Assertions.assertEquals(taskRunsBeforeRestart, taskRuns.get(), "the task must not run again after the failed restart tore it down");
    }
}

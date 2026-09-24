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
package net.onelitefeather.titan.app.module.testing;

import java.nio.file.Path;
import java.util.UUID;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.common.config.ConfigStore;
import org.jetbrains.annotations.Nullable;

/**
 * A shared, test-only harness that starts one or more {@link LobbyModule}s through a real
 * {@link ModuleRegistry}, so feature-module tests don't each have to wire up their own registry,
 * item registry and navigator entries.
 *
 * <p>{@link #start(Env, LobbyModule...)} attaches a fresh child node under the given Microtus
 * {@code Env}'s global event handler ({@code env.process().eventHandler()}) as the registry's
 * parent, and uses the {@code Env}'s scheduler and command manager - so events fired for a player
 * created through that same {@code Env} (e.g. {@code parent.call(event)} or a real interaction)
 * reach the started modules. Use this for a module whose behaviour needs a real {@link
 * net.minestom.server.entity.Player} or {@link net.minestom.server.instance.Instance}.
 *
 * <p>{@link #startStandalone(LobbyModule...)} does the same without an {@code Env}, backed by a
 * standalone {@link Scheduler#newScheduler()}, a plain {@link CommandManager} and a bare {@link
 * EventNode#all(String)} parent - the same pattern {@code ModuleContextConfigTest} and {@code
 * NavigatorEntriesWiringTest} already used before this harness existed. Use this whenever a
 * module's logic can be exercised by calling {@link #registry()}'s event node directly, without
 * booting a server.
 *
 * <p>Both entry points have an overload taking a {@link ConfigStore} (or a {@link Path} to one),
 * for modules whose {@link net.onelitefeather.titan.app.module.ModuleContext#config} reading needs
 * covering.
 *
 * <p>A harness is meant to live for a single test: {@link #close()} (or a try-with-resources block,
 * since this class is {@link AutoCloseable}) calls {@link ModuleRegistry#disableAll()} and detaches
 * the harness's own parent node again, so two harnesses started in sequence - in the same test or
 * across tests - never leak listeners into each other.
 *
 * <pre>{@code
 * @ExtendWith(MicrotusExtension.class)
 * class SitModuleTest {
 * 
 * @Test
 *       void sittingOnAStairEmitsASitEvent(Env env) {
 *       try (ModuleHarness harness = ModuleHarness.start(env, new SitModule())) {
 *       Player player = env.createPlayer(env.createFlatInstance());
 *       // ... fire the event that should make the module react, then assert on the result
 *       }
 *       }
 *       }
 *       }</pre>
 */
public final class ModuleHarness implements AutoCloseable {

    private final @Nullable EventNode<Event> attachedTo;
    private final EventNode<Event> parent;
    private final ModuleRegistry registry;
    private final ItemRegistry items;
    private final NavigatorEntries navigator;
    private boolean closed;

    private ModuleHarness(@Nullable EventNode<Event> attachedTo, EventNode<Event> parent, ModuleRegistry registry, ItemRegistry items, NavigatorEntries navigator) {
        this.attachedTo = attachedTo;
        this.parent = parent;
        this.registry = registry;
        this.items = items;
        this.navigator = navigator;
    }

    /**
     * Starts {@code modules} against {@code env}, with no {@link ConfigStore} configured - every
     * module's {@code context.config(...)} call returns its own defaults unchanged.
     *
     * @param env     the Microtus environment to attach to and to take the scheduler and command
     *                manager from
     * @param modules the modules to start, in registration order
     * @return a started harness; close it (or use try-with-resources) once the test is done
     */
    public static ModuleHarness start(Env env, LobbyModule... modules) {
        return start(env, (ConfigStore) null, modules);
    }

    /**
     * Starts {@code modules} against {@code env}, reading their configuration from the document at
     * {@code configFile} (opened via {@link ConfigStore#open(Path)}).
     *
     * @param env        the Microtus environment to attach to and to take the scheduler and command
     *                   manager from
     * @param configFile the configuration document the started modules read their section from
     * @param modules    the modules to start, in registration order
     * @return a started harness; close it (or use try-with-resources) once the test is done
     */
    public static ModuleHarness start(Env env, Path configFile, LobbyModule... modules) {
        return start(env, ConfigStore.open(configFile), modules);
    }

    /**
     * Starts {@code modules} against {@code env}, reading their configuration from
     * {@code configStore}.
     *
     * @param env         the Microtus environment to attach to and to take the scheduler and
     *                    command manager from
     * @param configStore the {@link ConfigStore} the started modules read their section from, or
     *                    {@code null} for none
     * @param modules     the modules to start, in registration order
     * @return a started harness; close it (or use try-with-resources) once the test is done
     */
    public static ModuleHarness start(Env env, @Nullable ConfigStore configStore, LobbyModule... modules) {
        EventNode<Event> globalNode = env.process().eventHandler();
        EventNode<Event> parent = EventNode.all("module-harness/" + UUID.randomUUID());
        globalNode.addChild(parent);
        return start(globalNode, parent, env.process().scheduler(), env.process().command(), configStore, modules);
    }

    /**
     * Starts {@code modules} without a Microtus {@code Env}, for modules whose behaviour under test
     * needs no real {@link net.minestom.server.entity.Player} or
     * {@link net.minestom.server.instance.Instance}. No {@link ConfigStore} is configured.
     *
     * @param modules the modules to start, in registration order
     * @return a started harness; close it (or use try-with-resources) once the test is done
     */
    public static ModuleHarness startStandalone(LobbyModule... modules) {
        return startStandalone((ConfigStore) null, modules);
    }

    /**
     * Starts {@code modules} without a Microtus {@code Env}, reading their configuration from the
     * document at {@code configFile} (opened via {@link ConfigStore#open(Path)}).
     *
     * @param configFile the configuration document the started modules read their section from
     * @param modules    the modules to start, in registration order
     * @return a started harness; close it (or use try-with-resources) once the test is done
     */
    public static ModuleHarness startStandalone(Path configFile, LobbyModule... modules) {
        return startStandalone(ConfigStore.open(configFile), modules);
    }

    /**
     * Starts {@code modules} without a Microtus {@code Env}, reading their configuration from
     * {@code configStore}.
     *
     * @param configStore the {@link ConfigStore} the started modules read their section from, or
     *                    {@code null} for none
     * @param modules     the modules to start, in registration order
     * @return a started harness; close it (or use try-with-resources) once the test is done
     */
    public static ModuleHarness startStandalone(@Nullable ConfigStore configStore, LobbyModule... modules) {
        EventNode<Event> parent = EventNode.all("module-harness/" + UUID.randomUUID());
        return start(null, parent, Scheduler.newScheduler(), new CommandManager(), configStore, modules);
    }

    private static ModuleHarness start(@Nullable EventNode<Event> attachedTo, EventNode<Event> parent, Scheduler scheduler, CommandManager commandManager, @Nullable ConfigStore configStore, LobbyModule... modules) {
        ItemRegistry items = new ItemRegistry(parent);
        NavigatorEntries navigator = new NavigatorEntries();
        ModuleRegistry.Builder builder = ModuleRegistry.builder().parent(parent).scheduler(scheduler).commandManager(commandManager).items(items).navigator(navigator).modules(modules);
        if (configStore != null) {
            builder.config(configStore);
        }
        ModuleRegistry registry = builder.build();
        registry.enableAll();
        return new ModuleHarness(attachedTo, parent, registry, items, navigator);
    }

    /**
     * @return the item registry shared by every module started through this harness
     */
    public ItemRegistry items() {
        return this.items;
    }

    /**
     * @return the navigator entries shared by every module started through this harness
     */
    public NavigatorEntries navigator() {
        return this.navigator;
    }

    /**
     * @return the {@link ModuleRegistry} this harness started; use its parent node - reached
     *         indirectly, e.g. by calling an event on the same node the harness attached to - to
     *         fire events at the started modules
     */
    public ModuleRegistry registry() {
        return this.registry;
    }

    /**
     * Stops every module started through this harness ({@link ModuleRegistry#disableAll()}) and, if
     * this harness was started against an {@code Env}, detaches its own parent node from the global
     * event handler again. Safe to call more than once. Not calling this leaks the harness's parent
     * node (and every listener still hanging off it) into the {@code Env} for the rest of the test
     * run.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.registry.disableAll();
        if (this.attachedTo != null) {
            this.attachedTo.removeChild(this.parent);
        }
    }
}

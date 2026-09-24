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
package net.onelitefeather.titan.app.module.navigator;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Covers the {@code lobby-navigator} spec end to end through {@link ModuleRegistry} and
 * {@link ModuleContext#navigator()}: a module contributing an entry through the platform, that
 * entry
 * disappearing once the module is disabled, and two modules whose entries share a slot aborting
 * {@link ModuleRegistry#enableAll()}.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorEntriesIntegrationTest {

    private static final ItemStack ICON = ItemStack.of(Material.FEATHER);

    private static NavigatorEntry entryAt(int slot, String destination) {
        return new NavigatorEntry(slot, ICON, Component.text(destination), destination);
    }

    /** A minimal {@link LobbyModule} that adds one navigator entry when enabled. */
    private static final class NavigatorContributingModule implements LobbyModule {

        private final String id;
        private final NavigatorEntry entry;

        NavigatorContributingModule(String id, NavigatorEntry entry) {
            this.id = id;
            this.entry = entry;
        }

        @Override
        public String id() {
            return this.id;
        }

        @Override
        public void enable(ModuleContext context) {
            context.navigator().add(this.entry);
        }
    }

    @DisplayName("A module's navigator entry appears in the shared registry")
    @Test
    void moduleContributesAnEntry(Env env) {
        EventNode<Event> parent = EventNode.all("test-navigator-contributes");
        NavigatorEntries navigatorEntries = new NavigatorEntries();
        NavigatorContributingModule module = new NavigatorContributingModule("teaser", entryAt(2, "Voyager"));
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command()).navigator(navigatorEntries).modules(module).build();

        registry.enableAll();

        Assertions.assertEquals(List.of(entryAt(2, "Voyager")), navigatorEntries.entries());
    }

    @DisplayName("A module's navigator entry disappears once the module is disabled")
    @Test
    void entryDisappearsAfterModuleDisabled(Env env) {
        EventNode<Event> parent = EventNode.all("test-navigator-disable");
        NavigatorEntries navigatorEntries = new NavigatorEntries();
        NavigatorContributingModule module = new NavigatorContributingModule("teaser", entryAt(2, "Voyager"));
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command()).navigator(navigatorEntries).modules(module).build();
        registry.enableAll();
        Assertions.assertFalse(navigatorEntries.entries().isEmpty(), "the entry must be present while the module is enabled");

        registry.disableAll();

        Assertions.assertTrue(navigatorEntries.entries().isEmpty(), "the entry must be gone once the module is disabled");
    }

    @DisplayName("Two modules contributing entries on the same slot abort enableAll(), naming the slot and both modules")
    @Test
    void conflictingSlotsAbortStartup(Env env) {
        EventNode<Event> parent = EventNode.all("test-navigator-conflict");
        NavigatorEntries navigatorEntries = new NavigatorEntries();
        NavigatorContributingModule survival = new NavigatorContributingModule("navigator", entryAt(4, "Survival"));
        NavigatorContributingModule teaser = new NavigatorContributingModule("teaser", entryAt(4, "Voyager"));
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command()).navigator(navigatorEntries).modules(survival, teaser).build();

        NavigatorConflictException thrown = Assertions.assertThrows(NavigatorConflictException.class, registry::enableAll);

        Assertions.assertTrue(thrown.getMessage().contains("4"));
        Assertions.assertTrue(thrown.getMessage().contains("navigator"));
        Assertions.assertTrue(thrown.getMessage().contains("teaser"));
    }
}

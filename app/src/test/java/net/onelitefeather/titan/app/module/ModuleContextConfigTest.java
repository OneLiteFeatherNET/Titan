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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.item.ItemPlacementConflictException;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.navigator.NavigatorConflictException;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import net.onelitefeather.titan.common.config.ConfigException;
import net.onelitefeather.titan.common.config.ConfigStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the {@code lobby-module-config} spec scenarios for the binding between {@link
 * ModuleContext#config} and {@link ConfigStore}: a module reading its own section, defaults for a
 * missing section (and the file being written after {@link ModuleRegistry#enableAll()}), startup
 * aborting on an invalid value with the module, field and reason named, a module never seeing
 * another module's section, no-store-configured falling back to defaults, and {@code config} being
 * closed for use once {@code enable()} returns - mirroring {@link ModuleContextTest}'s coverage of
 * {@link ModuleContext#listen}.
 *
 * <p>Plain unit test: none of this needs a {@link net.minestom.server.entity.Player} or
 * {@link net.minestom.server.instance.Instance}, so it builds {@link ModuleRegistry} from a
 * standalone {@link Scheduler#newScheduler()} and {@link CommandManager} instead of booting a
 * Microtus {@code Env}. One exception: {@code itemPlacementConflictAbortsEnableAllBeforeFlush}
 * still needs the server, since stamping an item's identity tag resolves its {@code Material}
 * through Minestom's registry data - see the comment on that method.
 */
class ModuleContextConfigTest {

    private record TestConfig(int value) {
        static final TestConfig DEFAULTS = new TestConfig(7);
    }

    private record OtherConfig(String label) {
        static final OtherConfig DEFAULTS = new OtherConfig("default-label");
    }

    private record ValidatedConfig(long cooldownMillis) {
        static final ValidatedConfig DEFAULTS = new ValidatedConfig(4000);

        ValidatedConfig {
            if (cooldownMillis < 0) {
                throw ConfigException.invalid("cooldownMillis", "must not be negative");
            }
        }
    }

    private static ModuleRegistry.Builder builder(EventNode<Event> parent) {
        return ModuleRegistry.builder().parent(parent).scheduler(Scheduler.newScheduler()).commandManager(new CommandManager());
    }

    @DisplayName("A module reads its own section, and never another module's")
    @Test
    void moduleReadsItsOwnSectionOnly(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("app.json");
        Files.writeString(file, "{\"configVersion\":2,\"sit\":{\"value\":42},\"navigator\":{\"label\":\"nav\"}}");
        ConfigStore store = ConfigStore.open(file);
        EventNode<Event> parent = EventNode.all("test-config-own-section");
        List<String> log = new ArrayList<>();
        AtomicReference<TestConfig> sitConfig = new AtomicReference<>();
        AtomicReference<OtherConfig> navigatorConfig = new AtomicReference<>();
        RecordingModule sit = new RecordingModule("sit", log, context -> sitConfig.set(context.config(TestConfig.class, TestConfig.DEFAULTS)), () -> {
        });
        RecordingModule navigator = new RecordingModule("navigator", log, context -> navigatorConfig.set(context.config(OtherConfig.class, OtherConfig.DEFAULTS)), () -> {
        });
        ModuleRegistry registry = builder(parent).config(store).modules(sit, navigator).build();

        registry.enableAll();

        Assertions.assertEquals(42, sitConfig.get().value(), "the 'sit' module must see its own section");
        Assertions.assertEquals("nav", navigatorConfig.get().label(), "the 'navigator' module must see its own section, not 'sit'");
    }

    @DisplayName("A missing section falls back to defaults, and app.json is written after enableAll()")
    @Test
    void missingSectionUsesDefaultsAndWritesFileOnFirstStart(@TempDir Path dir) {
        Path file = dir.resolve("app.json");
        Assertions.assertFalse(Files.exists(file));
        ConfigStore store = ConfigStore.open(file);
        EventNode<Event> parent = EventNode.all("test-config-missing-section");
        List<String> log = new ArrayList<>();
        AtomicReference<TestConfig> captured = new AtomicReference<>();
        RecordingModule module = new RecordingModule("sit", log, context -> captured.set(context.config(TestConfig.class, TestConfig.DEFAULTS)), () -> {
        });
        ModuleRegistry registry = builder(parent).config(store).modules(module).build();

        registry.enableAll();

        Assertions.assertEquals(7, captured.get().value(), "a missing section must fall back to the default, not 0");
        Assertions.assertTrue(Files.exists(file), "the first start must write app.json");
    }

    @DisplayName("An invalid value aborts enableAll(), naming module, field and reason, without writing a file")
    @Test
    void invalidValueAbortsEnableAllNamingModuleFieldAndReason(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("app.json");
        Files.writeString(file, "{\"configVersion\":2,\"tickle\":{\"cooldownMillis\":-5}}");
        ConfigStore store = ConfigStore.open(file);
        long lastModifiedBefore = Files.getLastModifiedTime(file).toMillis();
        EventNode<Event> parent = EventNode.all("test-config-invalid-value");
        List<String> log = new ArrayList<>();
        RecordingModule module = new RecordingModule("tickle", log, context -> context.config(ValidatedConfig.class, ValidatedConfig.DEFAULTS), () -> {
        });
        ModuleRegistry registry = builder(parent).config(store).modules(module).build();

        ModuleLifecycleException thrown = Assertions.assertThrows(ModuleLifecycleException.class, registry::enableAll);

        Assertions.assertTrue(thrown.getMessage().contains("tickle"), "the exception must name the failing module");
        Assertions.assertInstanceOf(ConfigException.class, thrown.getCause(), "the ConfigException must be preserved as the cause");
        ConfigException cause = (ConfigException) thrown.getCause();
        Assertions.assertEquals("tickle", cause.section(), "the cause must name the section (module)");
        Assertions.assertEquals("cooldownMillis", cause.field(), "the cause must name the field");
        Assertions.assertEquals("must not be negative", cause.reason(), "the cause must name the reason");
        Assertions.assertEquals(lastModifiedBefore, Files.getLastModifiedTime(file).toMillis(), "an aborted start must not rewrite the file");
    }

    @DisplayName("Without a configured ConfigStore, config() returns the defaults")
    @Test
    void noConfigStoreConfiguredReturnsDefaults() {
        EventNode<Event> parent = EventNode.all("test-config-no-store");
        List<String> log = new ArrayList<>();
        AtomicReference<TestConfig> captured = new AtomicReference<>();
        RecordingModule module = new RecordingModule("sit", log, context -> captured.set(context.config(TestConfig.class, TestConfig.DEFAULTS)), () -> {
        });
        ModuleRegistry registry = builder(parent).modules(module).build();

        Assertions.assertDoesNotThrow(registry::enableAll);

        Assertions.assertSame(TestConfig.DEFAULTS, captured.get(), "with no ConfigStore configured, config() must return the defaults unchanged");
    }

    // Needs the server: ItemRegistry.register() stamps the identity tag via ItemStack.withTag(),
    // which resolves the stack's Material through Minestom's registry data - unbound unless
    // MinecraftServer has been initialized. The unused `env` parameter is what makes
    // MicrotusExtension actually perform that initialization before the test runs. Every other
    // test in this class stays a plain unit test.
    @DisplayName("An item placement conflict aborts enableAll() before the config file is flushed")
    @ExtendWith(MicrotusExtension.class)
    @Test
    void itemPlacementConflictAbortsEnableAllBeforeFlush(@SuppressWarnings("unused") Env env, @TempDir Path dir) {
        Path file = dir.resolve("app.json");
        Assertions.assertFalse(Files.exists(file));
        ConfigStore store = ConfigStore.open(file);
        EventNode<Event> parent = EventNode.all("test-config-item-conflict-flush");
        List<String> log = new ArrayList<>();
        RecordingModule a = new RecordingModule("a", log, context -> {
            context.config(TestConfig.class, TestConfig.DEFAULTS);
            context.items().register(new LobbyItem(Key.key("titan:a"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(0), (player, event) -> {
            }));
        }, () -> {
        });
        RecordingModule b = new RecordingModule("b", log, context -> context.items().register(new LobbyItem(Key.key("titan:b"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(0), (player, event) -> {
        })), () -> {
        });
        ModuleRegistry registry = builder(parent).config(store).modules(a, b).build();

        Assertions.assertThrows(ItemPlacementConflictException.class, registry::enableAll);

        Assertions.assertFalse(Files.exists(file), "an item placement conflict must abort before the config file is written");
    }

    @DisplayName("A navigator slot conflict aborts enableAll() before the config file is flushed")
    @Test
    void navigatorConflictAbortsEnableAllBeforeFlush(@TempDir Path dir) {
        Path file = dir.resolve("app.json");
        Assertions.assertFalse(Files.exists(file));
        ConfigStore store = ConfigStore.open(file);
        EventNode<Event> parent = EventNode.all("test-config-navigator-conflict-flush");
        List<String> log = new ArrayList<>();
        NavigatorEntry entry = new NavigatorEntry(4, ItemStack.of(Material.FEATHER), Component.text("Survival"), "survival");
        RecordingModule a = new RecordingModule("a", log, context -> {
            context.config(TestConfig.class, TestConfig.DEFAULTS);
            context.navigator().add(entry);
        }, () -> {
        });
        RecordingModule b = new RecordingModule("b", log, context -> context.navigator().add(entry), () -> {
        });
        ModuleRegistry registry = builder(parent).config(store).modules(a, b).build();

        Assertions.assertThrows(NavigatorConflictException.class, registry::enableAll);

        Assertions.assertFalse(Files.exists(file), "a navigator slot conflict must abort before the config file is written");
    }

    @DisplayName("Calling config() after enable() has returned throws IllegalStateException")
    @Test
    void configAfterEnableReturnedThrows(@TempDir Path dir) {
        Path file = dir.resolve("app.json");
        ConfigStore store = ConfigStore.open(file);
        EventNode<Event> parent = EventNode.all("test-config-late-read");
        AtomicReference<ModuleContext> captured = new AtomicReference<>();
        RecordingModule module = new RecordingModule("late", new ArrayList<>(), captured::set, () -> {
        });
        ModuleRegistry registry = builder(parent).config(store).modules(module).build();

        registry.enableAll();
        ModuleContext context = captured.get();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> context.config(TestConfig.class, TestConfig.DEFAULTS));
        Assertions.assertTrue(thrown.getMessage().contains("late"), "the message must name the offending module");
    }
}

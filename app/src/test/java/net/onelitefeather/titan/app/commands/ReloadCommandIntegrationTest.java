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
package net.onelitefeather.titan.app.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.command.ConsoleSender;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.bootstrap.reload.ConfigDiff;
import net.onelitefeather.titan.app.bootstrap.reload.ConfigReloader;
import net.onelitefeather.titan.app.bootstrap.reload.ConfigSnapshotSource;
import net.onelitefeather.titan.app.bootstrap.reload.LiveConfig;
import net.onelitefeather.titan.app.bootstrap.reload.ModuleRestartOutcome;
import net.onelitefeather.titan.app.bootstrap.reload.ModuleRestarter;
import net.onelitefeather.titan.app.i18n.TitanTranslations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration coverage for {@code /titanreload} run from the console through Minestom's real
 * command dispatch ({@code CommandManager#execute}), against a {@link ConfigReloader} built from
 * fakes ({@link ConfigSnapshotSource}, {@link LiveConfig}, {@link ModuleRestarter}) - never the
 * real {@code Config} facade, per the "no {@code Config} mutators in tests" rule (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 5). See
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 2, and tasks.md, task
 * 5.3: the console may always run the command, and an unchanged reload restarts nothing.
 *
 * <p>Both of {@link ConfigReloader}'s executors are direct ({@link Runnable#run()} on the calling
 * thread): the fakes' snapshot is identical to the live values, so the diff is empty and
 * {@link ConfigReloader} never reaches the tick executor at all - see its own javadoc.
 */
@ExtendWith(MicrotusExtension.class)
class ReloadCommandIntegrationTest {

    @DisplayName("The reload command run from the console triggers exactly one unchanged run and restarts no module")
    @Test
    void consoleReloadWithNoChangesReportsUnchangedAndRestartsNothing(Env env) {
        Map<String, String> values = Map.of("sit.offset.y", "0.25");
        RecordingSource source = new RecordingSource(values);
        NoOpLiveConfig liveConfig = new NoOpLiveConfig(values);
        RecordingModuleRestarter restarter = new RecordingModuleRestarter();
        ConfigReloader reloader = new ConfigReloader(source, liveConfig, restarter, Runnable::run, Runnable::run);
        env.process().command().register(new ReloadCommand(reloader::reload));
        RecordingConsoleSender console = new RecordingConsoleSender();

        env.process().command().execute(console, "titanreload");

        Assertions.assertEquals(1, source.loadCalls, "exactly one reload run must have loaded a fresh snapshot");
        Assertions.assertTrue(restarter.restartCalls.isEmpty(), "an unchanged reload must not restart any module");
        Assertions.assertTrue(liveConfig.applyCalls.isEmpty(), "an unchanged reload must not apply anything to the live configuration");
        Assertions.assertEquals(1, console.sent.size(), "exactly one reply must be sent");
        TranslatableComponent reply = Assertions.assertInstanceOf(TranslatableComponent.class, console.sent.get(0));
        Assertions.assertEquals(TitanTranslations.CONFIG_RELOAD_UNCHANGED, reply.key(), "an unchanged reload must reply with the 'unchanged' key");
    }

    /**
     * A {@link ConsoleSender} that records every sent component instead of logging it - so this
     * test does not depend on the real, process-wide {@link net.kyori.adventure.translation.GlobalTranslator}
     * singleton ever having {@code TitanTranslations} registered on it (see that class's own
     * javadoc: a test must not mutate that shared singleton). {@code !(sender instanceof Player)}
     * still holds for this subclass, so the reload command's console-always-allowed branch applies
     * unchanged.
     */
    private static final class RecordingConsoleSender extends ConsoleSender {

        final List<Component> sent = new ArrayList<>();

        @Override
        public void sendMessage(Component message) {
            this.sent.add(message);
        }
    }

    private static final class RecordingSource implements ConfigSnapshotSource {

        private final Map<String, String> values;
        int loadCalls;

        RecordingSource(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public Map<String, String> load() {
            this.loadCalls++;
            return this.values;
        }
    }

    /**
     * A {@link LiveConfig} fake whose {@link #currentValues()} is fixed to the exact same values
     * the {@link RecordingSource} loads, so {@link ConfigDiff#between} is always empty for this
     * test - {@link #apply}/{@link #applyRevert} must therefore never be called; either failing
     * that assumption immediately makes the underlying bug obvious instead of silently mutating a
     * shared map.
     */
    private static final class NoOpLiveConfig implements LiveConfig {

        private final Map<String, String> values;
        final List<ConfigDiff> applyCalls = new ArrayList<>();

        NoOpLiveConfig(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public Map<String, String> currentValues() {
            return this.values;
        }

        @Override
        public void apply(ConfigDiff diff) {
            this.applyCalls.add(diff);
            throw new AssertionError("must not apply anything for an unchanged reload");
        }

        @Override
        public void applyRevert(Map<String, String> puts, Set<String> removals) {
            throw new AssertionError("must not revert anything for an unchanged reload");
        }
    }

    private static final class RecordingModuleRestarter implements ModuleRestarter {

        final List<String> restartCalls = new ArrayList<>();

        @Override
        public ModuleRestartOutcome restart(String moduleId) {
            this.restartCalls.add(moduleId);
            return new ModuleRestartOutcome.Restarted();
        }

        @Override
        public List<String> moduleOrder() {
            return List.of();
        }
    }
}

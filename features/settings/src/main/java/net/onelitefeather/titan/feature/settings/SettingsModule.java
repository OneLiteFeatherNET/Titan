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
package net.onelitefeather.titan.feature.settings;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.LevelSetting;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingValue;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.settings.SettingsStore;
import net.onelitefeather.titan.core.settings.SettingsStoreException;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a player's settings when the player configures, saves switched settings in the background,
 * and opens the menu from the hotbar item and {@code /settings}.
 *
 * <p>Nothing here waits on the store: loads and saves run on the IO executor, and a loaded value is
 * handed back to the tick thread through the scheduler. A disabled feature attaches nothing, so it
 * sends no request to Otis.
 */
@Singleton
public final class SettingsModule {

    static final int EVENT_PRIORITY = 550;

    private static final Logger LOG = LoggerFactory.getLogger(SettingsModule.class);
    private static final String ID = "settings";
    /** Ticks between two looks at the write queue: about half a second. */
    private static final int SWEEP_TICKS = 10;
    private static final Duration FLUSH_LIMIT = Duration.ofSeconds(5);

    private final EventNode<Event> titan;
    private final Telemetry telemetry;
    private final Scheduler scheduler;
    private final CommandManager commands;
    private final SettingsStore store;
    private final SettingsRegistry registry;
    private final SettingsOptions options;
    private final SettingsMessages messages;
    private final Clock clock;
    private final Executor io;
    private final SettingsTelemetry settingsTelemetry;
    private final WriteBehind writeBehind;
    private final PlayerSettingsService playerSettings;
    private final SettingsMenu menu;
    private final SettingsCommand command;
    private FeatureNode node;
    private Task sweep;

    public SettingsModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Telemetry telemetry, Scheduler scheduler, CommandManager commands, SettingsStore store, SettingsRegistry registry, SettingsOptions options, SettingsMessages messages, Clock clock, @Named(SettingsBeans.IO) Executor io) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.commands = Objects.requireNonNull(commands, "commands must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.io = Objects.requireNonNull(io, "io must not be null");
        this.settingsTelemetry = new SettingsTelemetry(telemetry);
        this.writeBehind = new WriteBehind(clock, options.writeDebounce(), options.writeRetries());
        this.playerSettings = new PlayerSettingsService(this.writeBehind, this.settingsTelemetry);
        this.menu = new SettingsMenu(this.playerSettings, registry, messages);
        this.command = new SettingsCommand(this.menu);
    }

    @PostConstruct
    void start() {
        this.messages.register();
        this.settingsTelemetry.started(this.options.persistent() ? "otis" : "memory");
        if (!this.options.enabled()) {
            return;
        }
        if (!this.options.persistent()) {
            LOG.warn("Player settings are kept in memory only: otis.url is empty, so they are lost when the lobby restarts");
        }
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(AsyncPlayerConfigurationEvent.class, event -> onConfigure(event.getPlayer())).on(PlayerDisconnectEvent.class, event -> onDisconnect(event.getPlayer()));
        this.commands.register(this.command);
        this.sweep = this.scheduler.scheduleTask(this::saveDue, TaskSchedule.tick(SWEEP_TICKS), TaskSchedule.tick(SWEEP_TICKS));
    }

    @PreDestroy
    void stop() {
        if (this.options.enabled()) {
            this.sweep.cancel();
            this.node.close();
            this.commands.unregister(this.command);
            flushAll();
        }
        this.menu.close();
        this.messages.close();
    }

    /**
     * The values of the players on this lobby; the
     * {@link net.onelitefeather.titan.core.settings.PlayerSettings} bean.
     */
    PlayerSettingsService playerSettings() {
        return this.playerSettings;
    }

    /**
     * Opens the settings menu for {@code player}; the hotbar item and {@code /settings} both call
     * this.
     */
    void open(Player player) {
        this.menu.open(player);
    }

    private void onConfigure(Player player) {
        UUID id = player.getUuid();
        long session = this.playerSettings.beginSession(id);
        this.io.execute(() -> loadAndApply(id, session));
    }

    private void onDisconnect(Player player) {
        UUID id = player.getUuid();
        this.playerSettings.endSession(id);
        submit(this.writeBehind.takeAll(id));
    }

    // Runs on the IO executor: the store may block.
    private void loadAndApply(UUID player, long session) {
        long started = this.clock.millis();
        Map<SettingKey, SettingValue> loaded = Map.of();
        String outcome = SettingsTelemetry.OK;
        try {
            loaded = this.store.load(player);
        } catch (SettingsStoreException failure) {
            outcome = failure.failure().outcome();
            LOG.warn("Could not load the settings of a player: {}", failure.failure());
        }
        this.settingsTelemetry.loaded(this.clock.millis() - started, outcome);
        Map<BooleanSetting, Boolean> flags = new HashMap<>();
        Map<LevelSetting, Integer> levels = new HashMap<>();
        loaded.forEach((key, value) -> known(key, value, flags, levels));
        this.scheduler.scheduleNextTick(() -> {
            this.playerSettings.applyLoaded(player, session, flags);
            levels.forEach((setting, level) -> this.playerSettings.applyLoadedLevel(player, session, setting, level));
        });
    }

    // A stored key the registry no longer offers is dropped: nothing reads it. A value of the wrong
    // kind or outside a level's list is dropped with a warning that names the key, never the value.
    private void known(SettingKey key, SettingValue value, Map<BooleanSetting, Boolean> flags, Map<LevelSetting, Integer> levels) {
        Optional<BooleanSetting> flag = this.registry.find(key);
        Optional<LevelSetting> level = this.registry.findLevel(key);
        if (flag.isPresent() && value instanceof SettingValue.Flag stored) {
            flags.put(flag.get(), stored.enabled());
        } else
            if (level.isPresent() && value instanceof SettingValue.Level stored && level.get().allows(stored.value())) {
                levels.put(level.get(), stored.value());
            } else if (flag.isPresent() || level.isPresent()) {
                LOG.warn("A stored value of {} does not fit its setting, the default is used", key);
            }
    }

    private void saveDue() {
        submit(this.writeBehind.takeDue());
    }

    private void submit(List<WriteBehind.Write> writes) {
        for (WriteBehind.Write write : writes) {
            this.io.execute(() -> save(write));
        }
    }

    // Runs on the IO executor: the store may block. The value is never logged, only the key.
    private void save(WriteBehind.Write write) {
        try {
            this.store.save(write.player(), write.key(), write.value());
            this.settingsTelemetry.written(SettingsTelemetry.OK);
        } catch (SettingsStoreException failure) {
            boolean again = this.writeBehind.failed(write, failure.failure().retryable());
            this.settingsTelemetry.written(again ? "retry" : "error");
            LOG.warn("Could not save setting {}: {}", write.key(), failure.failure());
        }
    }

    // Shutdown: every queued write goes out, but the wait is capped so a dead store cannot hold the stop.
    private void flushAll() {
        List<CompletableFuture<Void>> saves = new ArrayList<>();
        for (WriteBehind.Write write : this.writeBehind.takeEverything()) {
            saves.add(CompletableFuture.runAsync(() -> save(write), this.io));
        }
        try {
            CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new)).get(FLUSH_LIMIT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            LOG.warn("Some player settings were not saved before the shutdown limit");
        } catch (ExecutionException failure) {
            LOG.warn("Saving player settings failed during the shutdown", failure.getCause());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}

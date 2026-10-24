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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingChangeListener;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The values of the players on this lobby, held in memory. A session starts when a player
 * configures and ends when the player leaves; a load result is only applied to the session it was
 * started for, so a late answer for an earlier visit cannot change the current one.
 *
 * <p>Every method is synchronized: switches come from the tick thread, loads are applied there
 * too, but the configuration phase begins sessions off the tick thread.
 */
final class PlayerSettingsService implements PlayerSettings {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerSettingsService.class);

    private final WriteBehind writes;
    private final SettingsTelemetry telemetry;
    private final List<SettingChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<UUID, PlayerValues> players = new HashMap<>();
    private long sessions;

    PlayerSettingsService(WriteBehind writes, SettingsTelemetry telemetry) {
        this.writes = Objects.requireNonNull(writes, "writes must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
    }

    /**
     * Starts a session with defaults only, replacing any earlier one.
     *
     * @return the session's id; a load must hand it back to {@link #applyLoaded}
     */
    synchronized long beginSession(UUID player) {
        this.sessions++;
        this.players.put(player, new PlayerValues(this.sessions));
        return this.sessions;
    }

    /** Forgets the player's values; a load still running for the session is then discarded. */
    synchronized void endSession(UUID player) {
        this.players.remove(player);
    }

    @Override
    public synchronized boolean isEnabled(UUID player, BooleanSetting setting) {
        PlayerValues values = this.players.get(player);
        return values == null ? setting.defaultValue() : values.effective(setting);
    }

    @Override
    public void set(UUID player, BooleanSetting setting, boolean enabled) {
        boolean previous;
        synchronized (this) {
            PlayerValues values = this.players.computeIfAbsent(player, ignored -> new PlayerValues(0));
            previous = values.effective(setting);
            values.stored.put(setting.key(), new Stored(enabled, true));
            this.writes.changed(player, setting.key(), enabled);
            this.telemetry.toggled(setting.key());
        }
        notifyChanged(player, setting, previous, enabled);
    }

    @Override
    public void addChangeListener(SettingChangeListener listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    /**
     * Applies a loaded value to every setting the player has not switched during this session.
     *
     * @return {@code false} if the session has ended or been replaced, and nothing was applied
     */
    boolean applyLoaded(UUID player, long session, Map<BooleanSetting, Boolean> loaded) {
        List<Change> changes = new ArrayList<>();
        synchronized (this) {
            PlayerValues values = this.players.get(player);
            if (values == null || values.session != session) {
                return false;
            }
            for (Map.Entry<BooleanSetting, Boolean> entry : loaded.entrySet()) {
                BooleanSetting setting = entry.getKey();
                Stored current = values.stored.get(setting.key());
                if (current != null && current.touched()) {
                    continue;
                }
                boolean previous = values.effective(setting);
                values.stored.put(setting.key(), new Stored(entry.getValue(), false));
                if (previous != entry.getValue()) {
                    changes.add(new Change(player, setting, previous, entry.getValue()));
                }
            }
        }
        changes.forEach(change -> notifyChanged(change.player(), change.setting(), change.previous(), change.current()));
        return true;
    }

    private void notifyChanged(UUID player, BooleanSetting setting, boolean previous, boolean current) {
        if (previous == current) {
            return;
        }
        for (SettingChangeListener listener : this.listeners) {
            try {
                listener.changed(player, setting, previous, current);
            } catch (RuntimeException failure) {
                // One broken listener must not keep the others from hearing about the change.
                LOG.error("A setting change listener failed for {}", setting.key(), failure);
            }
        }
    }

    private record Change(UUID player, BooleanSetting setting, boolean previous, boolean current) {
    }

    /**
     * One stored value: {@code touched} marks a value the player switched, which a load keeps out
     * of.
     */
    private record Stored(boolean enabled, boolean touched) {
    }

    private static final class PlayerValues {

        private final long session;
        private final Map<SettingKey, Stored> stored = new HashMap<>();

        PlayerValues(long session) {
            this.session = session;
        }

        boolean effective(BooleanSetting setting) {
            Stored value = this.stored.get(setting.key());
            return value == null ? setting.defaultValue() : value.enabled();
        }
    }
}

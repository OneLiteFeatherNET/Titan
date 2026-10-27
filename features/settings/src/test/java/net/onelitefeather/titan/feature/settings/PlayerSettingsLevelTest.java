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

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.onelitefeather.titan.core.settings.LevelSetting;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingValue;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerSettingsLevelTest {

    private static final LevelSetting VOLUME = new LevelSetting(SettingKey.parse("titan:radio.volume"), List.of(25, 50, 75, 100), 100);

    private static PlayerSettingsService service(WriteBehind writes) {
        return new PlayerSettingsService(writes, new SettingsTelemetry(Telemetry.noop()));
    }

    private static WriteBehind writes(AdjustableClock clock) {
        return new WriteBehind(clock, Duration.ofSeconds(2), 3);
    }

    @DisplayName("Without a loaded value a level is its default")
    @Test
    void levelDefaultsWithoutAStoredValue() {
        PlayerSettingsService service = service(writes(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC)));
        UUID player = UUID.randomUUID();
        service.beginSession(player);

        Assertions.assertEquals(100, service.level(player, VOLUME), "a player without a stored level must have the default");
    }

    @DisplayName("A set level is readable at once")
    @Test
    void setLevelIsReadableAtOnce() {
        PlayerSettingsService service = service(writes(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC)));
        UUID player = UUID.randomUUID();
        service.beginSession(player);

        service.setLevel(player, VOLUME, 75);

        Assertions.assertEquals(75, service.level(player, VOLUME), "the level must read back right after it was set");
    }

    @DisplayName("Setting a level does not reach the switch listeners")
    @Test
    void setLevelDoesNotNotifySwitchListeners() {
        PlayerSettingsService service = service(writes(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC)));
        List<String> heard = new ArrayList<>();
        service.addChangeListener((player, setting, previous, current) -> heard.add(setting.key().toString()));
        UUID player = UUID.randomUUID();
        service.beginSession(player);

        service.setLevel(player, VOLUME, 50);

        Assertions.assertTrue(heard.isEmpty(), "a level change must not be heard as a switch change");
    }

    @DisplayName("A level outside the list is refused")
    @Test
    void levelOutsideTheListIsRefused() {
        PlayerSettingsService service = service(writes(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC)));
        UUID player = UUID.randomUUID();
        service.beginSession(player);

        Assertions.assertThrows(IllegalArgumentException.class, () -> service.setLevel(player, VOLUME, 60), "a level that is not in the list must be refused");
    }

    @DisplayName("A set level is queued for saving as a level value")
    @Test
    void setLevelIsQueuedAsALevel() {
        AdjustableClock clock = new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC);
        WriteBehind writes = writes(clock);
        PlayerSettingsService service = service(writes);
        UUID player = UUID.randomUUID();
        service.beginSession(player);

        service.setLevel(player, VOLUME, 25);
        clock.advance(Duration.ofSeconds(3));
        List<WriteBehind.Write> due = writes.takeDue();

        Assertions.assertEquals(List.of(new SettingValue.Level(25)), due.stream().map(WriteBehind.Write::value).toList(), "the queued write must carry the level");
    }

    @DisplayName("A loaded level is applied when the player has not set it")
    @Test
    void loadedLevelIsApplied() {
        PlayerSettingsService service = service(writes(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC)));
        UUID player = UUID.randomUUID();
        long session = service.beginSession(player);

        boolean applied = service.applyLoadedLevel(player, session, VOLUME, 50);

        Assertions.assertTrue(applied, "the load must apply to the running session");
        Assertions.assertEquals(50, service.level(player, VOLUME), "the loaded level must be the value in effect");
    }

    @DisplayName("A level the player set before the load finished wins over the loaded one")
    @Test
    void localLevelWinsOverALoadedOne() {
        PlayerSettingsService service = service(writes(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC)));
        UUID player = UUID.randomUUID();
        long session = service.beginSession(player);
        service.setLevel(player, VOLUME, 25);

        service.applyLoadedLevel(player, session, VOLUME, 50);

        Assertions.assertEquals(25, service.level(player, VOLUME), "a level set before the load must keep its value");
    }

    @DisplayName("A load for a session that has ended changes nothing")
    @Test
    void loadForAnEndedSessionIsDiscarded() {
        PlayerSettingsService service = service(writes(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC)));
        UUID player = UUID.randomUUID();
        long session = service.beginSession(player);
        service.endSession(player);

        Assertions.assertFalse(service.applyLoadedLevel(player, session, VOLUME, 50), "a load for a session that has ended must not apply");
    }
}

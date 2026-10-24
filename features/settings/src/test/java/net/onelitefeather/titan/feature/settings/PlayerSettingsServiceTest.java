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
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerSettingsServiceTest {

    private static final BooleanSetting TAG = SettingsFixtures.TAG;
    private static final BooleanSetting THROW = SettingsFixtures.THROW;

    private static PlayerSettingsService service(AdjustableClock clock) {
        WriteBehind writes = new WriteBehind(clock, Duration.ofSeconds(2), 3);
        return new PlayerSettingsService(writes, new SettingsTelemetry(Telemetry.noop()));
    }

    @DisplayName("Without a loaded value an opt-in setting is off and an opt-out setting is on")
    @Test
    void defaultsApplyWithoutAStoredValue() {
        PlayerSettingsService service = service(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC));
        UUID player = UUID.randomUUID();
        service.beginSession(player);
        Assertions.assertFalse(service.isEnabled(player, TAG), "an opt-in setting must be off until switched on");
        Assertions.assertTrue(service.isEnabled(player, THROW), "an opt-out setting must be on until switched off");
    }

    @DisplayName("A switch takes effect at once, before anything is saved")
    @Test
    void switchTakesEffectAtOnce() {
        PlayerSettingsService service = service(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC));
        UUID player = UUID.randomUUID();
        service.beginSession(player);
        service.set(player, TAG, true);
        Assertions.assertTrue(service.isEnabled(player, TAG), "the switched value must be readable right after the switch");
    }

    @DisplayName("A switch reports the previous and the current value to the listeners")
    @Test
    void listenersHearThePreviousAndTheCurrentValue() {
        PlayerSettingsService service = service(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC));
        List<String> heard = new ArrayList<>();
        service.addChangeListener((player, setting, previous, current) -> heard.add(setting.key() + " " + previous + "->" + current));
        UUID player = UUID.randomUUID();
        service.beginSession(player);
        service.set(player, TAG, true);
        Assertions.assertEquals(List.of("titan:tag.enabled false->true"), heard, "the listener must hear the switch with both values");
    }

    @DisplayName("A switch to the value the player already has notifies nobody")
    @Test
    void sameValueNotifiesNobody() {
        PlayerSettingsService service = service(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC));
        List<String> heard = new ArrayList<>();
        service.addChangeListener((player, setting, previous, current) -> heard.add(setting.key().toString()));
        UUID player = UUID.randomUUID();
        service.beginSession(player);
        service.set(player, TAG, false);
        Assertions.assertTrue(heard.isEmpty(), "a value that does not change must not notify");
    }

    @DisplayName("A switch leaves the other settings of the same player unchanged")
    @Test
    void otherSettingsOfThePlayerStayUnchanged() {
        PlayerSettingsService service = service(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC));
        UUID player = UUID.randomUUID();
        service.beginSession(player);
        service.set(player, TAG, true);
        Assertions.assertTrue(service.isEnabled(player, THROW), "switching the tag must not switch the throw target");
    }

    @DisplayName("A switch leaves the settings of other players unchanged")
    @Test
    void otherPlayersStayUnchanged() {
        PlayerSettingsService service = service(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC));
        UUID switched = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        service.beginSession(switched);
        service.beginSession(other);
        service.set(switched, TAG, true);
        Assertions.assertFalse(service.isEnabled(other, TAG), "another player must keep the default");
    }
}

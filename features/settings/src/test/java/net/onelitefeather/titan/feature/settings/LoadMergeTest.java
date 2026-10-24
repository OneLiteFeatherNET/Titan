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
import java.util.Map;
import java.util.UUID;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoadMergeTest {

    private static final BooleanSetting TAG = SettingsFixtures.TAG;
    private static final BooleanSetting THROW = SettingsFixtures.THROW;

    private static PlayerSettingsService service() {
        AdjustableClock clock = new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC);
        return new PlayerSettingsService(new WriteBehind(clock, Duration.ofSeconds(2), 3), new SettingsTelemetry(Telemetry.noop()));
    }

    @DisplayName("A loaded value is applied to a setting the player has not switched")
    @Test
    void loadedValueAppliesWhenNotSwitched() {
        PlayerSettingsService service = service();
        UUID player = UUID.randomUUID();
        long session = service.beginSession(player);
        service.applyLoaded(player, session, Map.of(TAG, true));
        Assertions.assertTrue(service.isEnabled(player, TAG), "the stored true must be applied");
    }

    @DisplayName("A setting switched during the load keeps the local value")
    @Test
    void switchedSettingKeepsItsLocalValue() {
        PlayerSettingsService service = service();
        UUID player = UUID.randomUUID();
        long session = service.beginSession(player);
        service.set(player, THROW, false);
        service.applyLoaded(player, session, Map.of(THROW, true));
        Assertions.assertFalse(service.isEnabled(player, THROW), "the stored value must not revert a switch made during the load");
    }

    @DisplayName("Other settings still take the stored value when one was switched")
    @Test
    void otherSettingsTakeTheStoredValue() {
        PlayerSettingsService service = service();
        UUID player = UUID.randomUUID();
        long session = service.beginSession(player);
        service.set(player, THROW, false);
        service.applyLoaded(player, session, Map.of(TAG, true));
        Assertions.assertTrue(service.isEnabled(player, TAG), "a setting that was not switched must take the loaded value");
    }

    @DisplayName("A load for a player who has left is discarded")
    @Test
    void loadAfterLeavingIsDiscarded() {
        PlayerSettingsService service = service();
        UUID player = UUID.randomUUID();
        long session = service.beginSession(player);
        service.endSession(player);
        boolean applied = service.applyLoaded(player, session, Map.of(TAG, true));
        Assertions.assertFalse(applied, "a load for a session that has ended must not be applied");
        Assertions.assertFalse(service.isEnabled(player, TAG), "the default must hold after the player left");
    }

    @DisplayName("A load from an earlier visit is discarded once a new visit has started")
    @Test
    void loadFromEarlierVisitIsDiscarded() {
        PlayerSettingsService service = service();
        UUID player = UUID.randomUUID();
        long earlier = service.beginSession(player);
        service.beginSession(player);
        boolean applied = service.applyLoaded(player, earlier, Map.of(TAG, true));
        Assertions.assertFalse(applied, "a load for an older session must not change the current one");
    }

    @DisplayName("A change caused by a load notifies the listeners")
    @Test
    void loadedChangeNotifiesListeners() {
        PlayerSettingsService service = service();
        List<String> heard = new ArrayList<>();
        service.addChangeListener((player, setting, previous, current) -> heard.add(previous + "->" + current));
        UUID player = UUID.randomUUID();
        long session = service.beginSession(player);
        service.applyLoaded(player, session, Map.of(TAG, true));
        Assertions.assertEquals(List.of("false->true"), heard, "a loaded value that differs from the default must be announced");
    }
}

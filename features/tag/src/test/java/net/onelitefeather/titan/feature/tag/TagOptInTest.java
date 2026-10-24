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
package net.onelitefeather.titan.feature.tag;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagOptInTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @DisplayName("The tag setting is the titan:tag.enabled opt-in, off until the player switches it on")
    @Test
    void settingIsAnOptInWithTheAgreedKey() {
        Assertions.assertEquals("titan:tag.enabled", TagSettings.ENABLED.key().toString(), "the key is fixed by the change, other modules and saved data rely on it");
        Assertions.assertFalse(TagSettings.ENABLED.defaultValue(), "a player who never chose must not be part of the game");
        Assertions.assertFalse(new FakePlayerSettings().isEnabled(PLAYER, TagSettings.ENABLED), "an unset player must read as not playing");
    }

    @DisplayName("Toggling a player who is off switches the setting on")
    @Test
    void toggleSwitchesOn() {
        FakePlayerSettings settings = new FakePlayerSettings();

        TagOptIn.Toggle result = new TagOptIn(settings).toggle(PLAYER);

        Assertions.assertEquals(TagOptIn.Toggle.SWITCHED_ON, result, "the command must report that the player now plays");
        Assertions.assertTrue(settings.isEnabled(PLAYER, TagSettings.ENABLED), "the shared setting must hold the new value");
    }

    @DisplayName("Toggling a player who is on switches the setting off")
    @Test
    void toggleSwitchesOff() {
        FakePlayerSettings settings = new FakePlayerSettings();
        settings.set(PLAYER, TagSettings.ENABLED, true);

        TagOptIn.Toggle result = new TagOptIn(settings).toggle(PLAYER);

        Assertions.assertEquals(TagOptIn.Toggle.SWITCHED_OFF, result, "the command must report that the player stepped out");
        Assertions.assertFalse(settings.isEnabled(PLAYER, TagSettings.ENABLED), "the shared setting must hold the new value");
    }

    @DisplayName("Toggling changes only the calling player")
    @Test
    void toggleLeavesOtherPlayersAlone() {
        FakePlayerSettings settings = new FakePlayerSettings();

        new TagOptIn(settings).toggle(PLAYER);

        Assertions.assertFalse(settings.isEnabled(OTHER, TagSettings.ENABLED), "another player's choice must not change");
    }

    @DisplayName("An unavailable settings store keeps the value and reports it")
    @Test
    void unavailableStoreKeepsTheValue() {
        FakePlayerSettings settings = new FakePlayerSettings();
        settings.makeUnavailable();

        TagOptIn.Toggle result = new TagOptIn(settings).toggle(PLAYER);

        Assertions.assertEquals(TagOptIn.Toggle.UNAVAILABLE, result, "the player must hear that the switch did not happen");
        Assertions.assertFalse(settings.isEnabled(PLAYER, TagSettings.ENABLED), "no local state may be invented while the store is down");
    }
}

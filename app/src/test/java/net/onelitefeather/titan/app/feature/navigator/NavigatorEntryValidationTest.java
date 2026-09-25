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
package net.onelitefeather.titan.app.feature.navigator;

import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit coverage for {@link NavigatorEntryValidation#buildEntry}: the checks moved out of
 * {@link NavigatorConfig.Entry}'s compact constructor (see {@code openspec/changes/
 * avaje-config-facade/design.md}, decision 6), now reporting the full
 * {@code navigator.entries.<name>.<field>} key instead of just {@code entries}.
 *
 * <p>{@link net.minestom.testing.extension.MicrotusExtension} is only needed because
 * {@link net.minestom.server.item.Material#fromKey(String)} resolves against Minestom's registry
 * data - the same reason {@code NavigatorConfigTest} needed it.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorEntryValidationTest {

    @DisplayName("A valid entry is built as given")
    @Test
    void validEntryIsBuiltAsGiven() {
        NavigatorConfig.Entry entry = NavigatorEntryValidation.buildEntry("survival", 4, "minecraft:grass_block", "<!i><green>Survival", "Survival", null);

        Assertions.assertEquals(new NavigatorConfig.Entry(4, "minecraft:grass_block", "<!i><green>Survival", "Survival"), entry);
    }

    @DisplayName("A valid entry with a feature gate is built as given")
    @Test
    void validEntryWithFeatureIsBuiltAsGiven() {
        NavigatorConfig.Entry entry = NavigatorEntryValidation.buildEntry("slender", 5, "minecraft:enderman_spawn_egg", "<!i><gray>Slender", "cygnus", "NAVIGATOR_SLENDER");

        Assertions.assertEquals("NAVIGATOR_SLENDER", entry.feature());
    }

    @DisplayName("A negative slot is rejected, naming the full key")
    @Test
    void negativeSlotIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> NavigatorEntryValidation.buildEntry("survival", -1, "minecraft:feather", "<white>Test", "Test", null));

        Assertions.assertEquals("navigator.entries.survival.slot", thrown.field());
        Assertions.assertTrue(thrown.reason().contains("-1"), "the reason must name the offending slot");
    }

    @DisplayName("A slot past 8 (outside CHEST_1_ROW) is rejected, naming the full key")
    @Test
    void slotPastEightIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> NavigatorEntryValidation.buildEntry("survival", 9, "minecraft:feather", "<white>Test", "Test", null));

        Assertions.assertEquals("navigator.entries.survival.slot", thrown.field());
    }

    @DisplayName("An unknown material is rejected, naming the full key")
    @Test
    void unknownMaterialIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> NavigatorEntryValidation.buildEntry("survival", 0, "minecraft:not_a_real_material", "<white>Test", "Test", null));

        Assertions.assertEquals("navigator.entries.survival.icon", thrown.field());
        Assertions.assertTrue(thrown.reason().contains("not_a_real_material"), "the reason must name the offending icon");
    }

    @DisplayName("A blank destination is rejected, naming the full key")
    @Test
    void blankDestinationIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> NavigatorEntryValidation.buildEntry("survival", 0, "minecraft:feather", "<white>Test", "   ", null));

        Assertions.assertEquals("navigator.entries.survival.destination", thrown.field());
    }

    @DisplayName("A null name is rejected")
    @Test
    void nullNameIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> NavigatorEntryValidation.buildEntry(null, 0, "minecraft:feather", "<white>Test", "Test", null));
    }

    @DisplayName("A null icon is rejected")
    @Test
    void nullIconIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> NavigatorEntryValidation.buildEntry("survival", 0, null, "<white>Test", "Test", null));
    }

    @DisplayName("A null display name is rejected")
    @Test
    void nullDisplayNameIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> NavigatorEntryValidation.buildEntry("survival", 0, "minecraft:feather", null, "Test", null));
    }

    @DisplayName("A null destination is rejected")
    @Test
    void nullDestinationIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> NavigatorEntryValidation.buildEntry("survival", 0, "minecraft:feather", "<white>Test", null, null));
    }
}

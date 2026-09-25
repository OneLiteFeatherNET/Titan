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

import java.util.List;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit coverage for {@link NavigatorConfig} and {@link NavigatorConfig.Entry}: the defaults
 * reproduce today's four navigator entries exactly, and an entry's compact constructor rejects a
 * slot outside {@code 0}-{@code 8}, an unknown material and a blank destination.
 *
 * <p>{@link net.minestom.testing.extension.MicrotusExtension} is only needed because
 * {@link net.minestom.server.item.Material#fromKey(String)} resolves against Minestom's registry
 * data - the same reason {@code EquipPlanTest} uses it for plain {@code ItemStack} construction.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorConfigTest {

    @DisplayName("DEFAULTS reproduces today's four navigator entries exactly, Slender gated behind NAVIGATOR_SLENDER")
    @Test
    void defaultsReproduceTodaysFourEntries() {
        Assertions.assertEquals("<yellow>Navigator", NavigatorConfig.DEFAULTS.title());
        Assertions.assertEquals(List.of(new NavigatorConfig.Entry(0, "minecraft:elytra", "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>", "ElytraRace"), new NavigatorConfig.Entry(4, "minecraft:grass_block", "<!i><green>Survival", "Survival"), new NavigatorConfig.Entry(5, "minecraft:enderman_spawn_egg", "<!i><gradient:#616161:#e80000c>Slender</gradient>", "cygnus", "NAVIGATOR_SLENDER"), new NavigatorConfig.Entry(8, "minecraft:wooden_axe", "<!i><rainbow>Creative</rainbow>", "MemberBuild")), NavigatorConfig.DEFAULTS.entries());
    }

    @DisplayName("A 4-arg entry has no feature gate")
    @Test
    void fourArgEntryHasNoFeature() {
        NavigatorConfig.Entry entry = new NavigatorConfig.Entry(0, "minecraft:feather", "<white>Test", "Test");

        Assertions.assertNull(entry.feature());
    }

    @DisplayName("A null title is rejected")
    @Test
    void nullTitleIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorConfig(null, List.of()));
    }

    @DisplayName("A null entries list is rejected")
    @Test
    void nullEntriesIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorConfig("<yellow>Navigator", null));
    }

    @DisplayName("A negative slot is rejected")
    @Test
    void negativeSlotIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> new NavigatorConfig.Entry(-1, "minecraft:feather", "<white>Test", "Test"));

        Assertions.assertEquals("entries", thrown.field());
        Assertions.assertTrue(thrown.reason().contains("-1"), "the reason must name the offending slot");
    }

    @DisplayName("A slot past 8 (outside CHEST_1_ROW) is rejected")
    @Test
    void slotPastEightIsRejected() {
        Assertions.assertThrows(ConfigException.class, () -> new NavigatorConfig.Entry(9, "minecraft:feather", "<white>Test", "Test"));
    }

    @DisplayName("An unknown material is rejected")
    @Test
    void unknownMaterialIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> new NavigatorConfig.Entry(0, "minecraft:not_a_real_material", "<white>Test", "Test"));

        Assertions.assertTrue(thrown.reason().contains("not_a_real_material"), "the reason must name the offending icon");
    }

    @DisplayName("A blank destination is rejected")
    @Test
    void blankDestinationIsRejected() {
        Assertions.assertThrows(ConfigException.class, () -> new NavigatorConfig.Entry(0, "minecraft:feather", "<white>Test", "   "));
    }

    @DisplayName("A null icon is rejected")
    @Test
    void nullIconIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorConfig.Entry(0, null, "<white>Test", "Test"));
    }

    @DisplayName("A null display name is rejected")
    @Test
    void nullDisplayNameIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorConfig.Entry(0, "minecraft:feather", null, "Test"));
    }

    @DisplayName("A null destination is rejected")
    @Test
    void nullDestinationIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorConfig.Entry(0, "minecraft:feather", "<white>Test", null));
    }
}

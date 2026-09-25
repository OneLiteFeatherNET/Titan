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

import io.avaje.config.Configuration;
import java.util.Map;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.ConfigSections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit coverage for {@link NavigatorConfig} and {@link NavigatorConfig.Entry}: the defaults
 * reproduce today's four navigator entries exactly, keyed by name. The checks an entry's compact
 * constructor performs - slot {@code 0}-{@code 8}, a known material, a non-blank destination - are
 * covered by {@link NavigatorEntryValidationTest} instead, against the same {@code require*}
 * functions the constructor now delegates to (see {@code openspec/changes/avaje-config-facade/
 * design.md}, decision 6). Also covers the {@code lobby-navigator} spec scenarios "Profil ändert
 * ein
 * einzelnes Ziel" and "Zusätzliches Ziel per Konfiguration" through {@link ConfigSections}, built
 * from a {@link Configuration} over a plain {@link Map} (see {@code design.md} decision 1's spike
 * result) - the map stands in for a profile or override source setting just one key.
 *
 * <p>{@link net.minestom.testing.extension.MicrotusExtension} is only needed because
 * {@link net.minestom.server.item.Material#fromKey(String)} resolves against Minestom's registry
 * data - the same reason {@code EquipPlanTest} uses it for plain {@code ItemStack} construction.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorConfigTest {

    @DisplayName("DEFAULTS reproduces today's four navigator entries exactly, keyed by name, Slender gated behind NAVIGATOR_SLENDER")
    @Test
    void defaultsReproduceTodaysFourEntries() {
        Assertions.assertEquals("<yellow>Navigator", NavigatorConfig.DEFAULTS.title());
        Assertions.assertEquals(Map.of("elytrarace", new NavigatorConfig.Entry(0, "minecraft:elytra", "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>", "ElytraRace"), "survival", new NavigatorConfig.Entry(4, "minecraft:grass_block", "<!i><green>Survival", "Survival"), "slender", new NavigatorConfig.Entry(5, "minecraft:enderman_spawn_egg", "<!i><gradient:#616161:#e80000c>Slender</gradient>", "cygnus", "NAVIGATOR_SLENDER"), "creative", new NavigatorConfig.Entry(8, "minecraft:wooden_axe", "<!i><rainbow>Creative</rainbow>", "MemberBuild")), NavigatorConfig.DEFAULTS.entries());
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
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorConfig(null, Map.of()));
    }

    @DisplayName("A null entries map is rejected")
    @Test
    void nullEntriesIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorConfig("<yellow>Navigator", null));
    }

    @DisplayName("A profile-like override changes a single entry's destination, leaving the others at their defaults")
    @Test
    void profileOverrideChangesASingleEntry() {
        Configuration configuration = Configuration.builder().putAll(Map.of("navigator.entries.survival.destination", "survival-v2")).build();
        ConfigSections sections = new ConfigSections(configuration);

        NavigatorConfig config = sections.section("navigator", NavigatorConfig.class, NavigatorConfig.DEFAULTS);

        Assertions.assertEquals("survival-v2", config.entries().get("survival").destination(), "the overridden entry's destination must come from the override");
        Assertions.assertEquals(NavigatorConfig.DEFAULTS.entries().get("elytrarace"), config.entries().get("elytrarace"), "an entry not mentioned in the override must keep its default");
        Assertions.assertEquals(NavigatorConfig.DEFAULTS.entries().get("slender"), config.entries().get("slender"), "an entry not mentioned in the override must keep its default");
        Assertions.assertEquals(NavigatorConfig.DEFAULTS.entries().get("creative"), config.entries().get("creative"), "an entry not mentioned in the override must keep its default");
    }

    @DisplayName("An additional target (Parkour on slot 2) can be added via configuration, alongside the defaults")
    @Test
    void additionalTargetIsAddedAlongsideDefaults() {
        Configuration configuration = Configuration.builder().putAll(Map.of("navigator.entries.parkour.slot", "2", "navigator.entries.parkour.icon", "minecraft:diamond_pickaxe", "navigator.entries.parkour.displayName", "<green>Parkour", "navigator.entries.parkour.destination", "Parkour")).build();
        ConfigSections sections = new ConfigSections(configuration);

        NavigatorConfig config = sections.section("navigator", NavigatorConfig.class, NavigatorConfig.DEFAULTS);

        Assertions.assertEquals(5, config.entries().size(), "the four defaults plus the new parkour entry");
        Assertions.assertEquals(2, config.entries().get("parkour").slot());
        Assertions.assertEquals("Parkour", config.entries().get("parkour").destination());
        Assertions.assertEquals(NavigatorConfig.DEFAULTS.entries().get("elytrarace"), config.entries().get("elytrarace"), "the default entries must be untouched");
    }
}

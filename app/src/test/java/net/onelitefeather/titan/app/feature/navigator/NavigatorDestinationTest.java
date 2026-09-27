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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit coverage for {@link Destination}: no {@code Env}, no Aves, no configuration - just the
 * enum's fixed slots and the pure {@link Destination#visible(net.onelitefeather.titan.common.
 * feature.FeatureFlags)} function.
 *
 * <p>Pairwise-distinct slots replaces the old start-up conflict check the platform-wide entry
 * registry used to run: a duplicate slot is now a programming error caught here, not at start-up
 * (see {@code openspec/changes/navigator-entries-in-code/design.md}, decision 2).
 */
class NavigatorDestinationTest {

    @DisplayName("Every destination occupies the slot the lobby-navigator spec fixes for it")
    @Test
    void destinationsOccupyTheirFixedSlots() {
        Assertions.assertEquals(0, Destination.ELYTRA_RACE.slot());
        Assertions.assertEquals(4, Destination.SURVIVAL.slot());
        Assertions.assertEquals(5, Destination.SLENDER.slot());
        Assertions.assertEquals(8, Destination.CREATIVE.slot());
    }

    @DisplayName("No two destinations share a slot")
    @Test
    void destinationsHavePairwiseDistinctSlots() {
        List<Integer> slots = Arrays.stream(Destination.values()).map(Destination::slot).toList();

        Set<Integer> distinctSlots = new HashSet<>(slots);

        Assertions.assertEquals(slots.size(), distinctSlots.size(), "every destination must occupy its own slot, but found duplicates in " + slots);
    }

    @DisplayName("visible() lists every destination except Slender while NAVIGATOR_SLENDER is off")
    @Test
    void visibleExcludesSlenderWhileItsFlagIsOff() {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", false);

        List<Destination> visible = Destination.visible(flags);

        Assertions.assertFalse(visible.contains(Destination.SLENDER), "Slender must not be visible while NAVIGATOR_SLENDER is off");
        Assertions.assertTrue(visible.containsAll(List.of(Destination.ELYTRA_RACE, Destination.SURVIVAL, Destination.CREATIVE)), "the ungated destinations must stay visible regardless of the flag");
    }

    @DisplayName("visible() lists every destination, including Slender, while NAVIGATOR_SLENDER is on")
    @Test
    void visibleIncludesSlenderWhileItsFlagIsOn() {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true);

        List<Destination> visible = Destination.visible(flags);

        Assertions.assertEquals(Set.copyOf(Arrays.asList(Destination.values())), Set.copyOf(visible), "every destination must be visible while NAVIGATOR_SLENDER is on, was: " + visible.stream().map(Enum::name).collect(Collectors.joining(", ")));
    }
}

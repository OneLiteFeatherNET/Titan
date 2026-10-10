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
package net.onelitefeather.titan.feature.navigator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.component.DataComponents;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Plain unit coverage for {@link Destination}: no Aves, no configuration - just the enum's fixed
 * slots and the pure {@code Destination.visible} function. Building an item needs Minestom's
 * registries, which is why the name test takes an {@code Env}.
 *
 * <p>Pairwise-distinct slots are checked here rather than at start-up: a duplicate slot is a
 * programming error caught by this test.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorDestinationTest {

    @DisplayName("Every destination occupies the slot the lobby-navigator spec fixes for it")
    @Test
    void destinationsOccupyTheirFixedSlots() {
        Assertions.assertEquals(0, Destination.ELYTRA_RACE.slot());
        Assertions.assertEquals(4, Destination.SURVIVAL.slot());
        Assertions.assertEquals(5, Destination.SLENDER.slot());
        Assertions.assertEquals(7, Destination.BUILD.slot());
        Assertions.assertEquals(8, Destination.CREATIVE.slot());
    }

    @DisplayName("Build forwards to the Build task, has no feature flag and needs titan.navigator.buildserver")
    @Test
    void buildForwardsToTheBuildTaskBehindItsPermission() {
        Assertions.assertEquals("Build", Destination.BUILD.task());
        Assertions.assertNull(Destination.BUILD.feature(), "Build must not sit behind a feature flag");
        Assertions.assertEquals("titan.navigator.buildserver", Destination.BUILD.permission());
    }

    @DisplayName("Only Build needs a permission")
    @Test
    void onlyBuildNeedsAPermission() {
        List<Destination> permissioned = Arrays.stream(Destination.values()).filter(destination -> destination.permission() != null).toList();

        Assertions.assertEquals(List.of(Destination.BUILD), permissioned);
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
        FakeFeatureFlags flags = new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, false);

        List<Destination> visible = Destination.visible(flags, false);

        Assertions.assertFalse(visible.contains(Destination.SLENDER), "Slender must not be visible while NAVIGATOR_SLENDER is off");
        Assertions.assertTrue(visible.containsAll(List.of(Destination.ELYTRA_RACE, Destination.SURVIVAL, Destination.CREATIVE)), "the ungated destinations must stay visible regardless of the flag");
    }

    @DisplayName("visible() without permissioned destinations never lists Build")
    @Test
    void visibleWithoutPermissionedNeverListsBuild() {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, true);

        Assertions.assertFalse(Destination.visible(flags, false).contains(Destination.BUILD), "the public menu must never contain Build");
    }

    @DisplayName("visible() with permissioned destinations lists Build")
    @Test
    void visibleWithPermissionedListsBuild() {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, false);

        Assertions.assertTrue(Destination.visible(flags, true).contains(Destination.BUILD), "the team menu must contain Build");
    }

    @DisplayName("visible() lists every destination, including Slender, while NAVIGATOR_SLENDER is on")
    @Test
    void visibleIncludesSlenderWhileItsFlagIsOn() {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, true);

        List<Destination> visible = Destination.visible(flags, true);

        Assertions.assertEquals(Set.copyOf(Arrays.asList(Destination.values())), Set.copyOf(visible), "every destination must be visible while NAVIGATOR_SLENDER is on, was: " + visible.stream().map(Enum::name).collect(Collectors.joining(", ")));
    }

    @DisplayName("Slender's name fades from #616161 to #e80000 across its letters")
    @Test
    void slenderNameFadesFromGrayToDeepRed(Env env) {
        List<TextColor> letterColors = new ArrayList<>();
        collectLetterColors(Destination.SLENDER.item().get(DataComponents.CUSTOM_NAME), null, letterColors);

        Assertions.assertEquals(TextColor.color(0x616161), letterColors.getFirst(), "the first letter of Slender must be #616161");
        Assertions.assertEquals(TextColor.color(0xe80000), letterColors.getLast(), "the last letter of Slender must be #e80000, not a value cut off from an invalid hex literal");
    }

    private static void collectLetterColors(Component component, TextColor inherited, List<TextColor> sink) {
        TextColor color = component.color() != null ? component.color() : inherited;
        if (component instanceof TextComponent text) {
            text.content().chars().forEach(letter -> sink.add(color));
        }
        component.children().forEach(child -> collectLetterColors(child, color, sink));
    }
}

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
import net.minestom.server.item.Material;
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
        Assertions.assertEquals(11, Destination.ELYTRA_RACE.slot());
        Assertions.assertEquals(20, Destination.SURVIVAL.slot());
        Assertions.assertEquals(15, Destination.SLENDER.slot());
        Assertions.assertEquals(24, Destination.BUILD.slot());
        Assertions.assertEquals(22, Destination.CREATIVE.slot());
        Assertions.assertEquals(13, Destination.BOUNCE.slot());
    }

    @DisplayName("Bounce forwards to the Bounce task and needs titan.navigator.bounce")
    @Test
    void bounceForwardsToTheBounceTaskBehindItsPermission() {
        Assertions.assertEquals("Bounce", Destination.BOUNCE.task());
        Assertions.assertEquals("titan.navigator.bounce", Destination.BOUNCE.permission(), "Bounce must be gated by its permission");
    }

    @DisplayName("Bounce is shown with the slime ball icon")
    @Test
    void bounceUsesTheSlimeBallIcon(Env env) {
        Assertions.assertEquals(Material.SLIME_BALL, Destination.BOUNCE.item(List.of(Component.empty())).material());
    }

    @DisplayName("Build forwards to the Team-Build task and needs titan.navigator.buildserver")
    @Test
    void buildForwardsToTheBuildTaskBehindItsPermission() {
        Assertions.assertEquals("Team-Build", Destination.BUILD.task());
        Assertions.assertEquals("titan.navigator.buildserver", Destination.BUILD.permission());
    }

    @DisplayName("Slender forwards to the live Slender task and needs titan.navigator.slender")
    @Test
    void slenderForwardsToTheSlenderTaskBehindItsPermission() {
        Assertions.assertEquals("Slender", Destination.SLENDER.task());
        Assertions.assertEquals("titan.navigator.slender", Destination.SLENDER.permission(), "Slender must be gated by its permission, not by a feature flag");
    }

    @DisplayName("Slender (Staging) forwards to the Slender-Staging task and needs cygnus.staging.join")
    @Test
    void slenderStagingForwardsToTheStagingTaskBehindTheProxyPermission() {
        Assertions.assertEquals("Slender-Staging", Destination.SLENDER_STAGING.task());
        Assertions.assertEquals("cygnus.staging.join", Destination.SLENDER_STAGING.permission(), "the staging entry shares the permission the proxy checks");
    }

    @DisplayName("Slender (Staging) is shown with the structure block icon")
    @Test
    void slenderStagingUsesTheStructureBlockIcon(Env env) {
        Assertions.assertEquals(Material.STRUCTURE_BLOCK, Destination.SLENDER_STAGING.item(List.of(Component.empty())).material());
    }

    @DisplayName("Only Slender, Slender (Staging), Bounce and Build need a permission")
    @Test
    void onlyTheGatedDestinationsNeedAPermission() {
        Set<Destination> permissioned = Arrays.stream(Destination.values()).filter(destination -> destination.permission() != null).collect(Collectors.toSet());

        Assertions.assertEquals(Set.of(Destination.SLENDER, Destination.SLENDER_STAGING, Destination.BOUNCE, Destination.BUILD), permissioned);
    }

    @DisplayName("Slender (Staging) is visible only with cygnus.staging.join, not with the Slender grant")
    @Test
    void slenderStagingIsVisibleOnlyWithItsOwnGrant() {
        Assertions.assertTrue(Destination.visible(Set.of(Destination.SLENDER_STAGING)).contains(Destination.SLENDER_STAGING), "the grant shows the staging entry");
        Assertions.assertFalse(Destination.visible(Set.of(Destination.SLENDER)).contains(Destination.SLENDER_STAGING), "the Slender grant alone must not show the staging entry");
        Assertions.assertFalse(Destination.visible(Set.of()).contains(Destination.SLENDER_STAGING), "without the grant the staging entry is hidden");
    }

    @DisplayName("No two destinations share a slot")
    @Test
    void destinationsHavePairwiseDistinctSlots() {
        List<Integer> slots = Arrays.stream(Destination.values()).map(Destination::slot).toList();

        Set<Integer> distinctSlots = new HashSet<>(slots);

        Assertions.assertEquals(slots.size(), distinctSlots.size(), "every destination must occupy its own slot, but found duplicates in " + slots);
    }

    @DisplayName("visible() without grants lists neither Slender nor Build")
    @Test
    void visibleWithoutGrantsListsNeitherPermissionedDestination() {
        List<Destination> visible = Destination.visible(Set.of());

        Assertions.assertTrue(visible.containsAll(List.of(Destination.ELYTRA_RACE, Destination.SURVIVAL, Destination.CREATIVE)), "the ungated destinations must stay visible");
        Assertions.assertFalse(visible.contains(Destination.SLENDER), "Slender must not be visible without titan.navigator.slender");
        Assertions.assertFalse(visible.contains(Destination.BUILD), "Build must not be visible without titan.navigator.buildserver");
    }

    @DisplayName("visible() with only the Slender grant lists Slender and not Build")
    @Test
    void visibleWithSlenderGrantListsSlenderOnly() {
        List<Destination> visible = Destination.visible(Set.of(Destination.SLENDER));

        Assertions.assertTrue(visible.contains(Destination.SLENDER), "Slender must be visible with titan.navigator.slender");
        Assertions.assertFalse(visible.contains(Destination.BUILD), "Build must stay hidden without its own grant");
    }

    @DisplayName("visible() lists every destination once both permissioned destinations are granted")
    @Test
    void visibleWithAllGrantsListsEveryDestination() {
        List<Destination> visible = Destination.visible(Set.copyOf(Destination.permissioned()));

        Assertions.assertEquals(Set.copyOf(Arrays.asList(Destination.values())), Set.copyOf(visible), "every destination must be visible with both grants, was: " + visible.stream().map(Enum::name).collect(Collectors.joining(", ")));
    }

    @DisplayName("visible() without the Bounce grant hides Bounce, and with it shows Bounce")
    @Test
    void bounceIsVisibleOnlyWithItsGrant() {
        Assertions.assertFalse(Destination.visible(Set.of()).contains(Destination.BOUNCE), "Bounce must not be visible without titan.navigator.bounce");
        Assertions.assertTrue(Destination.visible(Set.of(Destination.BOUNCE)).contains(Destination.BOUNCE), "Bounce must be visible with titan.navigator.bounce");
    }

    @DisplayName("Bounce's name fades from #00FA9A to #40E0D0 across its letters")
    @Test
    void bounceNameFadesFromSpringGreenToTurquoise(Env env) {
        List<TextColor> letterColors = new ArrayList<>();
        collectLetterColors(Destination.BOUNCE.item(List.of(Component.empty())).get(DataComponents.CUSTOM_NAME), null, letterColors);

        Assertions.assertEquals(TextColor.color(0x00FA9A), letterColors.getFirst(), "the first letter of Bounce must be #00FA9A");
        Assertions.assertEquals(TextColor.color(0x40E0D0), letterColors.getLast(), "the last letter of Bounce must be #40E0D0");
    }

    @DisplayName("Slender's name fades from #616161 to #e80000 across its letters")
    @Test
    void slenderNameFadesFromGrayToDeepRed(Env env) {
        List<TextColor> letterColors = new ArrayList<>();
        collectLetterColors(Destination.SLENDER.item(List.of(Component.empty())).get(DataComponents.CUSTOM_NAME), null, letterColors);

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

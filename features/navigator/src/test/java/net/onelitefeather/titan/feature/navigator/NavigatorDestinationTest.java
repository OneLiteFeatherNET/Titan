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

    @DisplayName("Every main-grid destination belongs to the minigame or the server group")
    @Test
    void destinationsBelongToTheirGroups() {
        Assertions.assertEquals(Destination.Group.MINIGAME, Destination.ELYTRA_RACE.group());
        Assertions.assertEquals(Destination.Group.MINIGAME, Destination.BOUNCE.group());
        Assertions.assertEquals(Destination.Group.MINIGAME, Destination.TAMIAS.group());
        Assertions.assertEquals(Destination.Group.MINIGAME, Destination.SLENDER.group());
        Assertions.assertEquals(Destination.Group.SERVER, Destination.SURVIVAL.group());
        Assertions.assertEquals(Destination.Group.SERVER, Destination.CREATIVE.group());
        Assertions.assertEquals(Destination.Group.SERVER, Destination.BUILD.group());
        Assertions.assertEquals(Destination.Group.STAGING, Destination.SLENDER_STAGING.group());
    }

    @DisplayName("Every main-grid destination names a description")
    @Test
    void mainDestinationsHaveADescription() {
        for (Destination destination : Destination.visible(Set.copyOf(Destination.permissioned()))) {
            Assertions.assertTrue(NavigatorMessages.DESCRIPTIONS.contains(destination.description()), destination + " must name a description key");
        }
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

    @DisplayName("Tamias forwards to the live Tamias task and needs titan.navigator.tamias")
    @Test
    void tamiasForwardsToTheTamiasTaskBehindItsPermission() {
        Assertions.assertEquals("Tamias", Destination.TAMIAS.task());
        Assertions.assertEquals("titan.navigator.tamias", Destination.TAMIAS.permission());
    }

    @DisplayName("Tamias is shown with the amethyst shard icon")
    @Test
    void tamiasUsesTheAmethystShardIcon(Env env) {
        Assertions.assertEquals(Material.AMETHYST_SHARD, Destination.TAMIAS.item(List.of(Component.empty())).material());
    }

    @DisplayName("Slender (Staging) sits in the submenu at slot 0, not on the main grid's slot 16")
    @Test
    void slenderStagingSitsInTheSubmenu() {
        Assertions.assertEquals(0, Destination.SLENDER_STAGING.slot());
        Assertions.assertEquals("Slender-Staging", Destination.SLENDER_STAGING.task());
        Assertions.assertEquals("cygnus.staging.join", Destination.SLENDER_STAGING.permission(), "the staging entry shares the permission the proxy checks");
        Assertions.assertEquals(Material.STRUCTURE_BLOCK, Destination.SLENDER_STAGING.item(List.of(Component.empty())).material());
    }

    @DisplayName("Bounce (Staging) sits in the submenu at slot 1 behind bounce.staging.join")
    @Test
    void bounceStagingSitsInTheSubmenu() {
        Assertions.assertEquals(1, Destination.BOUNCE_STAGING.slot());
        Assertions.assertEquals("Bounce-Staging", Destination.BOUNCE_STAGING.task());
        Assertions.assertEquals("bounce.staging.join", Destination.BOUNCE_STAGING.permission());
        Assertions.assertEquals(Material.SLIME_BLOCK, Destination.BOUNCE_STAGING.item(List.of(Component.empty())).material());
    }

    @DisplayName("ElytraRace (Staging) sits in the submenu at slot 2 behind elytrarace.staging.join")
    @Test
    void elytraRaceStagingSitsInTheSubmenu() {
        Assertions.assertEquals(2, Destination.ELYTRA_RACE_STAGING.slot());
        Assertions.assertEquals("ElytraRace-Staging", Destination.ELYTRA_RACE_STAGING.task());
        Assertions.assertEquals("elytrarace.staging.join", Destination.ELYTRA_RACE_STAGING.permission());
        Assertions.assertEquals(Material.FIREWORK_ROCKET, Destination.ELYTRA_RACE_STAGING.item(List.of(Component.empty())).material());
    }

    @DisplayName("Tamias (Staging) sits in the submenu at slot 3 behind tamias.staging.join")
    @Test
    void tamiasStagingSitsInTheSubmenu() {
        Assertions.assertEquals(3, Destination.TAMIAS_STAGING.slot());
        Assertions.assertEquals("Tamias-Staging", Destination.TAMIAS_STAGING.task());
        Assertions.assertEquals("tamias.staging.join", Destination.TAMIAS_STAGING.permission());
        Assertions.assertEquals(Material.AMETHYST_BLOCK, Destination.TAMIAS_STAGING.item(List.of(Component.empty())).material());
    }

    @DisplayName("Only the staging entries are on demand, and the live entries never are")
    @Test
    void onlyTheStagingEntriesAreOnDemand() {
        Set<Destination> onDemand = Arrays.stream(Destination.values()).filter(Destination::onDemand).collect(Collectors.toSet());

        Assertions.assertEquals(Set.of(Destination.SLENDER_STAGING, Destination.BOUNCE_STAGING, Destination.ELYTRA_RACE_STAGING, Destination.TAMIAS_STAGING), onDemand);
        Assertions.assertFalse(Destination.TAMIAS.onDemand(), "Tamias is live and must never be stopped by the idle stop");
    }

    @DisplayName("Only the gated destinations need a permission: the live ones and the four staging entries")
    @Test
    void onlyTheGatedDestinationsNeedAPermission() {
        Set<Destination> permissioned = Arrays.stream(Destination.values()).filter(destination -> destination.permission() != null).collect(Collectors.toSet());

        Assertions.assertEquals(Set.of(Destination.SLENDER, Destination.BOUNCE, Destination.TAMIAS, Destination.BUILD, Destination.SLENDER_STAGING, Destination.BOUNCE_STAGING, Destination.ELYTRA_RACE_STAGING, Destination.TAMIAS_STAGING), permissioned);
    }

    @DisplayName("A staging entry never appears in the main menu, not even with its own grant")
    @Test
    void stagingEntriesAreNeverPartOfTheMainMenu() {
        Assertions.assertFalse(Destination.visible(Set.of(Destination.SLENDER_STAGING)).contains(Destination.SLENDER_STAGING), "the staging grant must not show the entry on the main grid");
        Assertions.assertFalse(Destination.visible(Set.copyOf(Destination.permissioned())).stream().anyMatch(Destination::staging), "with every grant the main grid holds no staging entry");
    }

    @DisplayName("Tamias is visible only with titan.navigator.tamias")
    @Test
    void tamiasIsVisibleOnlyWithItsGrant() {
        Assertions.assertFalse(Destination.visible(Set.of()).contains(Destination.TAMIAS), "Tamias must be hidden without titan.navigator.tamias");
        Assertions.assertTrue(Destination.visible(Set.of(Destination.TAMIAS)).contains(Destination.TAMIAS), "Tamias must be visible with titan.navigator.tamias");
    }

    @DisplayName("No two staging entries share a submenu slot")
    @Test
    void stagingEntriesHavePairwiseDistinctSlots() {
        List<Integer> slots = Destination.stagingEntries().stream().map(Destination::slot).toList();

        Set<Integer> distinctSlots = new HashSet<>(slots);

        Assertions.assertEquals(slots.size(), distinctSlots.size(), "every staging entry must occupy its own submenu slot, but found duplicates in " + slots);
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

    @DisplayName("visible() lists every main-grid destination once all grants are held, and no staging entry")
    @Test
    void visibleWithAllGrantsListsEveryMainGridDestination() {
        List<Destination> visible = Destination.visible(Set.copyOf(Destination.permissioned()));

        Set<Destination> mainGrid = Arrays.stream(Destination.values()).filter(destination -> !destination.staging()).collect(Collectors.toSet());
        Assertions.assertEquals(mainGrid, Set.copyOf(visible), "every main-grid destination must be visible with all grants, was: " + visible.stream().map(Enum::name).collect(Collectors.joining(", ")));
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

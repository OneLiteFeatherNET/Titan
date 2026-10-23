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
package net.onelitefeather.titan.feature.npc;

import java.util.List;
import java.util.UUID;
import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApparitionRulesTest {

    private static final Pos POINT = new Pos(112.5, 80, 157.5);

    private static ApparitionRules rules() {
        return new ApparitionRules(List.of(POINT));
    }

    private static ApparitionRules.Presence at(UUID player, Pos position, boolean inside) {
        return new ApparitionRules.Presence(player, position, inside);
    }

    @DisplayName("A silhouette shows when a player inside the zone comes within 14 blocks")
    @Test
    void showsWithinRange() {
        UUID player = UUID.randomUUID();
        var changes = rules().update(List.of(at(player, new Pos(112.5, 80, 143.5), true)));
        Assertions.assertEquals(1, changes.size(), "one player is 14 blocks from the point");
        Assertions.assertTrue(changes.getFirst().show(), "the change is a show");
    }

    @DisplayName("No silhouette shows farther than 14 blocks away")
    @Test
    void doesNotShowFarAway() {
        Assertions.assertTrue(rules().update(List.of(at(UUID.randomUUID(), new Pos(112.5, 80, 130.5), true))).isEmpty(), "27 blocks away is too far");
    }

    @DisplayName("A player outside the zone never sees a silhouette")
    @Test
    void outsideZoneDoesNotShow() {
        Assertions.assertTrue(rules().update(List.of(at(UUID.randomUUID(), new Pos(112.5, 80, 150.5), false))).isEmpty(), "a player outside the zone is not shown anything");
    }

    @DisplayName("A silhouette hides when the player comes within 4 blocks")
    @Test
    void hidesWhenCloseAndNeverShowsAgain() {
        ApparitionRules rules = rules();
        UUID player = UUID.randomUUID();
        rules.update(List.of(at(player, new Pos(112.5, 80, 143.5), true)));
        var hides = rules.update(List.of(at(player, new Pos(112.5, 80, 153.5), true)));
        Assertions.assertEquals(1, hides.size(), "the player is 4 blocks away");
        Assertions.assertFalse(hides.getFirst().show(), "the change is a hide");
        Assertions.assertTrue(rules.update(List.of(at(player, new Pos(112.5, 80, 140.5), true))).isEmpty(), "a point shows to a player at most once");
    }

    @DisplayName("A silhouette hides when the player leaves the zone")
    @Test
    void hidesWhenLeavingTheZone() {
        ApparitionRules rules = rules();
        UUID player = UUID.randomUUID();
        rules.update(List.of(at(player, new Pos(112.5, 80, 143.5), true)));
        var hides = rules.update(List.of(at(player, new Pos(112.5, 80, 143.5), false)));
        Assertions.assertEquals(1, hides.size(), "leaving the zone hides what the player sees");
    }

    @DisplayName("A disconnect hides every silhouette the player still sees")
    @Test
    void forgetHidesEverythingShown() {
        ApparitionRules rules = rules();
        UUID player = UUID.randomUUID();
        rules.update(List.of(at(player, new Pos(112.5, 80, 143.5), true)));
        var hides = rules.forget(player);
        Assertions.assertEquals(1, hides.size(), "the silhouette shown to the leaving player is hidden");
        Assertions.assertFalse(hides.getFirst().show(), "the change is a hide");
    }

    @DisplayName("Each player sees the silhouette on his own, not the others")
    @Test
    void visibilityIsPerPlayer() {
        ApparitionRules rules = rules();
        UUID near = UUID.randomUUID();
        UUID far = UUID.randomUUID();
        var changes = rules.update(List.of(at(near, new Pos(112.5, 80, 143.5), true), at(far, new Pos(112.5, 80, 165.5), false)));
        Assertions.assertEquals(near, changes.getFirst().player(), "only the player near the point gets the silhouette");
        Assertions.assertEquals(1, changes.size(), "the far player gets nothing: " + changes);
    }
}

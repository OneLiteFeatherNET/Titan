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
package net.onelitefeather.titan.feature.portal;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Every test builds its own trigger, permissions and clock (F.I.R.S.T. - Independent). */
class PortalTriggerTest {

    private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");
    private static final Point OUTSIDE = new Pos(10.5, 64.5, 0.5);
    private static final Point INSIDE = new Pos(0.5, 64.5, 0.5);

    private final AdjustableClock clock = new AdjustableClock(START, ZoneOffset.UTC);
    private final List<String> delivered = new ArrayList<>();
    private final List<String> denied = new ArrayList<>();
    private final FakePermissionService permissions = new FakePermissionService();

    private static Portal box(String id, Vec min, Vec max, String task, String permission) {
        return new Portal(id, new Box(min, max), task, permission);
    }

    private static Portal openBox() {
        return box("survival", new Vec(0, 64, 0), new Vec(1, 65, 1), "Survival", null);
    }

    private static UUID player() {
        return UUID.randomUUID();
    }

    private void move(PortalTrigger trigger, UUID player, Point from, Point to) {
        trigger.onMove(player, from, to).ifPresent(portal -> this.delivered.add(portal.task()));
    }

    private PortalTrigger trigger(Portal... portals) {
        return new PortalTrigger(new PortalIndex(List.of(portals)), this.permissions, this.clock, portal -> this.denied.add(portal.id()));
    }

    @DisplayName("Walking into a box delivers the player to the portal's task exactly once")
    @Test
    void enteringDeliversOnce() {
        PortalTrigger trigger = trigger(openBox());
        UUID player = player();

        move(trigger, player, OUTSIDE, INSIDE);

        Assertions.assertEquals(List.of("Survival"), this.delivered, "entering 'survival' must deliver to its task once");
    }

    @DisplayName("Staying inside a box does not deliver again, even after the cooldown has passed")
    @Test
    void stayingDoesNotRepeat() {
        PortalTrigger trigger = trigger(openBox());
        UUID player = player();
        move(trigger, player, OUTSIDE, INSIDE);

        this.clock.advance(Duration.ofSeconds(10));
        move(trigger, player, INSIDE, new Pos(1.2, 64.5, 0.5));

        Assertions.assertEquals(1, this.delivered.size(), "staying in 'survival' 10 s after the delivery must not deliver again");
    }

    @DisplayName("Leaving and re-entering within 3 seconds does not deliver again")
    @Test
    void reenteringDuringCooldownDoesNothing() {
        PortalTrigger trigger = trigger(openBox());
        UUID player = player();
        move(trigger, player, OUTSIDE, INSIDE);
        move(trigger, player, INSIDE, OUTSIDE);
        move(trigger, player, OUTSIDE, new Pos(10.5, 64.5, 5.5));
        this.clock.advance(Duration.ofSeconds(2));

        move(trigger, player, new Pos(10.5, 64.5, 5.5), INSIDE);

        Assertions.assertEquals(1, this.delivered.size(), "re-entering 2 s after the delivery is inside the 3 s cooldown");
    }

    @DisplayName("Leaving and re-entering 3 seconds after the delivery delivers again")
    @Test
    void reenteringAfterCooldownDelivers() {
        PortalTrigger trigger = trigger(openBox());
        UUID player = player();
        move(trigger, player, OUTSIDE, INSIDE);
        move(trigger, player, INSIDE, OUTSIDE);
        move(trigger, player, OUTSIDE, new Pos(10.5, 64.5, 5.5));
        this.clock.advance(Duration.ofSeconds(3));

        move(trigger, player, new Pos(10.5, 64.5, 5.5), INSIDE);

        Assertions.assertEquals(2, this.delivered.size(), "re-entering exactly 3 s after the delivery must deliver");
    }

    @DisplayName("A touch during the cooldown is not made up for once the cooldown is over")
    @Test
    void touchDuringCooldownIsNotCaughtUpLater() {
        PortalTrigger trigger = trigger(openBox());
        UUID player = player();
        move(trigger, player, OUTSIDE, INSIDE);
        move(trigger, player, INSIDE, OUTSIDE);
        move(trigger, player, OUTSIDE, new Pos(10.5, 64.5, 5.5));
        move(trigger, player, new Pos(10.5, 64.5, 5.5), INSIDE);
        this.clock.advance(Duration.ofSeconds(10));

        move(trigger, player, INSIDE, new Pos(1.2, 64.5, 0.5));

        Assertions.assertEquals(1, this.delivered.size(), "an entry swallowed by the cooldown must not fire while the player stays");
    }

    @DisplayName("Flying through a disc delivers, and a second pass after the cooldown delivers again")
    @Test
    void discPassesDeliver() {
        Portal disc = new Portal("gate", new Disc(new Vec(0, 64, 0), 3, new Vec(1, 0, 0)), "Gate", null);
        PortalTrigger trigger = trigger(disc);
        UUID player = player();
        Point west = new Pos(-2, 64, 0);
        Point east = new Pos(2, 64, 0);

        move(trigger, player, west, east);
        move(trigger, player, east, new Pos(4, 64, 0));
        this.clock.advance(Duration.ofSeconds(3));
        move(trigger, player, new Pos(4, 64, 0), west);

        Assertions.assertEquals(2, this.delivered.size(), "both passes through 'gate' (there and back, 3 s apart) must deliver");
    }

    @DisplayName("A step of 8 blocks that jumps over a 1-block-thin disc delivers")
    @Test
    void fastStepOverThinDiscDelivers() {
        Portal disc = new Portal("gate", new Disc(new Vec(0, 64, 0), 3, new Vec(1, 0, 0)), "Gate", null);
        PortalTrigger trigger = trigger(disc);
        UUID player = player();

        move(trigger, player, new Pos(-4, 64, 0), new Pos(4, 64, 0));

        Assertions.assertEquals(1, this.delivered.size(), "the segment crosses the disc although neither endpoint is near it");
    }

    @DisplayName("Two players do not influence each other's cooldown")
    @Test
    void playersAreIndependent() {
        PortalTrigger trigger = trigger(openBox());
        UUID first = player();
        UUID second = player();

        move(trigger, first, OUTSIDE, INSIDE);
        move(trigger, second, OUTSIDE, INSIDE);

        Assertions.assertEquals(2, this.delivered.size(), "the second player's entry is not blocked by the first player's cooldown");
    }

    @DisplayName("The cooldown covers every portal, not only the one just used")
    @Test
    void cooldownCoversAllPortals() {
        Portal other = box("creative", new Vec(20, 64, 0), new Vec(21, 65, 1), "Creative", null);
        PortalTrigger trigger = trigger(openBox(), other);
        UUID player = player();
        move(trigger, player, OUTSIDE, INSIDE);
        this.clock.advance(Duration.ofSeconds(1));

        move(trigger, player, new Pos(25.5, 64.5, 0.5), new Pos(20.5, 64.5, 0.5));

        Assertions.assertEquals(1, this.delivered.size(), "entering 'creative' 1 s after 'survival' is inside the cooldown");
    }

    @DisplayName("Forgetting a player clears the latch and the cooldown")
    @Test
    void forgetClearsState() {
        PortalTrigger trigger = trigger(openBox());
        UUID player = player();
        move(trigger, player, OUTSIDE, INSIDE);

        trigger.forget(player);
        move(trigger, player, OUTSIDE, INSIDE);

        Assertions.assertEquals(2, this.delivered.size(), "after the disconnect cleanup the same player starts from scratch");
    }

    @DisplayName("A player without the portal's permission is neither delivered nor put on cooldown")
    @Test
    void missingPermissionDoesNothingAndNoCooldown() {
        for (PermissionResult missing : List.of(PermissionResult.NOT_SET, PermissionResult.DENIED)) {
            this.delivered.clear();
            FakePermissionService localPermissions = new FakePermissionService();
            Portal restricted = box("vip", new Vec(0, 64, 0), new Vec(1, 65, 1), "Vip", "titan.portal.vip");
            Portal open = box("survival", new Vec(20, 64, 0), new Vec(21, 65, 1), "Survival", null);
            PortalTrigger trigger = new PortalTrigger(new PortalIndex(List.of(restricted, open)), localPermissions, this.clock, portal -> this.denied.add(portal.id()));
            UUID player = player();
            localPermissions.set(player, "titan.portal.vip", missing);

            move(trigger, player, OUTSIDE, INSIDE);
            move(trigger, player, new Pos(25.5, 64.5, 0.5), new Pos(20.5, 64.5, 0.5));

            Assertions.assertEquals(List.of("Survival"), this.delivered, "with " + missing + " 'vip' must do nothing and leave the open portal usable at once");
        }
    }

    @DisplayName("A denied entry is reported once, with the portal it was refused for")
    @Test
    void deniedEntryIsReported() {
        Portal restricted = box("vip", new Vec(0, 64, 0), new Vec(1, 65, 1), "Vip", "titan.portal.vip");
        PortalTrigger trigger = trigger(restricted);
        UUID player = player();

        move(trigger, player, OUTSIDE, INSIDE);
        move(trigger, player, INSIDE, new Pos(1.2, 64.5, 0.5));

        Assertions.assertEquals(List.of("vip"), this.denied, "one refused entry is reported once, staying inside does not report it again");
        Assertions.assertTrue(this.delivered.isEmpty(), "a refused entry delivers nothing");
    }

    @DisplayName("A refused entry during the cooldown of another delivery is not reported as denied")
    @Test
    void deniedEntryDuringCooldownIsNotReported() {
        Portal open = box("survival", new Vec(20, 64, 0), new Vec(21, 65, 1), "Survival", null);
        Portal restricted = box("vip", new Vec(0, 64, 0), new Vec(1, 65, 1), "Vip", "titan.portal.vip");
        PortalTrigger trigger = trigger(open, restricted);
        UUID player = player();
        move(trigger, player, new Pos(25.5, 64.5, 0.5), new Pos(20.5, 64.5, 0.5));
        move(trigger, player, new Pos(20.5, 64.5, 0.5), new Pos(10.5, 64.5, 0.5));
        this.clock.advance(Duration.ofSeconds(2));

        move(trigger, player, new Pos(10.5, 64.5, 0.5), INSIDE);

        Assertions.assertEquals(List.of("Survival"), this.delivered, "the first entry delivered");
        Assertions.assertTrue(this.denied.isEmpty(), "the refused entry falls into the cooldown, which is not a denial");
    }

    @DisplayName("A player with the portal's permission is delivered")
    @Test
    void grantedPermissionDelivers() {
        Portal restricted = box("vip", new Vec(0, 64, 0), new Vec(1, 65, 1), "Vip", "titan.portal.vip");
        PortalTrigger trigger = trigger(restricted);
        UUID player = player();
        this.permissions.set(player, "titan.portal.vip", PermissionResult.ALLOWED);

        move(trigger, player, OUTSIDE, INSIDE);

        Assertions.assertEquals(List.of("Vip"), this.delivered, "ALLOWED must deliver to the portal's task");
    }
}

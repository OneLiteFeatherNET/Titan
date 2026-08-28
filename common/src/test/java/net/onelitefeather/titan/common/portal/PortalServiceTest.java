/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.portal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.deliver.ServiceAvailability;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import net.onelitefeather.titan.common.feature.TestFeatureAudience;
import net.onelitefeather.titan.common.feature.TitanFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.togglz.core.activation.DefaultActivationStrategyProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.user.NoOpUserProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PortalServiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final long COOLDOWN = 3000L;

    /** Blocks 0..4 on every axis. */
    private static final Vec MIN = new Vec(0, 64, 0);
    private static final Vec MAX = new Vec(4, 68, 4);

    private static final Pos INSIDE = new Pos(2.5, 64, 2.5);
    private static final Pos ALSO_INSIDE = new Pos(3.5, 64, 3.5);
    private static final Pos OUTSIDE = new Pos(30.5, 64, 30.5);

    @Test
    @DisplayName("walking into a portal delivers the player to the configured target")
    void deliversOnEntry(Env env) {
        Fixture fixture = new Fixture(env, ungated());

        assertEquals(PortalOutcome.DELIVERED, fixture.move(INSIDE));

        assertEquals(1, fixture.deliver.components.size());
        DeliverComponent component = fixture.deliver.components.getFirst();
        assertEquals(fixture.player.getUuid(), component.playerId());
        assertInstanceOf(DeliverComponent.TaskComponent.class, component);
        assertEquals("Survival", ((DeliverComponent.TaskComponent) component).taskName());
    }

    @Test
    @DisplayName("a portal pointing at a single service delivers to that service")
    void deliversToNamedServer(Env env) {
        Fixture fixture = new Fixture(env, new PortalDefinition("build", "server", "Build-1", null, MIN, MAX));

        assertEquals(PortalOutcome.DELIVERED, fixture.move(INSIDE));

        DeliverComponent component = fixture.deliver.components.getFirst();
        assertInstanceOf(DeliverComponent.ServerDeliverComponent.class, component);
        assertEquals("Build-1", ((DeliverComponent.ServerDeliverComponent) component).gameServer());
    }

    @Test
    @DisplayName("moving outside a portal does nothing at all")
    void ignoresMovementOutside(Env env) {
        Fixture fixture = new Fixture(env, ungated());

        assertEquals(PortalOutcome.NO_PORTAL, fixture.move(OUTSIDE));

        assertTrue(fixture.deliver.components.isEmpty());
        fixture.chat.assertEmpty();
    }

    @Test
    @DisplayName("an unreachable target leaves the player where they are and tells them so")
    void reportsUnreachableTarget(Env env) {
        Fixture fixture = new Fixture(env, ungated());
        fixture.reachable = false;
        fixture.player.teleport(INSIDE);

        assertEquals(PortalOutcome.TARGET_UNREACHABLE, fixture.move(INSIDE));

        assertTrue(fixture.deliver.components.isEmpty(), "nobody is sent anywhere");
        assertEquals(INSIDE, fixture.player.getPosition(), "the player is not moved");
        fixture.chat.assertSingle();
    }

    @Test
    @DisplayName("a delivery that throws is reported to the player rather than swallowed")
    void reportsFailedDelivery(Env env) {
        Fixture fixture = new Fixture(env, ungated());
        fixture.deliver.explode = true;
        fixture.player.teleport(INSIDE);

        assertEquals(PortalOutcome.DELIVERY_FAILED, fixture.move(INSIDE));

        assertEquals(INSIDE, fixture.player.getPosition());
        fixture.chat.assertSingle();
    }

    @Test
    @DisplayName("a gated portal refuses a player the navigator would refuse too")
    void refusesPlayerOutsideTheStage(Env env) {
        Fixture fixture = new Fixture(env, gated());
        fixture.release(TitanFeatures.NAVIGATOR_SURVIVAL, ReleaseStage.INTERNAL);

        assertEquals(PortalOutcome.DENIED_FEATURE, fixture.move(INSIDE));

        assertTrue(fixture.deliver.components.isEmpty());
        fixture.chat.assertSingle();
    }

    @Test
    @DisplayName("a gated portal admits the player the navigator admits: same gate, same answer")
    void admitsPlayerInsideTheStage(Env env) {
        Fixture fixture = new Fixture(env, gated());
        fixture.release(TitanFeatures.NAVIGATOR_SURVIVAL, ReleaseStage.INTERNAL);
        fixture.audience.grantPermission(fixture.player.getUuid(), ReleaseStage.INTERNAL_PERMISSION);

        assertTrue(fixture.gate.isVisibleTo(TitanFeatures.NAVIGATOR_SURVIVAL, fixture.player.getUuid()), "the navigator entry would be shown");
        assertEquals(PortalOutcome.DELIVERED, fixture.move(INSIDE));
        assertEquals(1, fixture.deliver.components.size());
    }

    @Test
    @DisplayName("a feature nobody enabled keeps its portal shut")
    void refusesWhenTheFeatureIsUnconfigured(Env env) {
        Fixture fixture = new Fixture(env, gated());

        assertEquals(PortalOutcome.DENIED_FEATURE, fixture.move(INSIDE));
        assertTrue(fixture.deliver.components.isEmpty());
    }

    @Test
    @DisplayName("standing in a portal does not fire it again, however often the player moves")
    void doesNotRetriggerWhileInside(Env env) {
        Fixture fixture = new Fixture(env, ungated());

        assertEquals(PortalOutcome.DELIVERED, fixture.move(INSIDE));
        for (int movement = 0; movement < 20; movement++) {
            assertEquals(PortalOutcome.ALREADY_INSIDE, fixture.move(ALSO_INSIDE));
        }

        assertEquals(1, fixture.deliver.components.size(), "one entry, one delivery");
    }

    @Test
    @DisplayName("a player left behind by a failed switch is not messaged again on every step")
    void doesNotRepeatTheRefusal(Env env) {
        Fixture fixture = new Fixture(env, ungated());
        fixture.reachable = false;

        assertEquals(PortalOutcome.TARGET_UNREACHABLE, fixture.move(INSIDE));
        for (int movement = 0; movement < 20; movement++) {
            assertEquals(PortalOutcome.ALREADY_INSIDE, fixture.move(ALSO_INSIDE));
        }

        fixture.chat.assertSingle();
    }

    @Test
    @DisplayName("stepping out and straight back in is debounced by the cooldown")
    void debouncesQuickReentry(Env env) {
        Fixture fixture = new Fixture(env, ungated());

        assertEquals(PortalOutcome.DELIVERED, fixture.move(INSIDE));
        assertEquals(PortalOutcome.NO_PORTAL, fixture.move(OUTSIDE));
        fixture.clock.advance(COOLDOWN - 1);

        assertEquals(PortalOutcome.COOLING_DOWN, fixture.move(INSIDE));
        assertEquals(1, fixture.deliver.components.size());
    }

    @Test
    @DisplayName("after the cooldown, re-entering works again - it is a debounce, not a ban")
    void deliversAgainAfterTheCooldown(Env env) {
        Fixture fixture = new Fixture(env, ungated());

        assertEquals(PortalOutcome.DELIVERED, fixture.move(INSIDE));
        assertEquals(PortalOutcome.NO_PORTAL, fixture.move(OUTSIDE));
        fixture.clock.advance(COOLDOWN);

        assertEquals(PortalOutcome.DELIVERED, fixture.move(INSIDE));
        assertEquals(2, fixture.deliver.components.size());
    }

    @Test
    @DisplayName("an unusable entry is not a portal: the region stays empty")
    void dropsUnusableEntries(Env env) {
        Fixture fixture = new Fixture(env, new PortalDefinition("broken", null, "Survival", "NOT_A_FEATURE", MIN, MAX));

        assertTrue(fixture.service.index().isEmpty());
        assertEquals(PortalOutcome.NO_PORTAL, fixture.move(INSIDE));
        assertTrue(fixture.deliver.components.isEmpty());
    }

    @Test
    @DisplayName("a duplicate id is refused, so the latch keeps identifying one portal")
    void dropsDuplicateIds(Env env) {
        PortalDefinition first = new PortalDefinition("survival", null, "Survival", null, MIN, MAX);
        PortalDefinition second = new PortalDefinition("survival", null, "Other", null, new Vec(40, 64, 40), new Vec(44, 68, 44));
        Fixture fixture = new Fixture(env, first, second);

        assertEquals(1, fixture.service.index().portals().size());
        assertEquals(PortalOutcome.NO_PORTAL, fixture.move(new Pos(42.5, 66, 42.5)));
    }

    private static PortalDefinition ungated() {
        return new PortalDefinition("survival", "task", "Survival", null, MIN, MAX);
    }

    private static PortalDefinition gated() {
        return new PortalDefinition("survival", "task", "Survival", "NAVIGATOR_SURVIVAL", MIN, MAX);
    }

    /** Everything a portal needs, with the two switches a test wants to flip. */
    private static final class Fixture {

        private final InMemoryStateRepository repository = new InMemoryStateRepository();
        private final TestFeatureAudience audience = new TestFeatureAudience();
        private final RecordingDeliver deliver = new RecordingDeliver();
        private final MutableClock clock = new MutableClock(Instant.parse("2026-10-15T12:00:00Z"));
        private final FeatureGate gate;
        private final PortalService service;
        private final Player player;
        private final Collector<SystemChatPacket> chat;
        private boolean reachable = true;

        private Fixture(Env env, PortalDefinition... portals) {
            FeatureManager featureManager = new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(this.repository).userProvider(new NoOpUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();
            this.gate = FeatureGate.with(featureManager, this.audience, this.clock, BERLIN);
            PortalConfig config = PortalConfig.of(List.of(portals), COOLDOWN, "<red>unreachable <target>", "<red>denied <portal>");
            ServiceAvailability availability = new ServiceAvailability() {

                @Override
                public boolean isTaskReachable(String taskName) {
                    return Fixture.this.reachable;
                }

                @Override
                public boolean isServerReachable(String serviceName) {
                    return Fixture.this.reachable;
                }
            };
            this.service = PortalService.create(config, this.deliver, this.gate, availability, this.clock);
            Instance instance = env.createFlatInstance();
            TestConnection connection = env.createConnection();
            this.chat = connection.trackIncoming(SystemChatPacket.class);
            this.player = connection.connect(instance, new Pos(0, 64, 0));
        }

        private PortalOutcome move(Pos position) {
            return this.service.handleMove(this.player, position);
        }

        private void release(TitanFeatures feature, ReleaseStage stage) {
            this.repository.setFeatureState(new FeatureState(feature, true).setParameter(FeatureGate.STAGE_PARAMETER, stage.id()));
        }
    }

    /** Records what was delivered, and can be told to fail the way a broken route would. */
    private static final class RecordingDeliver implements Deliver {

        private final List<DeliverComponent> components = new ArrayList<>();
        private boolean explode;

        @Override
        public void sendPlayer(Player player, DeliverComponent component) {
            if (this.explode) {
                throw new IllegalStateException("the delivery route is down");
            }
            this.components.add(component);
        }
    }

    /** A clock a test can push forward, so the cooldown is testable without waiting. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(long millis) {
            this.instant = this.instant.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.instant;
        }
    }
}

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
package net.onelitefeather.titan.common.resourcepack;

import net.kyori.adventure.resource.ResourcePackStatus;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.ResourcePackPopPacket;
import net.minestom.server.network.packet.server.common.ResourcePackPushPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class ResourcePackServiceIntegrationTest {

    private static final String BASE_HASH = "1111111111111111111111111111111111111111";
    private static final String SEASON_HASH = "2222222222222222222222222222222222222222";
    private static final String NEXT_SEASON_HASH = "3333333333333333333333333333333333333333";

    private static final ResourcePackDefinition BASE = new ResourcePackDefinition(UUID.fromString("00000000-0000-4000-8000-00000000ba5e"), "https://packs.example/pack-" + BASE_HASH + ".zip", BASE_HASH, false, null);
    private static final ResourcePackDefinition SEASON = new ResourcePackDefinition(UUID.fromString("00000000-0000-4000-8000-000000005ea0"), "https://packs.example/pack-" + SEASON_HASH + ".zip", SEASON_HASH, false, null);
    private static final ResourcePackDefinition NEXT_SEASON = new ResourcePackDefinition(UUID.fromString("00000000-0000-4000-8000-000000005ea1"), "https://packs.example/pack-" + NEXT_SEASON_HASH + ".zip", NEXT_SEASON_HASH, false, null);

    /** Collects the timeout guards instead of running them, so a test can fire them on demand. */
    private static final class ManualScheduler implements PackTimeoutScheduler {

        private final Deque<Runnable> pending = new ArrayDeque<>();

        @Override
        public void schedule(Runnable task, Duration delay) {
            this.pending.add(task);
        }

        private void runAll() {
            while (!this.pending.isEmpty()) {
                this.pending.poll().run();
            }
        }
    }

    private static ResourcePackSettings settings(ResourcePackDefinition base, ResourcePackDefinition season) {
        return new ResourcePackSettings(base, season, 5000L, false, ".");
    }

    private static ResourcePackService service(ResourcePackSettings settings, BedrockDetector detector, PackTimeoutScheduler scheduler) {
        return new ResourcePackService(settings, new HeldPackRegistry(), detector, scheduler);
    }

    @Test
    @DisplayName("An unconfigured lobby sends no resource pack packet at all")
    void testUnconfiguredLobbySendsNothing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);
        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        Player player = connection.connect(instance);

        ResourcePackService service = service(ResourcePackSettings.disabled(), BedrockDetector.never(), PackTimeoutScheduler.never());
        assertFalse(service.enabled());
        service.onConfiguration(player);
        service.applySeason(null, List.of(player));

        pushes.assertEmpty();
        pops.assertEmpty();
    }

    @Test
    @DisplayName("Base and season pack are pushed as two packs, each with its own id and hash")
    void testBaseAndSeasonArePushed(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);
        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        Player player = connection.connect(instance);

        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), PackTimeoutScheduler.never());
        service.onConfiguration(player);

        List<ResourcePackPushPacket> pushed = pushes.collect();
        assertEquals(2, pushed.size());
        assertEquals(BASE.id(), pushed.get(0).id());
        assertEquals(BASE_HASH, pushed.get(0).hash());
        assertEquals(SEASON.id(), pushed.get(1).id());
        assertEquals(SEASON_HASH, pushed.get(1).hash());
        pops.assertEmpty();
    }

    @Test
    @DisplayName("A pack the player already holds is not pushed a second time")
    void testHeldPackIsNotPushedAgain(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);
        Player player = connection.connect(instance);

        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), PackTimeoutScheduler.never());
        service.onConfiguration(player);
        service.onConfiguration(player);

        assertEquals(2, pushes.collect().size());
    }

    @Test
    @DisplayName("A season change pops only the season id and pushes the new one, the base pack stays")
    void testSeasonChangeSwapsOnlyTheSeason(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), PackTimeoutScheduler.never());
        service.onConfiguration(player);

        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);
        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        service.applySeason(NEXT_SEASON, List.of(player));

        List<ResourcePackPopPacket> popped = pops.collect();
        assertEquals(1, popped.size(), "Exactly one pack is removed on a season change");
        assertEquals(SEASON.id(), popped.getFirst().id());
        assertNotNull(popped.getFirst().id(), "A pop without an id clears every pack the client holds");

        List<ResourcePackPushPacket> pushed = pushes.collect();
        assertEquals(1, pushed.size(), "Only the new season is pushed, the base pack is not resent");
        assertEquals(NEXT_SEASON.id(), pushed.getFirst().id());

        assertTrue(service.registry().holds(player.getUuid(), PackSlot.BASE, BASE.id()));
        assertTrue(service.registry().holds(player.getUuid(), PackSlot.SEASON, NEXT_SEASON.id()));
    }

    @Test
    @DisplayName("Ending a season pops it without pushing a successor")
    void testSeasonEnd(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), PackTimeoutScheduler.never());
        service.onConfiguration(player);

        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);
        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        service.applySeason(null, List.of(player));

        pops.assertSingle(packet -> assertEquals(SEASON.id(), packet.id()));
        pushes.assertEmpty();
        assertTrue(service.registry().entry(player.getUuid(), PackSlot.SEASON).isEmpty());
        assertTrue(service.registry().holds(player.getUuid(), PackSlot.BASE, BASE.id()));
    }

    @Test
    @DisplayName("A season change reaches players who join afterwards")
    void testSeasonChangeAppliesToLaterJoins(Env env) {
        Instance instance = env.createFlatInstance();
        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), PackTimeoutScheduler.never());
        service.applySeason(NEXT_SEASON, List.of());

        TestConnection connection = env.createConnection();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);
        Player player = connection.connect(instance);
        service.onConfiguration(player);

        List<ResourcePackPushPacket> pushed = pushes.collect();
        assertEquals(2, pushed.size());
        assertEquals(NEXT_SEASON.id(), pushed.get(1).id());
    }

    @Test
    @DisplayName("A bedrock player receives nothing, because geyser would confirm a pack it never delivered")
    void testBedrockPlayerReceivesNothing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);
        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        Player player = connection.connect(instance);

        ResourcePackService service = service(settings(BASE, SEASON), (id, name) -> true, PackTimeoutScheduler.never());
        service.onConfiguration(player);
        service.applySeason(NEXT_SEASON, List.of(player));

        pushes.assertEmpty();
        pops.assertEmpty();
        assertEquals(0, service.registry().trackedPlayers());
    }

    @Test
    @DisplayName("A bedrock player's reported success is ignored instead of being believed")
    void testBedrockStatusIsIgnored(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ResourcePackSettings settings = new ResourcePackSettings(BASE, null, 5000L, true, ".");
        ResourcePackService service = service(settings, (id, name) -> true, PackTimeoutScheduler.never());
        service.onConfiguration(player);

        service.onStatus(player, BASE.id(), ResourcePackStatus.SUCCESSFULLY_LOADED);

        assertFalse(service.registry().entry(player.getUuid(), PackSlot.BASE).orElseThrow().confirmed(), "Geyser reports success without delivering, so the book must not record it as loaded");
    }

    @Test
    @DisplayName("A java player's answer updates the book")
    void testStatusUpdatesTheBook(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), PackTimeoutScheduler.never());
        service.onConfiguration(player);

        service.onStatus(player, BASE.id(), ResourcePackStatus.DOWNLOADED);
        assertFalse(service.registry().entry(player.getUuid(), PackSlot.BASE).orElseThrow().confirmed(), "An intermediate status decides nothing");

        service.onStatus(player, BASE.id(), ResourcePackStatus.SUCCESSFULLY_LOADED);
        assertTrue(service.registry().entry(player.getUuid(), PackSlot.BASE).orElseThrow().confirmed());

        service.onStatus(player, SEASON.id(), ResourcePackStatus.DECLINED);
        assertTrue(service.registry().entry(player.getUuid(), PackSlot.SEASON).isEmpty(), "A declined pack is not held");
    }

    @Test
    @DisplayName("A declined pack is offered again on the next configuration")
    void testDeclinedPackIsPushedAgain(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);
        Player player = connection.connect(instance);

        ResourcePackService service = service(settings(BASE, null), BedrockDetector.never(), PackTimeoutScheduler.never());
        service.onConfiguration(player);
        service.onStatus(player, BASE.id(), ResourcePackStatus.DECLINED);
        service.onConfiguration(player);

        assertEquals(2, pushes.collect().size());
    }

    @Test
    @DisplayName("A disconnect clears the book")
    void testDisconnectClearsTheBook(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), PackTimeoutScheduler.never());
        service.onConfiguration(player);
        assertEquals(1, service.registry().trackedPlayers());

        service.onDisconnect(player.getUuid());

        assertEquals(0, service.registry().trackedPlayers());
    }

    @Test
    @DisplayName("The timeout guard completes the pack future a parked configuration thread waits on")
    void testTimeoutCompletesThePendingFuture(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ManualScheduler scheduler = new ManualScheduler();
        ResourcePackService service = service(settings(BASE, null), BedrockDetector.never(), scheduler);
        service.onConfiguration(player);

        CompletableFuture<Void> future = player.getResourcePackFuture();
        assertNotNull(future, "A pushed pack leaves a future that doConfiguration joins without a timeout");
        assertFalse(future.isDone(), "Nothing else completes that future while the client stays silent");
        assertEquals(1, scheduler.pending.size(), "Every push arms exactly one guard");

        scheduler.runAll();

        assertTrue(future.isDone(), "The guard has to release the configuration thread itself");
    }

    @Test
    @DisplayName("No pack pushed means no guard armed")
    void testNoGuardWithoutPush(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ManualScheduler scheduler = new ManualScheduler();
        ResourcePackService service = service(ResourcePackSettings.disabled(), BedrockDetector.never(), scheduler);
        service.onConfiguration(player);

        assertTrue(scheduler.pending.isEmpty());
    }

    @Test
    @DisplayName("An expired guard leaves no stale future behind, so the next request is waited on again")
    void testExpiredGuardDoesNotPoisonTheNextRequest(Env env) {
        // Scenario A: the client never answers. Minestom clears Player#resourcePackFuture only
        // inside onResourcePackStatus, so a guard that merely completes the future from outside
        // leaves it in the field, completed, forever - and sendResourcePacks then reuses it
        // instead of creating a new one. Every later configuration pass would join an
        // already-completed future and stop waiting for packs at all.
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ManualScheduler scheduler = new ManualScheduler();
        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), scheduler);
        service.onConfiguration(player);
        CompletableFuture<Void> first = player.getResourcePackFuture();
        assertNotNull(first);

        scheduler.runAll();

        assertTrue(first.isDone(), "The parked configuration thread has to be released");
        assertNull(player.getResourcePackFuture(), "The expired request must not stay in the player's future field");
        assertTrue(service.registry().entry(player.getUuid(), PackSlot.BASE).isEmpty(), "A pack that timed out is not held");
        assertTrue(service.registry().entry(player.getUuid(), PackSlot.SEASON).isEmpty(), "A pack that timed out is not held");

        service.applySeason(NEXT_SEASON, List.of(player));

        CompletableFuture<Void> second = player.getResourcePackFuture();
        assertNotNull(second, "The next push has to create a future of its own");
        assertNotSame(first, second);
        assertFalse(second.isDone(), "The next configuration pass has to wait for the client again");
    }

    @Test
    @DisplayName("A guard belonging to an answered request never releases the request that follows it")
    void testStaleGuardDoesNotReleaseALaterRequest(Env env) {
        // Scenario B: the client answers in time, but the guard armed for that request is still
        // queued. A guard that looks the future up when it fires would find the *next* request's
        // future and complete it - letting configuration proceed before the client answered.
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ManualScheduler scheduler = new ManualScheduler();
        ResourcePackService service = service(settings(BASE, null), BedrockDetector.never(), scheduler);
        service.onConfiguration(player);
        CompletableFuture<Void> first = player.getResourcePackFuture();
        assertNotNull(first);
        assertEquals(1, scheduler.pending.size());

        // The client answers the first request. Minestom completes and clears the future.
        player.onResourcePackStatus(BASE.id(), ResourcePackStatus.SUCCESSFULLY_LOADED);
        service.onStatus(player, BASE.id(), ResourcePackStatus.SUCCESSFULLY_LOADED);
        assertTrue(first.isDone());
        assertNull(player.getResourcePackFuture());

        // A second request goes out while the first guard is still queued.
        service.applySeason(SEASON, List.of(player));
        CompletableFuture<Void> second = player.getResourcePackFuture();
        assertNotNull(second);
        assertNotSame(first, second);

        // The stale guard fires. It belongs to the answered request and must do nothing.
        scheduler.pending.poll().run();

        assertFalse(second.isDone(), "The stale guard released a request the client has not answered yet");
        assertTrue(service.registry().holds(player.getUuid(), PackSlot.SEASON, SEASON.id()), "The stale guard must not touch the pack of a later request");
        assertTrue(service.registry().holds(player.getUuid(), PackSlot.BASE, BASE.id()), "The confirmed base pack must survive a stale guard");
    }

    @Test
    @DisplayName("A guard is only armed for the request that actually pushed something")
    void testGuardIsArmedPerRequest(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ManualScheduler scheduler = new ManualScheduler();
        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), scheduler);
        service.onConfiguration(player);
        assertEquals(1, scheduler.pending.size());

        // Nothing new to push - the player already holds both packs.
        service.onConfiguration(player);
        assertEquals(1, scheduler.pending.size(), "A request that pushes nothing must not arm a guard");
    }

    @Test
    @DisplayName("A terminal status is routed to the condition of its own slot")
    void testConditionIsCalledForItsSlot(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        List<ResourcePackStatus> base = new ArrayList<>();
        List<ResourcePackStatus> season = new ArrayList<>();
        ResourcePackService service = service(settings(BASE, SEASON), BedrockDetector.never(), PackTimeoutScheduler.never()).withCondition(PackSlot.BASE, (reporter, status) -> base.add(status)).withCondition(PackSlot.SEASON, (reporter, status) -> season.add(status));
        service.onConfiguration(player);

        service.onStatus(player, BASE.id(), ResourcePackStatus.DOWNLOADED);
        assertEquals(List.of(), base, "An intermediate status is not a verdict");

        service.onStatus(player, BASE.id(), ResourcePackStatus.FAILED_DOWNLOAD);
        service.onStatus(player, SEASON.id(), ResourcePackStatus.SUCCESSFULLY_LOADED);

        assertEquals(List.of(ResourcePackStatus.FAILED_DOWNLOAD), base);
        assertEquals(List.of(ResourcePackStatus.SUCCESSFULLY_LOADED), season);
    }

    @Test
    @DisplayName("A negative timeout leaves the guard off")
    void testTimeoutCanBeDisabled(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        ManualScheduler scheduler = new ManualScheduler();
        ResourcePackService service = new ResourcePackService(new ResourcePackSettings(BASE, null, -1L, false, "."), new HeldPackRegistry(), BedrockDetector.never(), scheduler);
        service.onConfiguration(player);

        assertTrue(scheduler.pending.isEmpty());
    }
}

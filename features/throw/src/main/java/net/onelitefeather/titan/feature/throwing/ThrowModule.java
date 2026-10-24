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
package net.onelitefeather.titan.feature.throwing;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.PortalTransferGuard;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.feature.throwing.ThrowRules.Party;
import net.onelitefeather.titan.feature.throwing.ThrowRules.Refusal;
import net.onelitefeather.titan.feature.throwing.ThrowTrajectory.Trajectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lets a permitted player pick up another player and throw them. The thrower holds a charge
 * item while the target rides on their head; a release throws the target in the look direction.
 * Every way a carry ends restores the thrower's hotbar slot first.
 */
@Singleton
public final class ThrowModule {

    static final int EVENT_PRIORITY = 750;
    static final String ID = "throw";
    static final String PERMISSION = "titan.throw";
    static final String IMMUNE_PERMISSION = "titan.throw.immune";

    /** A carry lasts at most this long; then the target drops without an impulse. */
    static final Duration MAX_CARRY = Duration.ofSeconds(15);

    /** A charge started within this time of the pickup is the same click, and is ignored. */
    static final Duration PICKUP_CLICK_GRACE = Duration.ofMillis(500);

    /**
     * How often a running carry checks that the target still holds the permission to be carried.
     */
    static final int IMMUNITY_CHECK_TICKS = 20;

    private static final Logger LOG = LoggerFactory.getLogger(ThrowModule.class);
    private static final SoundEvent PICKUP_SOUND = SoundEvent.of("minecraft:entity.item.pickup", null);
    private static final SoundEvent CHARGE_SOUND = SoundEvent.of("minecraft:entity.experience_orb.pickup", null);
    private static final SoundEvent THROW_SOUND = SoundEvent.of("minecraft:entity.player.attack.knockback", null);
    private static final Particle PREVIEW_PARTICLE = Particle.END_ROD;
    private static final Particle LANDING_PARTICLE = Particle.HAPPY_VILLAGER;
    private static final Particle THROW_PARTICLE = Particle.CLOUD;

    private final EventNode<Event> titan;
    private final Scheduler scheduler;
    private final Clock clock;
    private final PermissionService permissions;
    private final PlayerSettings settings;
    private final SettingsRegistry registry;
    private final Telemetry telemetry;
    private final ThrowTelemetry metrics;
    private final ThrowMessages messages;
    private final CarryRegistry carries = new CarryRegistry();
    private final ThrowCooldowns cooldowns;
    private final FlightProtection flights = new FlightProtection();
    private final ThrowFeedback feedback;
    /**
     * When each thrower's last click was evaluated; the client's repeats within the grace are
     * ignored.
     */
    private final Map<UUID, Instant> lastPickupClick = new HashMap<>();
    private FeatureNode node;
    private Task tick;
    private long ticks;
    private boolean started;

    @Inject
    public ThrowModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Scheduler scheduler, Clock clock, Telemetry telemetry, PermissionService permissions, PlayerSettings settings, SettingsRegistry registry) {
        this(titan, scheduler, clock, telemetry, permissions, settings, registry, Player::sendActionBar);
    }

    ThrowModule(EventNode<Event> titan, Scheduler scheduler, Clock clock, Telemetry telemetry, PermissionService permissions, PlayerSettings settings, SettingsRegistry registry, ThrowFeedback.Sink actionBars) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.permissions = Objects.requireNonNull(permissions, "permissions must not be null");
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.metrics = new ThrowTelemetry(this.telemetry);
        this.messages = new ThrowMessages();
        this.cooldowns = new ThrowCooldowns(clock);
        this.feedback = new ThrowFeedback(clock, actionBars);
    }

    @PostConstruct
    void start() {
        this.registry.register(ID, ThrowSettings.TARGET);
        this.messages.register();
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PlayerEntityInteractEvent.class, this::onInteract).on(PlayerBeginItemUseEvent.class, this::onBeginUse).on(PlayerCancelItemUseEvent.class, this::onCancelUse).on(PlayerChangeHeldSlotEvent.class, this::onHeldSlot).on(PlayerPacketEvent.class, this::onPacket).on(PlayerDeathEvent.class, event -> endCarriesOf(event.getPlayer(), false)).on(PlayerDisconnectEvent.class, event -> endCarriesOf(event.getPlayer(), true)).on(RemoveEntityFromInstanceEvent.class, this::onInstanceChange);
        this.settings.addChangeListener(this::onSettingChanged);
        this.tick = this.scheduler.scheduleTask(this::tick, TaskSchedule.tick(1), TaskSchedule.tick(1));
        this.started = true;
        LOG.info("Throw column started: pickups need {}", PERMISSION);
    }

    @PreDestroy
    void stop() {
        this.started = false;
        if (this.tick != null) {
            this.tick.cancel();
        }
        if (this.node != null) {
            this.node.close();
        }
        // The hand goes back for every carry still running; nothing is thrown on the way out.
        for (Carry carry : this.carries.all()) {
            this.carries.endByThrower(carry.thrower());
            restoreAndDetach(carry);
        }
        this.messages.close();
    }

    /** The guard the portal column asks before handing a player to another server. */
    PortalTransferGuard transferGuard() {
        return playerId -> this.flights.mayTransfer(playerId, this.clock.instant());
    }

    // --- pickup -----------------------------------------------------------

    private void onInteract(PlayerEntityInteractEvent event) {
        if (event.getHand() != PlayerHand.MAIN || !(event.getTarget() instanceof Player target)) {
            return;
        }
        Player thrower = event.getPlayer();
        // One click, one pickup: while a carry runs, the client's repeats of the interact change nothing.
        if (this.carries.isCarrying(thrower.getUuid())) {
            return;
        }
        if (!thrower.getItemInMainHand().isAir() || thrower.getInstance() == null || thrower.getInstance() != target.getInstance()) {
            return;
        }
        Instant now = this.clock.instant();
        Instant lastEvaluated = this.lastPickupClick.get(thrower.getUuid());
        if (lastEvaluated != null && now.isBefore(lastEvaluated.plus(PICKUP_CLICK_GRACE))) {
            // The second packet of the same click, or a repeat while the button is held.
            return;
        }
        this.lastPickupClick.put(thrower.getUuid(), now);
        pickUp(thrower, target);
    }

    private void pickUp(Player thrower, Player target) {
        UUID throwerId = thrower.getUuid();
        UUID targetId = target.getUuid();
        Instant now = this.clock.instant();
        boolean permitted = this.permissions.check(throwerId, PERMISSION) == PermissionResult.ALLOWED;
        Optional<Refusal> refusal = ThrowRules.pickup(permitted, party(thrower, false, false, now), party(target, isImmune(targetId), !this.settings.isEnabled(targetId, ThrowSettings.TARGET), now));
        if (refusal.isEmpty() && !(this.cooldowns.throwerMayPickUp(throwerId) && this.cooldowns.targetMayBePickedUp(targetId) && this.cooldowns.pairMayPickUp(throwerId, targetId))) {
            refusal = Optional.of(Refusal.COOLDOWN);
        }
        if (refusal.isPresent()) {
            this.metrics.refused(refusal.get());
            // Without the permission the click is no feature of theirs: nothing is said.
            if (refusal.get() != Refusal.PERMISSION) {
                this.feedback.refused(thrower, refusal.get());
            }
            return;
        }
        int slot = thrower.getHeldSlot();
        ItemStack snapshot = thrower.getInventory().getItemStack(slot);
        this.carries.start(new Carry(throwerId, targetId, slot, snapshot, now, false));
        thrower.getInventory().setItemStack(slot, CarrierItem.create());
        thrower.addPassenger(target);
        this.cooldowns.targetPickedUp(targetId);
        this.metrics.pickedUp();
        thrower.playSound(Sound.sound(PICKUP_SOUND, Sound.Source.PLAYER, 1f, 1f));
    }

    /**
     * The rule input for one player; {@code carrying}, {@code carried} and {@code inFlight} come
     * from the registries.
     */
    private Party party(Player player, boolean immune, boolean optedOut, Instant now) {
        UUID id = player.getUuid();
        boolean busy = player.isFlyingWithElytra();
        boolean carried = player.getVehicle() != null || this.carries.isCarried(id);
        return new Party(busy, carried, this.carries.isCarrying(id), this.flights.inFlight(id, now), immune, optedOut);
    }

    private boolean isImmune(UUID target) {
        return this.permissions.check(target, IMMUNE_PERMISSION) == PermissionResult.ALLOWED;
    }

    // --- charge and throw ---------------------------------------------------

    private void onBeginUse(PlayerBeginItemUseEvent event) {
        if (event.getHand() != PlayerHand.MAIN || !CarrierItem.isCarrier(event.getItemStack())) {
            return;
        }
        Player thrower = event.getPlayer();
        Optional<Carry> carry = this.carries.carryOf(thrower.getUuid());
        if (carry.isEmpty() || carry.get().charging()) {
            return;
        }
        if (this.clock.instant().isBefore(carry.get().pickedAt().plus(PICKUP_CLICK_GRACE))) {
            return;
        }
        this.carries.setCharging(thrower.getUuid(), true);
    }

    private void onCancelUse(PlayerCancelItemUseEvent event) {
        if (event.getHand() != PlayerHand.MAIN || !CarrierItem.isCarrier(event.getItemStack())) {
            return;
        }
        Player thrower = event.getPlayer();
        Optional<Carry> carry = this.carries.carryOf(thrower.getUuid());
        if (carry.isEmpty() || !carry.get().charging()) {
            return;
        }
        // The release is the throw; a release without a running charge throws nothing.
        this.carries.setCharging(thrower.getUuid(), false);
        throwTarget(thrower, carry.get(), Charge.strength(event.getUseDuration()));
    }

    private void throwTarget(Player thrower, Carry carry, double strength) {
        Player target = passengerPlayer(thrower, carry.target());
        Vec impulse = ThrowVelocity.impulse(thrower.getPosition().direction(), strength);
        this.carries.endByThrower(thrower.getUuid());
        restoreAndDetach(carry);
        if (target != null) {
            target.setVelocity(impulse);
            this.flights.launch(carry.target(), this.clock.instant());
        }
        this.cooldowns.throwerThrew(thrower.getUuid());
        this.metrics.thrown();
        Instance instance = thrower.getInstance();
        if (instance != null) {
            Point at = thrower.getPosition();
            instance.playSound(Sound.sound(THROW_SOUND, Sound.Source.PLAYER, 1f, 1f), at);
            thrower.sendPacketToViewersAndSelf(new ParticlePacket(THROW_PARTICLE, at.x(), at.y() + 1, at.z(), 0.3f, 0.3f, 0.3f, 0.02f, 8));
        }
    }

    private void onHeldSlot(PlayerChangeHeldSlotEvent event) {
        Optional<Carry> carry = this.carries.carryOf(event.getPlayer().getUuid());
        if (carry.isEmpty() || !carry.get().charging()) {
            return;
        }
        // The charge is cancelled, the carry stays: the item remains in its slot.
        this.carries.setCharging(event.getPlayer().getUuid(), false);
        this.metrics.chargeAborted();
    }

    // --- ending a carry ----------------------------------------------------

    private void onPacket(PlayerPacketEvent event) {
        if (!(event.getPacket() instanceof ClientInputPacket input) || !input.shift()) {
            return;
        }
        Player target = event.getPlayer();
        Optional<Carry> carry = this.carries.carryingTarget(target.getUuid());
        if (carry.isEmpty()) {
            return;
        }
        this.carries.endByTarget(target.getUuid());
        restoreAndDetach(carry.get());
        this.cooldowns.pairEscaped(carry.get().thrower(), target.getUuid());
        this.metrics.escaped();
    }

    /**
     * A player who dies, leaves or changes instance ends every carry they are part of, without an
     * impulse.
     */
    private void endCarriesOf(Player player, boolean leaving) {
        UUID id = player.getUuid();
        this.carries.endByThrower(id).ifPresent(this::restoreAndDetach);
        this.carries.endByTarget(id).ifPresent(this::restoreAndDetach);
        if (leaving) {
            this.flights.forget(id);
            this.feedback.forget(id);
            this.lastPickupClick.remove(id);
        }
    }

    private void onInstanceChange(RemoveEntityFromInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            endCarriesOf(player, false);
        }
    }

    private void onSettingChanged(UUID player, BooleanSetting setting, boolean previous, boolean current) {
        if (!this.started || current || !ThrowSettings.TARGET.equals(setting)) {
            return;
        }
        // Switching the opt-out on frees the target at once; the thrower's hand comes back.
        this.carries.endByTarget(player).ifPresent(this::restoreAndDetach);
    }

    /** Puts the thrower's slot back as it was and lets go of the target, if both are online. */
    private void restoreAndDetach(Carry carry) {
        Player thrower = online(carry.thrower());
        Player target = online(carry.target());
        if (thrower != null) {
            thrower.getInventory().setItemStack(carry.slot(), carry.snapshot());
            if (target != null && target.getVehicle() == thrower) {
                thrower.removePassenger(target);
            }
        }
    }

    // --- per tick -----------------------------------------------------------

    private void tick() {
        this.ticks++;
        Instant now = this.clock.instant();
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            this.flights.observeGround(player.getUuid(), player.isOnGround(), now);
        }
        for (Carry carry : this.carries.all()) {
            Player thrower = online(carry.thrower());
            Player target = online(carry.target());
            if (thrower == null || target == null || thrower.getInstance() == null || thrower.getInstance() != target.getInstance()) {
                this.carries.endByThrower(carry.thrower());
                restoreAndDetach(carry);
            } else if (carry.expired(now, MAX_CARRY)) {
                this.carries.endByThrower(carry.thrower());
                restoreAndDetach(carry);
            } else if (this.ticks % IMMUNITY_CHECK_TICKS == 0 && isImmune(carry.target())) {
                this.carries.endByThrower(carry.thrower());
                restoreAndDetach(carry);
            } else if (carry.charging()) {
                showCharge(thrower, thrower.getCurrentItemUseTime());
            }
        }
        this.flights.prune(now);
    }

    private void showCharge(Player thrower, long useTicks) {
        double strength = Charge.strength(useTicks);
        thrower.sendActionBar(Component.text(ChargeFeedback.bar(strength), NamedTextColor.GREEN).appendSpace().append(ThrowMessages.text(ThrowMessages.HINT, thrower.getLocale())));
        if (ChargeFeedback.soundDue(useTicks)) {
            thrower.playSound(Sound.sound(CHARGE_SOUND, Sound.Source.PLAYER, 1f, ChargeFeedback.pitch(strength)));
        }
        if (ChargeFeedback.previewDue(useTicks)) {
            sendPreview(thrower, strength);
        }
    }

    /** The predicted flight, sent to the thrower alone. */
    private void sendPreview(Player thrower, double strength) {
        Instance instance = thrower.getInstance();
        if (instance == null) {
            return;
        }
        Vec impulse = ThrowVelocity.impulse(thrower.getPosition().direction(), strength);
        Point eyes = thrower.getPosition().add(0, thrower.getEyeHeight(), 0);
        Trajectory trajectory = ThrowTrajectory.compute(eyes, impulse, point -> instance.getBlock(point).blocksMotion());
        for (Vec point : trajectory.points()) {
            thrower.sendPacket(new ParticlePacket(PREVIEW_PARTICLE, point.x(), point.y(), point.z(), 0f, 0f, 0f, 0f, 1));
        }
        trajectory.landing().ifPresent(point -> thrower.sendPacket(new ParticlePacket(LANDING_PARTICLE, point.x(), point.y(), point.z(), 0.2f, 0.2f, 0.2f, 0f, 3)));
    }

    // --- helpers ------------------------------------------------------------

    private static Player online(UUID playerId) {
        return MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
    }

    /**
     * The carried player, found among the passengers of the thrower; {@code null} if they are gone.
     */
    private static Player passengerPlayer(Player thrower, UUID target) {
        return thrower.getPassengers().stream().filter(Player.class::isInstance).map(Player.class::cast).filter(player -> player.getUuid().equals(target)).findFirst().orElse(null);
    }
}

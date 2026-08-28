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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.deliver.DeliverType;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.deliver.ServiceAvailability;
import net.onelitefeather.titan.common.deliver.TitanServiceAvailability;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.TitanFeatures;
import net.onelitefeather.titan.common.utils.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides what happens when a player moves into a portal, and is the only place that decides it.
 *
 * <p>The order is fixed and each step can stop the delivery:
 *
 * <ol>
 * <li><b>Where are they</b> - {@link PortalIndex} answers, in one hash lookup, whether the new
 * position is inside a portal at all. This runs on every movement, so it has to be the cheap
 * step (US-7.01).</li>
 * <li><b>Did they just arrive</b> - a portal fires on entering, not on standing inside. The
 * latch is {@link Tags#PORTAL_INSIDE}, plus {@link Tags#PORTAL_COOLDOWN} as a debounce for
 * stepping in and out. Without both, a player whose switch was refused would set the portal
 * off again on their very next movement packet, several times a second.</li>
 * <li><b>May they</b> - if the portal names a feature, {@link FeatureGate} decides, exactly as
 * it decides for the navigator entry to the same destination. There is no second permission
 * path (US-7.04).</li>
 * <li><b>Is anybody home</b> - {@link ServiceAvailability} is asked before the switch, because
 * {@link Deliver} cannot report a failure afterwards. An unreachable target leaves the player
 * standing where they are, with a message (US-7.03).</li>
 * </ol>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class PortalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalService.class);

    private final PortalIndex index;
    private final Deliver deliver;
    private final FeatureGate featureGate;
    private final ServiceAvailability availability;
    private final PortalConfig config;
    private final Clock clock;

    private PortalService(PortalIndex index, Deliver deliver, FeatureGate featureGate, ServiceAvailability availability, PortalConfig config, Clock clock) {
        this.index = index;
        this.deliver = deliver;
        this.featureGate = featureGate;
        this.availability = availability;
        this.config = config;
        this.clock = clock;
    }

    /**
     * Creates a service over the given configuration, resolving and indexing its portals.
     * Unusable entries are dropped with a log line and simply do not exist afterwards.
     *
     * @param config       the portal configuration
     * @param deliver      the delivery route players are handed to
     * @param featureGate  the gate that also guards the navigator entries
     * @param availability the source that knows whether a target can take a player
     * @param clock        the time source the cooldown is measured against
     * @return a service ready to answer movements
     */
    public static PortalService create(PortalConfig config, Deliver deliver, FeatureGate featureGate, ServiceAvailability availability, Clock clock) {
        List<Portal> portals = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (PortalDefinition definition : config.portals()) {
            definition.resolve().ifPresent(portal -> {
                if (ids.add(portal.id())) {
                    portals.add(portal);
                } else {
                    LOGGER.warn("Ignoring a second portal with the id '{}'; ids identify a portal in the logs and in the re-entry latch, so they have to be unique", portal.id());
                }
            });
        }
        PortalIndex index = PortalIndex.of(portals);
        LOGGER.info("Loaded {} of {} configured portals into {} chunk columns", index.portals().size(), config.portals().size(), index.occupiedColumns());
        return new PortalService(index, deliver, featureGate, availability, config, clock);
    }

    /**
     * Creates a service wired to the CloudNet bridge for reachability and to the system clock.
     *
     * @param config      the portal configuration
     * @param deliver     the delivery route players are handed to
     * @param featureGate the gate that also guards the navigator entries
     * @return a service ready to answer movements
     */
    public static PortalService create(PortalConfig config, Deliver deliver, FeatureGate featureGate) {
        return create(config, deliver, featureGate, TitanServiceAvailability.holder(), Clock.systemUTC());
    }

    /**
     * Handles one movement of one player.
     *
     * @param player   the player who moved
     * @param position the position they moved to
     * @return what the movement did, for logging and tests
     */
    public PortalOutcome handleMove(Player player, Point position) {
        if (this.index.isEmpty()) {
            return PortalOutcome.NO_PORTAL;
        }
        Portal portal = this.index.portalAt(position);
        if (portal == null) {
            // Leaving arms the latch again - this is the only place it is cleared.
            player.removeTag(Tags.PORTAL_INSIDE);
            return PortalOutcome.NO_PORTAL;
        }
        String inside = player.getTag(Tags.PORTAL_INSIDE);
        if (portal.id().equals(inside)) {
            return PortalOutcome.ALREADY_INSIDE;
        }
        // Set before deciding: whatever the decision turns out to be, the player is standing in
        // this portal now, and a refusal must not repeat on every following movement packet.
        player.setTag(Tags.PORTAL_INSIDE, portal.id());
        long now = this.clock.millis();
        Long blockedUntil = player.getTag(Tags.PORTAL_COOLDOWN);
        // Per player, not per portal - see the limitation noted on Tags#PORTAL_COOLDOWN.
        if (blockedUntil != null && now < blockedUntil) {
            return PortalOutcome.COOLING_DOWN;
        }
        player.setTag(Tags.PORTAL_COOLDOWN, now + this.config.retriggerCooldownMillis());
        return attempt(player, portal);
    }

    /**
     * Returns the index behind this service, mainly so callers can log how many portals are live.
     *
     * @return the portal index
     */
    public PortalIndex index() {
        return this.index;
    }

    private PortalOutcome attempt(Player player, Portal portal) {
        TitanFeatures feature = portal.feature();
        if (feature != null && !this.featureGate.isVisibleTo(feature, player.getUuid())) {
            player.sendMessage(message(this.config.deniedMessage(), portal));
            return PortalOutcome.DENIED_FEATURE;
        }
        if (!isReachable(portal)) {
            LOGGER.debug("Portal '{}' did not deliver {}: target {} '{}' is not reachable", portal.id(), player.getUuid(), portal.type(), portal.target());
            player.sendMessage(message(this.config.unreachableMessage(), portal));
            return PortalOutcome.TARGET_UNREACHABLE;
        }
        try {
            this.deliver.sendPlayer(player, component(player, portal));
        } catch (RuntimeException exception) {
            // Never swallowed: the operator gets the stack trace, the player gets a sentence.
            LOGGER.warn("Portal '{}' failed to deliver {} to {} '{}'", portal.id(), player.getUuid(), portal.type(), portal.target(), exception);
            player.sendMessage(message(this.config.unreachableMessage(), portal));
            return PortalOutcome.DELIVERY_FAILED;
        }
        return PortalOutcome.DELIVERED;
    }

    private boolean isReachable(Portal portal) {
        return switch (portal.type()) {
            case TASK -> this.availability.isTaskReachable(portal.target());
            case SERVER -> this.availability.isServerReachable(portal.target());
        };
    }

    private static DeliverComponent component(Player player, Portal portal) {
        if (portal.type() == DeliverType.SERVER) {
            return DeliverComponent.serverBuilder().player(player).serverName(portal.target()).build();
        }
        return DeliverComponent.taskBuilder().player(player).taskName(portal.target()).build();
    }

    private static Component message(String template, Portal portal) {
        return MiniMessage.miniMessage().deserialize(template, Placeholder.unparsed("portal", portal.id()), Placeholder.unparsed("target", portal.target()));
    }
}

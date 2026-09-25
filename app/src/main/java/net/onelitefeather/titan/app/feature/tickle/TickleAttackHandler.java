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
package net.onelitefeather.titan.app.feature.tickle;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.tag.Tag;

/**
 * Reacts to a player attacking another player while holding a feather in either hand: applies the
 * tickle cooldown and broadcasts the tickle message to every player in the instance.
 *
 * <p>Reads "now" from an injected {@link Clock} instead of {@link System#currentTimeMillis()} and
 * parses the tickle message once per attack - sent to the {@linkplain Instance instance's} own
 * audience - instead of once per recipient.
 *
 * <p>Keeps today's observable behaviour unchanged, including its two known bugs, tracked by the
 * follow-up change {@code tickle-cooldown} rather than fixed here: the {@link SetCooldownPacket}
 * duration is a millisecond timestamp divided by 20 rather than a tick count, and the first hit
 * after the cooldown expires only clears the cooldown tag (see
 * {@link TickleCooldownRule.Decision#CLEAR_EXPIRED_TAG}) instead of tickling again.
 *
 * <p>Package-private: {@link TickleModule} is the only class outside this package that sees this
 * handler, wiring it up via {@code context.listen(EntityAttackEvent.class, ...)}.
 */
final class TickleAttackHandler implements Consumer<EntityAttackEvent> {

    /** This feature's own namespaced tag; no other module reads or writes it. */
    static final Tag<Long> COOLDOWN_EXPIRY = Tag.Long("titan:tickle/cooldown");

    private static final String TICKLE_MESSAGE = "<yellow><player> <white>tickled <yellow><target>";

    private final Clock clock;
    private final long cooldownMillis;

    TickleAttackHandler(Clock clock, long cooldownMillis) {
        this.clock = clock;
        this.cooldownMillis = cooldownMillis;
    }

    @Override
    public void accept(EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getTarget() instanceof Player target)) {
            return;
        }
        Instance instance = player.getInstance();
        if (instance == null) {
            return;
        }
        if (!hasFeatherItem(player)) {
            return;
        }

        long now = this.clock.millis();
        boolean hasCooldownTag = player.hasTag(COOLDOWN_EXPIRY);
        long cooldownExpiryMillis = hasCooldownTag ? player.getTag(COOLDOWN_EXPIRY) : 0L;

        switch (TickleCooldownRule.decide(hasCooldownTag, cooldownExpiryMillis, now)) {
            case TICKLE -> tickle(player, target, instance, now);
            case CLEAR_EXPIRED_TAG -> player.removeTag(COOLDOWN_EXPIRY);
            case ON_COOLDOWN -> {
                // still on cooldown: nothing to do
            }
        }
    }

    private void tickle(Player player, Player target, Instance instance, long now) {
        long cooldownExpiryMillis = TickleCooldownRule.expiryAfter(now, this.cooldownMillis);
        player.setTag(COOLDOWN_EXPIRY, cooldownExpiryMillis);

        SetCooldownPacket cooldownPacket = new SetCooldownPacket(player.getItemInOffHand().material().name(), (int) (cooldownExpiryMillis / 20));
        player.getPlayerConnection().sendPacket(cooldownPacket);

        Component message = MiniMessage.miniMessage().deserialize(TICKLE_MESSAGE, Placeholder.component("player", Optional.ofNullable(player.getDisplayName()).orElse(player.getName())), Placeholder.component("target", Optional.ofNullable(target.getDisplayName()).orElse(target.getName())));
        instance.sendMessage(message);
    }

    /**
     * Checks if {@code player} has a feather item in either hand.
     *
     * @param player the player to check
     * @return true if the player has a feather item, false otherwise
     */
    private static boolean hasFeatherItem(Player player) {
        return player.getItemInOffHand().material() == Material.FEATHER || player.getItemInMainHand().material() == Material.FEATHER;
    }
}

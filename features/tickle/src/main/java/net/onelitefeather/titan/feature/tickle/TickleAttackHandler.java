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
package net.onelitefeather.titan.feature.tickle;

import io.avaje.config.Config;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
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
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Reacts to a player attacking another player while holding a feather in either hand: applies the
 * tickle cooldown and broadcasts the tickle message to every player in the instance.
 *
 * <p>Reads "now" from an injected {@link Clock} instead of {@link System#currentTimeMillis()} so
 * tests can control it, and reads the configured cooldown live on every attack instead of once at
 * construction.
 */
final class TickleAttackHandler implements Consumer<EntityAttackEvent> {

    /** This feature's own namespaced tag; no other module reads or writes it. */
    static final Tag<Long> COOLDOWN_EXPIRY = Tag.Long("titan:tickle/cooldown");

    private static final String TICKLE_MESSAGE = "<yellow><player> <white>tickled <yellow><target>";

    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");
    private static final String TICKLED = "tickled";
    private static final String COOLDOWN = "cooldown";

    private final Clock clock;
    private final LongCounter attacks;

    TickleAttackHandler(Clock clock, Telemetry telemetry) {
        this.clock = clock;
        this.attacks = telemetry.meter().counterBuilder("titan.tickle.attacks").setUnit("{attack}").build();
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
            case TICKLE -> {
                tickle(player, target, instance, now);
                this.attacks.add(1, Attributes.of(RESULT, TICKLED));
            }
            case CLEAR_EXPIRED_TAG -> player.removeTag(COOLDOWN_EXPIRY);
            case ON_COOLDOWN -> this.attacks.add(1, Attributes.of(RESULT, COOLDOWN));
        }
    }

    private void tickle(Player player, Player target, Instance instance, long now) {
        long cooldownExpiryMillis = TickleCooldownRule.expiryAfter(now, Config.getLong(TickleSettings.COOLDOWN_KEY));
        player.setTag(COOLDOWN_EXPIRY, cooldownExpiryMillis);

        // Known bug: SetCooldownPacket expects ticks, but this is epoch millis / 20; deliberately not fixed yet.
        SetCooldownPacket cooldownPacket = new SetCooldownPacket(player.getItemInOffHand().material().name(), (int) (cooldownExpiryMillis / 20));
        player.getPlayerConnection().sendPacket(cooldownPacket);

        Component message = MiniMessage.miniMessage().deserialize(TICKLE_MESSAGE, Placeholder.component("player", Optional.ofNullable(player.getDisplayName()).orElse(player.getName())), Placeholder.component("target", Optional.ofNullable(target.getDisplayName()).orElse(target.getName())));
        instance.sendMessage(message);
    }

    private static boolean hasFeatherItem(Player player) {
        return player.getItemInOffHand().material() == Material.FEATHER || player.getItemInMainHand().material() == Material.FEATHER;
    }
}

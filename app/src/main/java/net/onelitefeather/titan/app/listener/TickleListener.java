/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.app.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public final class TickleListener implements Consumer<EntityAttackEvent> {

	private static final String TICKLE_MESSAGE = "<yellow><player> <white>tickled <yellow><target>";
	private final AppConfig config;

	public TickleListener(AppConfig config) {
		this.config = config;
	}

	@Override
	public void accept(EntityAttackEvent event) {
		if (!(event.getEntity() instanceof Player player) || !(event.getTarget() instanceof Player target))
			return;
		if (player.getInstance() == null)
			return;

		if (!hasFeatherItem(player))
			return;
		if (!player.hasTag(Tags.TICKLE_COOLDOWN)) {
			player.setTag(Tags.TICKLE_COOLDOWN, System.currentTimeMillis() + this.config.tickleDuration());
			SetCooldownPacket packet = new SetCooldownPacket(player.getItemInOffHand().material().name(),
					(int) ((System.currentTimeMillis() + this.config.tickleDuration()) / 20));
			player.getPlayerConnection().sendPacket(packet);
			player.getInstance().getPlayers().forEach(worldPlayer -> {
				Component message = MiniMessage.miniMessage().deserialize(TICKLE_MESSAGE,
						Placeholder.component("player",
								Optional.ofNullable(player.getDisplayName()).orElse(player.getName())),
						Placeholder.component("target",
								Optional.ofNullable(target.getDisplayName()).orElse(target.getName())));
				worldPlayer.sendMessage(message);
			});
		} else if (System.currentTimeMillis() > player.getTag(Tags.TICKLE_COOLDOWN)) {
			player.removeTag(Tags.TICKLE_COOLDOWN);
		}
	}

	/**
	 * Checks if the player has a feather item in either hand.
	 *
	 * @param player
	 *            the player to check
	 * @return true if the player has a feather item, false otherwise
	 */
	private boolean hasFeatherItem(@NotNull Player player) {
		return player.getItemInOffHand().material() == Material.FEATHER
				|| player.getItemInMainHand().material() == Material.FEATHER;
	}
}

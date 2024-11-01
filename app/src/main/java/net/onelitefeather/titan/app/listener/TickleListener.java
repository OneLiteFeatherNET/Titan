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
        if (!(event.getEntity() instanceof Player) || !(event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        Player target = (Player) event.getTarget();
        if (player.getInstance() == null) return;
        if (player.getItemInOffHand().material() == Material.FEATHER || player.getItemInMainHand().material() == Material.FEATHER) {
            if (!player.hasTag(Tags.TICKLE_COOLDOWN)) {
                player.setTag(Tags.TICKLE_COOLDOWN, System.currentTimeMillis() + this.config.tickleDuration());
                SetCooldownPacket packet = new SetCooldownPacket(
                        player.getItemInOffHand().material().id(),
                        (int) ((System.currentTimeMillis() + this.config.tickleDuration()) / 20)
                );
                player.getPlayerConnection().sendPacket(packet);
                player.getInstance().getPlayers().forEach(worldPlayer -> {
                    Component message = MiniMessage.miniMessage().deserialize(
                            TICKLE_MESSAGE,
                            Placeholder.component("player", Optional.ofNullable(player.getDisplayName()).orElse(player.getName())),
                            Placeholder.component("target", Optional.ofNullable(target.getDisplayName()).orElse(target.getName()))
                    );
                    worldPlayer.sendMessage(message);
                });
            } else if (System.currentTimeMillis() > player.getTag(Tags.TICKLE_COOLDOWN)) {
                player.removeTag(Tags.TICKLE_COOLDOWN);
            }
        }
    }
}

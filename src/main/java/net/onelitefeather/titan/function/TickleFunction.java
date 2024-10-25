package net.onelitefeather.titan.function;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.tag.Tag;

import java.util.Optional;

public final class TickleFunction {

    private static final Tag<Long> COOLDOWN = Tag.Long("tickle_cooldown");
    private static final String TICKLE_MESSAGE = "<yellow><player> <white>tickled <yellow><target>";

    private final NavigationFunction navigationFunction;

    private void tickleListener(EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        Player target = (Player) event.getTarget();
        if (player.getInstance() == null) return;
        if (player.getItemInOffHand().material() == Material.FEATHER || player.getItemInMainHand().material() == Material.FEATHER) {
            if (!player.hasTag(COOLDOWN)) {
                player.setTag(COOLDOWN, System.currentTimeMillis() + 4000);
                SetCooldownPacket packet = new SetCooldownPacket(
                        player.getItemInOffHand().material().id(),
                        (int) ((System.currentTimeMillis() + 4000) / 20)
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
            } else if (System.currentTimeMillis() > player.getTag(COOLDOWN)) {
                player.removeTag(COOLDOWN);
            }
        }
    }

    private TickleFunction(EventNode<Event> node, NavigationFunction navigationFunction) {
        this.navigationFunction = navigationFunction;
        node.addListener(EntityAttackEvent.class, this::tickleListener);
    }

    public static TickleFunction instance(EventNode<Event> node, NavigationFunction navigationFunction) {
        return new TickleFunction(node, navigationFunction);
    }

}

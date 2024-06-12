package net.onelitefeather.titan.feature

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventListener
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.SetCooldownPacket
import net.minestom.server.tag.Tag

class TickelFeature(eventNode: EventNode<Event>) {
    private val TICKLE_MESSAGE = "<yellow><player> <white>tickled <yellow><target>"
    private val cooldown: Tag<Long> = Tag.Long("tickel_cooldown")

    init {
        eventNode.addListener(EventListener.of(EntityAttackEvent::class.java, this::onEntityAttack))
    }

    private fun onEntityAttack(entityAttackEvent: EntityAttackEvent) {
        val player = entityAttackEvent.entity
        val target = entityAttackEvent.target
        if (player is Player && target is Player) {
            player.instance ?: return
            if (player.itemInOffHand.material() == Material.FEATHER || player.itemInMainHand.material() == Material.FEATHER) {
                if (!player.hasTag(cooldown)) {
                    player.setTag(cooldown, System.currentTimeMillis() + 4000)
                    val packet = SetCooldownPacket(player.itemInOffHand.material().id(),
                        ((System.currentTimeMillis() + 4000) / 20).toInt()
                    )
                    player.playerConnection.sendPacket(packet)
                    player.instance?.players?.forEach { worldPlayer ->
                        val message = MiniMessage.miniMessage().deserialize(
                            TICKLE_MESSAGE, Placeholder.component("player", player.displayName ?: player.name), Placeholder.component("target", target.displayName ?: target.name)
                        )
                        worldPlayer.sendMessage(message)
                    }
                } else if (System.currentTimeMillis() > player.getTag(cooldown)) {
                    player.removeTag(cooldown)
                }
            }
        }
    }

}
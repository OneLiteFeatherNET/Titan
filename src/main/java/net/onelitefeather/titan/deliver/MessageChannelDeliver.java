package net.onelitefeather.titan.deliver;

import net.minestom.server.entity.Player;

public class MessageChannelDeliver implements Deliver {
    @Override
    public void deliver(Player player, String server) {
        player.sendPluginMessage("titan:main", server.getBytes());
    }
}

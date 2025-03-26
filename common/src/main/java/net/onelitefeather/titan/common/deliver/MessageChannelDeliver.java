package net.onelitefeather.titan.common.deliver;

import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public final class MessageChannelDeliver implements Deliver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageChannelDeliver.class);

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        String channel = switch (component.type()) {
            case TASK -> "connect:fleet";
            case SERVER -> "connect:game_server";
            case null, default -> throw new IllegalStateException("Unexpected value: " + component.type());
        };
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(component);
            player.sendPluginMessage(channel, byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            LOGGER.error("Failed to send player message", e);
        }
    }
}

package net.onelitefeather.titan.app.testutils;

import net.minestom.server.entity.Player;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.model.DeliverComponent;

public final class DummyDeliver implements Deliver {
    @Override
    public void sendPlayer(Player player, DeliverComponent component) {

    }

    public static Deliver instance() {
        return new DummyDeliver();
    }
}

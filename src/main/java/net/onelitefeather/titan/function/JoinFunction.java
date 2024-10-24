package net.onelitefeather.titan.function;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;

public class JoinFunction {

    private static final CachedPacket SIMULATED_DISTANCE_PACKET = new CachedPacket(new UpdateSimulationDistancePacket(2));
    private final EventNode<Event> node;
    private final Pos spawn;

}

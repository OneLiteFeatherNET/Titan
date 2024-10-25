package net.onelitefeather.titan;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.commands.EndCommand;
import net.onelitefeather.titan.deliver.MessageChannelDeliver;
import net.onelitefeather.titan.function.BlockHandlerFunction;
import net.onelitefeather.titan.function.DeathFunction;
import net.onelitefeather.titan.function.ElytraFunction;
import net.onelitefeather.titan.function.JoinFunction;
import net.onelitefeather.titan.function.NavigationFunction;
import net.onelitefeather.titan.function.PreventFunction;
import net.onelitefeather.titan.function.RespawnFunction;
import net.onelitefeather.titan.function.SitFunction;
import net.onelitefeather.titan.function.TickleFunction;
import net.onelitefeather.titan.function.WorldFunction;

public final class TitanApplication {
    public static void main(String[] args) {
        var minecraftServer = MinecraftServer.init();
        var eventNode = EventNode.all("titan");
        var deliver = new MessageChannelDeliver();
        NavigationFunction navigationFunction = NavigationFunction.instance(deliver, eventNode);
        JoinFunction.instance(eventNode, WorldFunction.lobbySpawn(), navigationFunction);
        TickleFunction.instance(eventNode, navigationFunction);
        SitFunction.instance(eventNode);
        PreventFunction.instance(eventNode);
        ElytraFunction.instance(eventNode);
        DeathFunction.instance(eventNode);
        BlockHandlerFunction.registerAll();
        RespawnFunction.instance(eventNode, navigationFunction);
        MinecraftServer.getCommandManager().register(EndCommand.instance());

        minecraftServer.start("0.0.0.0", 25565);
    }
}

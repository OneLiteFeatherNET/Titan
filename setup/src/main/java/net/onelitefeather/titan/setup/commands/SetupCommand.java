package net.onelitefeather.titan.setup.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.onelitefeather.titan.common.config.AppConfigProvider;
import net.onelitefeather.titan.common.map.MapProvider;

public class SetupCommand extends Command {

    public SetupCommand(AppConfigProvider appConfigProvider, MapProvider mapProvider) {
        super("setup");
        this.setCondition(Conditions::playerOnly);
        this.addSubcommand(new AppCommand(appConfigProvider));
        this.addSubcommand(new MapCommand(mapProvider));
    }
}

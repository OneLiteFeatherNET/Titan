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

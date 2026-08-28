/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.setup;

import net.minestom.server.MinecraftServer;

public class TitanLauncher {
    public static void main(String[] args) {
        var minecraftServer = MinecraftServer.init();
        Titan.instance();
        // CloudNet passes the bind address/port via -Dservice.bind.host /
        // -Dservice.bind.port; fall back to the standalone defaults otherwise.
        String bindHost = System.getProperty("service.bind.host", "0.0.0.0");
        int bindPort = Integer.getInteger("service.bind.port", 25565);
        minecraftServer.start(bindHost, bindPort);
    }
}

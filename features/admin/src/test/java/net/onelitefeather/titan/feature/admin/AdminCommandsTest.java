/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.admin;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.mockito.Mockito;

/**
 * Cyano {@code Env} coverage for {@link AdminCommands}: {@code start()} registers {@code stop} and
 * {@code end} with unchanged permission behaviour, {@code stop()} unregisters both again.
 */
@ExtendWith(MicrotusExtension.class)
class AdminCommandsTest {

    private static Player playerWithStopPermission(TriState state) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUuid()).thenReturn(UUID.randomUUID());
        Mockito.when(player.getOrDefault(Mockito.eq(PermissionChecker.POINTER), Mockito.any())).thenReturn(PermissionChecker.always(state));
        return player;
    }

    @DisplayName("start() registers stop and end with the CommandManager")
    @Test
    void startRegistersStopAndEnd(Env env) {
        CommandManager commandManager = env.process().command();
        AdminCommands adminCommands = new AdminCommands(commandManager, Telemetry.noop());

        adminCommands.start();

        Assertions.assertTrue(commandManager.commandExists("stop"), "stop must be registered after start()");
        Assertions.assertTrue(commandManager.commandExists("end"), "end must be registered after start()");
    }

    @DisplayName("stop's condition always allows the console")
    @Test
    void stopConditionAlwaysAllowsTheConsole(Env env) {
        CommandManager commandManager = env.process().command();
        AdminCommands adminCommands = new AdminCommands(commandManager, Telemetry.noop());
        adminCommands.start();
        CommandCondition stopCondition = commandManager.getCommand("stop").getCondition();
        ConsoleSender console = commandManager.getConsoleSender();

        Assertions.assertTrue(stopCondition.canUse(console, null), "the console must always be allowed to stop the service");
    }

    @DisplayName("stop's condition allows a player with titan.command.stop")
    @Test
    void stopConditionAllowsAPlayerWithThePermission(Env env) {
        CommandManager commandManager = env.process().command();
        AdminCommands adminCommands = new AdminCommands(commandManager, Telemetry.noop());
        adminCommands.start();
        CommandCondition stopCondition = commandManager.getCommand("stop").getCondition();

        Assertions.assertTrue(stopCondition.canUse(playerWithStopPermission(TriState.TRUE), null), "a player with titan.command.stop must be allowed to stop the service");
    }

    @DisplayName("stop's condition denies a player without titan.command.stop")
    @Test
    void stopConditionDeniesAPlayerWithoutThePermission(Env env) {
        CommandManager commandManager = env.process().command();
        AdminCommands adminCommands = new AdminCommands(commandManager, Telemetry.noop());
        adminCommands.start();
        CommandCondition stopCondition = commandManager.getCommand("stop").getCondition();

        Assertions.assertFalse(stopCondition.canUse(playerWithStopPermission(TriState.FALSE), null), "a player without titan.command.stop must not be allowed to stop the service");
    }

    @DisplayName("end's condition stays disabled, even for the console")
    @Test
    void endConditionStaysDisabled(Env env) {
        CommandManager commandManager = env.process().command();
        AdminCommands adminCommands = new AdminCommands(commandManager, Telemetry.noop());
        adminCommands.start();
        CommandCondition endCondition = commandManager.getCommand("end").getCondition();
        ConsoleSender console = commandManager.getConsoleSender();

        Assertions.assertFalse(endCondition.canUse(console, null), "end must stay disabled, as it is today");
    }

    @DisplayName("@PreDestroy unregisters stop and end from the CommandManager")
    @Test
    void preDestroyUnregistersBothCommands(Env env) {
        CommandManager commandManager = env.process().command();
        AdminCommands adminCommands = new AdminCommands(commandManager, Telemetry.noop());
        adminCommands.start();

        adminCommands.stop();

        Assertions.assertFalse(commandManager.commandExists("stop"), "stop must be unregistered after @PreDestroy");
        Assertions.assertFalse(commandManager.commandExists("end"), "end must be unregistered after @PreDestroy");
    }
}

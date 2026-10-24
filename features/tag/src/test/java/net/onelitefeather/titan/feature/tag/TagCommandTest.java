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
package net.onelitefeather.titan.feature.tag;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class TagCommandTest {

    @DisplayName("The console gets a message that only players can play, and nothing changes")
    @Test
    void consoleIsToldOnlyPlayersCanPlay() {
        FakePlayerSettings settings = new FakePlayerSettings();
        CommandManager commands = new CommandManager();
        commands.register(new TagCommand(new TagOptIn(settings)));
        List<String> messages = new ArrayList<>();
        ConsoleSender console = new ConsoleSender() {
            @Override
            public void sendMessage(Component message) {
                messages.add(PlainTextComponentSerializer.plainText().serialize(message));
            }
        };

        commands.execute(console, "tag");

        Assertions.assertEquals(List.of("Only players can play tag."), messages, "the console must be told that only players can play");
    }

    @DisplayName("A player running /tag switches the tag setting on")
    @Test
    void playerRunningTagSwitchesTheSettingOn(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePlayerSettings settings = new FakePlayerSettings();
        CommandManager commands = new CommandManager();
        commands.register(new TagCommand(new TagOptIn(settings)));

        commands.execute(player, "tag");

        Assertions.assertTrue(settings.isEnabled(player.getUuid(), TagSettings.ENABLED), "/tag must write the same setting the settings menu writes");
    }
}

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
package net.onelitefeather.titan.feature.navigator;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

/**
 * The Minestom side of the staging start: the permission check, the connect through the navigator's
 * one exit, and the action bar and chat lines in the player's language.
 */
final class StagingPorts implements StagingConnect<Player>, StagingNotice<Player> {

    private final BiPredicate<Player, Destination> allowed;
    private final BiConsumer<Player, Destination> connect;

    StagingPorts(BiPredicate<Player, Destination> allowed, BiConsumer<Player, Destination> connect) {
        this.allowed = allowed;
        this.connect = connect;
    }

    @Override
    public boolean allowed(Player player, Destination destination) {
        return this.allowed.test(player, destination);
    }

    @Override
    public void connect(Player player, Destination destination) {
        this.connect.accept(player, destination);
    }

    @Override
    public void waiting(Player player, Destination destination) {
        player.sendActionBar(NavigatorMessages.text(NavigatorMessages.STAGING_STARTING, language(player), Component.text(destination.label())));
    }

    @Override
    public void timeout(Player player, Destination destination) {
        player.sendMessage(ServerPicker.stagingTimeout(destination, language(player)));
    }

    @Override
    public void unavailable(Player player) {
        player.sendMessage(ServerPicker.unavailable(language(player)));
    }

    private static Locale language(Player player) {
        return NavigatorModule.languageOf(player);
    }
}

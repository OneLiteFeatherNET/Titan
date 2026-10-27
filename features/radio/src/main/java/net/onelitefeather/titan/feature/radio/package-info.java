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
/**
 * The {@code radio} column: a hotbar item that opens the category overview; each category lists its
 * titles, the vanilla music discs and the MIDI songs from {@code radio/songs.yaml}. Each title
 * plays only for the player who started it:
 * a vanilla disc through the client's own record sound, a MIDI song as note block sounds sent
 * tick by tick. The play mode lives in the session, the volume in the player's settings.
 */
@InjectModule(name = "radioColumn", requires = {EventNode.class, Scheduler.class, Telemetry.class, PlayerSettings.class, SettingsRegistry.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"}, provides = {LobbyItem.class})
package net.onelitefeather.titan.feature.radio;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.telemetry.Telemetry;

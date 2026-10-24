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
 * The {@code tag} column: an always-open tag game for the players who switched it on in the
 * settings menu or with {@code /tag}. The player's choice is the {@code titan:tag.enabled} setting
 * of {@code lobby-settings}; the column reads it through the core {@link PlayerSettings} port only.
 */
@InjectModule(name = "tagColumn", requires = {EventNode.class, Scheduler.class, CommandManager.class, Telemetry.class, Clock.class, PlayerSettings.class, SettingsRegistry.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
package net.onelitefeather.titan.feature.tag;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.telemetry.Telemetry;

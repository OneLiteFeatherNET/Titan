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
 * The Otis settings adapter. Provides the Otis {@link SettingsStore} only when {@code otis.url} is
 * set; otherwise the settings column keeps its in-memory store.
 */
@InjectModule(name = "otisPlatform", requires = {Clock.class, Telemetry.class}, provides = {SettingsStore.class})
package net.onelitefeather.titan.platform.otis;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.onelitefeather.titan.core.settings.SettingsStore;
import net.onelitefeather.titan.core.telemetry.Telemetry;

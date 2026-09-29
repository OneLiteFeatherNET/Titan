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
 * The {@code daytime} column: the lobby's time of day follows the real wall clock. See
 * {@code docs/lobby-modules.md}, "Wie eine Column Plattform-Beans bekommt", for how a column
 * declares its platform dependencies.
 */
@InjectModule(name = "daytimeColumn", requires = {Instance.class, Scheduler.class, Clock.class})
package net.onelitefeather.titan.feature.daytime;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;

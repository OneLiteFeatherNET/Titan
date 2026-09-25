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
package net.onelitefeather.titan.app.feature.navigator;

import java.util.Set;

/**
 * Test-only entry point into {@link NavigatorModule#readEntryNames()} - the name-resolution half
 * of the exact read path {@link NavigatorModule#enable} runs for {@code navigator.entries} -
 * without needing a Minestom server: {@link
 * net.onelitefeather.titan.app.bootstrap.ConfigurationPrintMain} calls
 * {@link #resolvedEntryNames()} from a child JVM so
 * {@link net.onelitefeather.titan.app.bootstrap.ConfigurationPrecedenceTest} can cover the
 * {@code lobby-module-config} spec's "Liste von Einträgen" scenario.
 *
 * <p>Deliberately stops at the entry names, the same boundary
 * {@link NavigatorModule#readEntryNames()} draws: building a full, renderable
 * {@link net.onelitefeather.titan.app.module.navigator.NavigatorEntry} additionally resolves each
 * entry's icon via {@code Material.fromKey}, which needs Minestom's registry data - see
 * {@link NavigatorEntryValidationTest}'s Javadoc. The spec scenario only asks whether the resolved
 * entry names contain the extra entry alongside the shipped defaults, so this helper does not need
 * that registry at all.
 *
 * <p>Public, unlike {@link NavigatorModule#readEntryNames()} itself, purely so a class in another
 * package (the bootstrap test package) can call it; it adds no validation of its own.
 */
public final class NavigatorValidation {

    private NavigatorValidation() {
    }

    /**
     * @return every configured {@code navigator.entries} entry name, exactly as
     *         {@link NavigatorModule#enable} resolves them before validating and rendering each one
     */
    public static Set<String> resolvedEntryNames() {
        return NavigatorModule.readEntryNames();
    }
}

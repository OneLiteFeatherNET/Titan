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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.config.Configuration;
import java.nio.file.Path;

/**
 * Builds the lobby's {@code avaje-config} {@link Configuration} - {@code application.yaml}, its
 * active profiles, an external file, environment variables and system properties, in that rank
 * order (see
 * {@code openspec/changes/standardized-config-profiles/specs/lobby-module-config/spec.md},
 * "Overrides have a fixed rank order"). This is the single place both {@link PlatformBeans} (in
 * production) and the configuration precedence test build a {@link Configuration} from, so a test
 * exercises exactly the code path production runs, never a re-implementation of it.
 *
 * <p>An instance, not a static method: depending on this factory type - rather than calling
 * {@code Configuration.builder()} directly, or the static {@code io.avaje.config.Config} facade -
 * is what lets a test substitute its own factory later if the need ever arises, and keeps
 * {@link PlatformBeans} depending on an abstraction instead of global state (see the change's
 * "Principles": "the factory depends on a Path, not on globals").
 *
 * <p>{@code workingDir} is accepted for exactly that reason, even though {@code avaje-config}
 * itself resolves {@code application.yaml} and friends against the JVM's actual working directory,
 * not an arbitrary {@link Path} (see {@code design.md}, decision 1's spike result) - a caller must
 * therefore only call {@link #load(Path)} with a {@code workingDir} that equals the JVM's real
 * working directory (e.g. by starting the whole process with that directory, the way the
 * configuration precedence test does with {@link ProcessBuilder#directory(java.io.File)}), never
 * with an arbitrary path the JVM was not actually started in.
 */
public final class ConfigurationFactory {

    /**
     * @param workingDir the directory the lobby was started in; must equal the JVM's actual working
     *                   directory (see the class Javadoc)
     * @return a {@link Configuration} built from {@code application.yaml}, its active profiles, an
     *         external file (via {@code CONFIG_FILE}/{@code config.file}), environment variables
     *         and system properties
     */
    public Configuration load(Path workingDir) {
        return Configuration.builder().includeResourceLoading().build();
    }
}

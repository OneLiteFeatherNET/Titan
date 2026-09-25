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
import net.onelitefeather.titan.app.Titan;
import net.onelitefeather.titan.common.config.AppJsonMigration;

/**
 * Runs the lobby's full configuration bootstrap sequence: the one-time {@code app.json} &rarr;
 * {@code application.yaml} switch (see
 * {@code openspec/changes/standardized-config-profiles/design.md}, decision 4), then builds the
 * {@code avaje-config} {@link Configuration} through {@link ConfigurationFactory}.
 *
 * <p>Both {@link Titan#Titan()} (production) and {@link ConfigurationPrintMain} (the child JVM
 * {@link ConfigurationPrecedenceTest} drives) call {@link #load()}, so a test exercises exactly
 * the sequence production runs - including the migration step - never a re-implementation of it
 * (DRY).
 *
 * <p>The working directory is read exactly once, here, as {@code Path.of("").toAbsolutePath()},
 * and handed to {@link AppJsonMigration#migrate(Path)}; {@link ConfigurationFactory#load()} itself
 * always resolves {@code application.yaml} against the JVM's actual working directory (see its
 * Javadoc), so this is the one place a caller of this class needs to reason about the working
 * directory at all.
 */
public final class ConfigurationLoader {

    /**
     * Migrates a legacy {@code app.json} in the working directory, if there is one to migrate (see
     * {@link AppJsonMigration#migrate(Path)}), then builds the {@code avaje-config} {@link
     * Configuration}.
     *
     * @return the {@link Configuration} built after the migration step has run
     */
    public Configuration load() {
        Path workingDir = Path.of("").toAbsolutePath();
        new AppJsonMigration().migrate(workingDir);
        return new ConfigurationFactory().load();
    }
}

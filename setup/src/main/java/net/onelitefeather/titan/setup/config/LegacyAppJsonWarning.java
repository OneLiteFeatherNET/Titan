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
package net.onelitefeather.titan.setup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Warns once when the setup server finds a legacy {@code app.json} but no {@code
 * application.yaml} in the working directory.
 * <p>
 * The setup server does not migrate {@code app.json} itself - that one-time switch is {@code
 * common}'s {@code AppJsonMigration}, which only the lobby runs, on its own next start (see {@code
 * openspec/changes/standardized-config-profiles/design.md}, decision 4). An operator who deploys
 * the setup server before the lobby has had that next start would otherwise get no signal that
 * {@link SetupSpawnConfig#read} is still falling back to {@link SetupSpawnConfig#DEFAULTS} instead
 * of whatever {@code app.json} still carries.
 */
public final class LegacyAppJsonWarning {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyAppJsonWarning.class);

    private static final String APP_JSON = "app.json";
    private static final String APPLICATION_YAML = "application.yaml";

    private LegacyAppJsonWarning() {
    }

    /**
     * Logs one WARN naming {@code spawn.simulationDistance}'s default (see {@link
     * SetupSpawnConfig#DEFAULTS}) when {@code workingDir} has an {@code app.json} but no {@code
     * application.yaml}. Writes nothing, either way - the setup server never touches either file.
     *
     * @param workingDir the directory to check for {@code app.json}/{@code application.yaml}
     */
    public static void warnIfLegacyAppJsonPresent(Path workingDir) {
        boolean appJsonPresent = Files.exists(workingDir.resolve(APP_JSON));
        boolean applicationYamlPresent = Files.exists(workingDir.resolve(APPLICATION_YAML));
        if (appJsonPresent && !applicationYamlPresent) {
            LOGGER.warn("Found {} but no {} - the setup server does not migrate {}, the lobby does on its next start; until then, spawn.simulationDistance uses the default ({})", APP_JSON, APPLICATION_YAML, APP_JSON, SetupSpawnConfig.DEFAULTS.simulationDistance());
        }
    }
}

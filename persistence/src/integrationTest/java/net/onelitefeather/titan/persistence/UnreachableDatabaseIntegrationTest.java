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
package net.onelitefeather.titan.persistence;

import static net.onelitefeather.titan.persistence.TestSecrets.TEST_SECRET;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.avaje.config.Configuration;
import org.junit.jupiter.api.Test;

/** Needs a real TCP connect attempt and the pool's timeout, so it is not a unit test. */
class UnreachableDatabaseIntegrationTest {

    @Test
    void unreachableDatabase_failsTheStartWithoutLeakingThePassword() {
        Configuration config = Configuration.builder().load("titan/defaults/database.yaml").build();
        config.setProperty("titan.database.url", "jdbc:postgresql://127.0.0.1:1/x");
        config.setProperty("titan.database.user", "titan");
        config.setProperty("titan.database.password", TEST_SECRET);
        config.setProperty("titan.database.hikari.connectionTimeout", "250");
        config.setProperty("titan.database.hikari.initializationFailTimeout", "250");

        RuntimeException failure = assertThrows(RuntimeException.class, () -> ConfigurationProperties.scopeBuilder(config).build().close(), "start must fail when the database is unreachable");

        for (Throwable t = failure; t != null; t = t.getCause()) {
            assertFalse(String.valueOf(t.getMessage()).contains(TEST_SECRET), "password leaked in " + t.getClass().getName());
        }
    }
}

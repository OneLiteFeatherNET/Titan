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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.zaxxer.hikari.HikariConfig;
import io.avaje.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class HikariSettingsTest {


    private final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        this.logs.start();
        this.root.addAppender(this.logs);
    }

    @AfterEach
    void releaseLogs() {
        this.root.detachAppender(this.logs);
    }

    private static Configuration config(String... keyValues) {
        Configuration config = Configuration.builder().load("titan/defaults/database.yaml").build();
        config.setProperty("titan.database.url", "jdbc:postgresql://db/x");
        for (int i = 0; i < keyValues.length; i += 2) {
            config.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return config;
    }

    private static HikariConfig hikari(String... keyValues) {
        return HikariSettings.apply(DatabaseSettings.from(config(keyValues)));
    }

    private static void assertNoSecret(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            assertFalse(String.valueOf(t.getMessage()).contains(TEST_SECRET), "value leaked in " + t.getClass().getName());
        }
    }

    private void assertNoSecretLogged() {
        for (ILoggingEvent event : this.logs.list) {
            assertFalse(event.getFormattedMessage().contains(TEST_SECRET), "value leaked in log message: " + event.getFormattedMessage());
            assertFalse(String.valueOf(event.getMessage()).contains(TEST_SECRET), "value leaked in log pattern");
            for (Object arg : event.getArgumentArray() == null ? new Object[0] : event.getArgumentArray()) {
                assertFalse(String.valueOf(arg).contains(TEST_SECRET), "value leaked in a log argument");
            }
            for (IThrowableProxy t = event.getThrowableProxy(); t != null; t = t.getCause()) {
                assertFalse(String.valueOf(t.getMessage()).contains(TEST_SECRET), "value leaked in a logged throwable");
            }
        }
    }

    @Test
    void hikariProperty_landsInTheHikariConfig() {
        assertEquals(10, hikari("titan.database.hikari.maximumPoolSize", "10").getMaximumPoolSize());
    }

    @Test
    void shippedDefaults_keepTheSmallLobbyPool() {
        HikariConfig pool = hikari();

        assertEquals(4, pool.getMaximumPoolSize());
        assertEquals(5000, pool.getConnectionTimeout());
        assertEquals(5000, pool.getInitializationFailTimeout());
        assertEquals("titan", pool.getPoolName());
    }

    @Test
    void nestedHikariKey_reachesTheDriverDataSourceProperties() {
        HikariConfig pool = hikari("titan.database.hikari.dataSource.reWriteBatchedInserts", "true");

        assertEquals("true", pool.getDataSourceProperties().get("reWriteBatchedInserts").toString());
    }

    @Test
    void unknownHikariKey_failsNamingTheKeyButNotItsValue() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> hikari("titan.database.hikari.noSuchSetting", TEST_SECRET), "an unknown hikari key aborts the start");

        assertTrue(failure.getMessage().contains("noSuchSetting"), "the message names the key: " + failure.getMessage());
        assertNull(failure.getCause(), "a cause could carry the value");
        assertNoSecret(failure);
        assertNoSecretLogged();
    }

    @Test
    void invalidTypedHikariValue_failsNamingTheKeyAndLeaksNothing() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> hikari("titan.database.hikari.maximumPoolSize", TEST_SECRET));

        assertTrue(failure.getMessage().contains("maximumPoolSize"), failure.getMessage());
        assertNull(failure.getCause(), "a cause could carry the value");
        assertNoSecret(failure);
        assertNoSecretLogged();
    }

    @Test
    void everyRejectedKey_isNamedInOnePass() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> hikari("titan.database.hikari.maximumPoolSize", TEST_SECRET, "titan.database.hikari.noSuchSetting", TEST_SECRET, "titan.database.hikari.poolName", "fine"));

        assertEquals("titan.database.hikari has unknown or invalid settings: [maximumPoolSize, noSuchSetting]", failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dataSourceClassName", "dataSourceJNDI", "dataSource"})
    void settingThatBypassesTheJdbcUrl_isRejectedNamingTheKey(String key) {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> hikari("titan.database.hikari." + key, TEST_SECRET));

        assertTrue(failure.getMessage().contains(key), failure.getMessage());
        assertNoSecret(failure);
    }

    @Test
    void urlUserAndPassword_winOverTheHikariEntries() {
        HikariConfig pool = hikari("titan.database.user", "real-user", "titan.database.password", TEST_SECRET, "titan.database.hikari.jdbcUrl", "jdbc:postgresql://other/y", "titan.database.hikari.username", "other-user", "titan.database.hikari.password", "other-" + TEST_SECRET); // ggignore

        assertEquals("jdbc:postgresql://db/x", pool.getJdbcUrl());
        assertEquals("real-user", pool.getUsername());
        assertEquals(TEST_SECRET, pool.getPassword());
    }
}

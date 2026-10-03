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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.avaje.config.Configuration;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DatabaseSettingsTest {

    private static final String HBM2DDL = "titan.database.hibernate.hbm2ddl.auto";
    private static final String JPA_ACTION = "titan.database.hibernate.jakarta.persistence.schema-generation.database.action";

    private static Configuration config(String... keyValues) {
        Configuration config = Configuration.builder().load("titan/defaults/database.yaml").build();
        config.setProperty("titan.database.url", "jdbc:postgresql://db/x");
        for (int i = 0; i < keyValues.length; i += 2) {
            config.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return config;
    }

    private static IllegalStateException rejected(String... keyValues) {
        return assertThrows(IllegalStateException.class, () -> DatabaseSettings.from(config(keyValues)));
    }

    @Test
    void hibernateProperty_landsWithThePrefix() {
        Map<String, String> hibernate = DatabaseSettings.from(config("titan.database.hibernate.jdbc.batch_size", "20")).hibernateProperties();

        assertEquals("20", hibernate.get("hibernate.jdbc.batch_size"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "create", "create-drop", "UPDATE", " update "})
    void changingHbm2ddl_failsNamingTheSetting(String value) {
        IllegalStateException failure = rejected(HBM2DDL, value);

        assertTrue(failure.getMessage().contains("hbm2ddl.auto"), failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "create", "drop-and-create", "none", "UPDATE"})
    void changingTheJpaSchemaAction_failsNamingTheSetting(String value) {
        IllegalStateException failure = rejected(JPA_ACTION, value);

        assertTrue(failure.getMessage().contains("jakarta.persistence.schema-generation.database.action"), failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"validate", "none", "Validate"})
    void hbm2ddlThatKeepsValidation_isAllowed(String value) {
        Map<String, String> hibernate = DatabaseSettings.from(config(HBM2DDL, value)).hibernateProperties();

        assertEquals(value.toLowerCase(java.util.Locale.ROOT), hibernate.get("hibernate.hbm2ddl.auto"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"validate", "Validate"})
    void jpaSchemaActionValidate_isAllowed(String value) {
        Map<String, String> hibernate = DatabaseSettings.from(config(JPA_ACTION, value)).hibernateProperties();

        assertEquals("validate", hibernate.get("jakarta.persistence.schema-generation.database.action"));
    }

    @Test
    void absentSchemaTooling_isAllowed() {
        Map<String, String> hibernate = DatabaseSettings.from(config()).hibernateProperties();

        assertFalse(hibernate.containsKey("hibernate.hbm2ddl.auto"));
        assertFalse(hibernate.containsKey("jakarta.persistence.schema-generation.database.action"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"connection.url", "connection.username", "connection.password", "connection.driver_class", "connection.provider_class", "connection.datasource", "jakarta.persistence.jdbc.url", "jakarta.persistence.jdbc.user", "jakarta.persistence.jdbc.driver", "jakarta.persistence.jtaDataSource", "jakarta.persistence.nonJtaDataSource"})
    void connectionBypassingKey_isRejectedNamingTheKey(String key) {
        IllegalStateException failure = rejected("titan.database.hibernate." + key, TEST_SECRET);

        assertTrue(failure.getMessage().contains(key), failure.getMessage());
        assertFalse(failure.getMessage().contains(TEST_SECRET), "no value in the message");
    }

    @Test
    void doubledHibernatePrefix_isRejectedNamingTheKeyOnly() {
        IllegalStateException failure = rejected("titan.database.hibernate.hibernate.jdbc.batch_size", TEST_SECRET);

        assertTrue(failure.getMessage().contains("hibernate.hibernate.jdbc.batch_size"), failure.getMessage());
        assertFalse(failure.getMessage().contains(TEST_SECRET), "no value in the message");
    }

    @Test
    void connectionTuningKeys_stayAllowed() {
        Map<String, String> hibernate = DatabaseSettings.from(config("titan.database.hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION", "titan.database.hibernate.connection.provider_disables_autocommit", "true")).hibernateProperties();

        assertEquals("true", hibernate.get("hibernate.connection.provider_disables_autocommit"));
        assertTrue(hibernate.containsKey("hibernate.connection.handling_mode"));
    }

    @Test
    void hikariProperties_areDefensivelyCopied() {
        DatabaseSettings settings = DatabaseSettings.from(config());
        Properties handedOut = settings.hikari();

        handedOut.setProperty("injected", "x");

        assertFalse(settings.hikari().containsKey("injected"), "the record's own Properties must not be mutable from outside");
    }

    @Test
    void toString_showsNoCredentialsOrValues() {
        String text = DatabaseSettings.from(config("titan.database.password", TEST_SECRET, "titan.database.hikari.dataSource.password", TEST_SECRET)).toString();

        assertFalse(text.contains(TEST_SECRET), text);
    }
}

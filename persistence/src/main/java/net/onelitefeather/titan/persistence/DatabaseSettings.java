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

import io.avaje.config.Configuration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * The {@code titan.database.*} keys: the connection, and the {@code hikari} and {@code hibernate}
 * sections passed on as they are. Errors name keys, never values, because a value may be a
 * password.
 * {@code toString} shows no values either.
 */
record DatabaseSettings(String url, String user, String password, Properties hikari,
                        Map<String, String> hibernateProperties, int migrationLockTimeoutSeconds) {

    private static final String HIKARI = "titan.database.hikari";
    private static final String HIBERNATE = "titan.database.hibernate";
    private static final String MIGRATION_LOCK_TIMEOUT = "titan.database.migrationLockTimeoutSeconds";
    private static final int DEFAULT_MIGRATION_LOCK_TIMEOUT_SECONDS = 60;
    private static final String HBM2DDL = "hibernate.hbm2ddl.auto";
    private static final String JPA_SCHEMA_ACTION = "jakarta.persistence.schema-generation.database.action";
    // The lobby sets hbm2ddl.auto=validate last, so none there is overridden. The JPA key outranks
    // hbm2ddl.auto, so none there would switch validation off.
    private static final Map<String, Set<String>> SCHEMA_TOOLING_ALLOWED = Map.of(HBM2DDL, Set.of("validate", "none"), JPA_SCHEMA_ACTION, Set.of("validate"));
    // These would open a connection next to the pool; the connection always comes from Hikari.
    private static final Set<String> CONNECTION_BYPASS = Set.of("hibernate.connection.url", "hibernate.connection.username", "hibernate.connection.password", "hibernate.connection.driver_class", "hibernate.connection.provider_class", "hibernate.connection.datasource", "jakarta.persistence.jtaDataSource", "jakarta.persistence.nonJtaDataSource");
    private static final String JPA_PREFIX = "jakarta.";
    private static final String JPA_JDBC_PREFIX = "jakarta.persistence.jdbc.";
    private static final String HIBERNATE_PREFIX = "hibernate.";

    DatabaseSettings {
        hikari = copyOf(hikari);
        hibernateProperties = Map.copyOf(hibernateProperties);
    }

    /**
     * @throws IllegalStateException if the url is empty ({@code @RequiresProperty} treats an empty
     *                               value as set, so say so instead of failing inside the pool), or
     *                               a hibernate setting is not allowed
     */
    static DatabaseSettings from(Configuration config) {
        String url = config.get(DatabaseProperties.URL);
        if (url.isBlank()) {
            throw new IllegalStateException(DatabaseProperties.URL + " is set but empty; unset it to run without a database");
        }
        return new DatabaseSettings(url, config.getOptional("titan.database.user").orElse(null), config.getOptional("titan.database.password").orElse(null), config.forPath(HIKARI).asProperties(), hibernateProperties(config.forPath(HIBERNATE).asProperties()), migrationLockTimeoutSeconds(config));
    }

    private static int migrationLockTimeoutSeconds(Configuration config) {
        int seconds = config.getInt(MIGRATION_LOCK_TIMEOUT, DEFAULT_MIGRATION_LOCK_TIMEOUT_SECONDS);
        if (seconds < 1) {
            throw new IllegalStateException(MIGRATION_LOCK_TIMEOUT + " must be at least 1");
        }
        return seconds;
    }

    @Override
    public Properties hikari() {
        return copyOf(this.hikari);
    }

    private static Properties copyOf(Properties source) {
        Properties copy = new Properties();
        copy.putAll(source);
        return copy;
    }

    // Keys are relative to the section; Hibernate's own jakarta.* keys keep their name, the rest gets "hibernate.".
    private static Map<String, String> hibernateProperties(Properties section) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (String key : section.stringPropertyNames()) {
            String setting = HIBERNATE + "." + key;
            if (key.startsWith(HIBERNATE_PREFIX)) {
                throw new IllegalStateException(setting + " repeats the hibernate. prefix; name it without");
            }
            String name = key.startsWith(JPA_PREFIX) ? key : HIBERNATE_PREFIX + key;
            if (CONNECTION_BYPASS.contains(name) || name.startsWith(JPA_JDBC_PREFIX)) {
                throw new IllegalStateException(setting + " is not allowed: the connection always comes from the pool");
            }
            properties.put(name, schemaToolingChecked(name, setting, section.getProperty(key)));
        }
        return Map.copyOf(properties);
    }

    private static String schemaToolingChecked(String name, String setting, String value) {
        Set<String> allowed = SCHEMA_TOOLING_ALLOWED.get(name);
        if (allowed == null) {
            return value;
        }
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalised)) {
            throw new IllegalStateException(setting + " must be one of " + new TreeSet<>(allowed) + ": Flyway owns the schema");
        }
        return normalised;
    }

    @Override
    public String toString() {
        return "DatabaseSettings[hikariKeys=" + List.copyOf(this.hikari.stringPropertyNames()) + ", hibernateKeys=" + List.copyOf(this.hibernateProperties.keySet()) + "]";
    }
}

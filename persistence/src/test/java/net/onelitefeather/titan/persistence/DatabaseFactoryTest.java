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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.avaje.config.Configuration;
import io.avaje.inject.BeanScope;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.junit.jupiter.api.Test;

class DatabaseFactoryTest {

    private static BeanScope scopeOf(Configuration config) {
        return ConfigurationProperties.scopeBuilder(config).build();
    }

    private static Configuration shipped() {
        return Configuration.builder().load("titan/defaults/database.yaml").build();
    }

    @Test
    void withoutUrl_createsNeitherDataSourceNorSessionFactory() {
        try (BeanScope scope = scopeOf(shipped())) {
            assertTrue(scope.getOptional(DataSource.class).isEmpty(), "no DataSource without titan.database.url");
            assertTrue(scope.getOptional(SessionFactory.class).isEmpty(), "no SessionFactory without titan.database.url");
        }
    }

    @Test
    void withEmptyUrl_failsTheStartAndNamesTheKey() {
        Configuration config = shipped();
        config.setProperty("titan.database.url", "");

        RuntimeException failure = assertThrows(RuntimeException.class, () -> scopeOf(config).close(), "an empty url is a misconfiguration");

        assertTrue(rootCause(failure).getMessage().contains("titan.database.url"), "the message names the key");
    }

    @Test
    void yamlHbm2ddlNone_isOverriddenByValidate() {
        Configuration config = shipped();
        config.setProperty("titan.database.url", "jdbc:postgresql://db/x");
        config.setProperty("titan.database.hibernate.hbm2ddl.auto", "none");

        try (HikariDataSource pool = new HikariDataSource()) {
            HibernatePersistenceConfiguration hibernate = DatabaseFactory.hibernateConfiguration(DatabaseSettings.from(config), pool, List.of());

            assertEquals("validate", hibernate.properties().get("hibernate.hbm2ddl.auto"), "Flyway owns the schema, Hibernate only validates");
            assertSame(pool, hibernate.properties().get("jakarta.persistence.nonJtaDataSource"), "the connection comes from the pool");
        }
    }

    @Test
    void yamlCannotReplaceThePoolDataSource() {
        Configuration config = shipped();
        config.setProperty("titan.database.url", "jdbc:postgresql://db/x");
        config.setProperty("titan.database.hibernate.jakarta.persistence.nonJtaDataSource", "other");

        assertThrows(IllegalStateException.class, () -> DatabaseSettings.from(config), "a second data source is rejected");
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable t = failure;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}

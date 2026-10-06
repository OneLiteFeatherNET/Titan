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
package net.onelitefeather.titan.feature.jumprun.persistence;

import io.avaje.config.Configuration;
import io.avaje.inject.BeanScope;
import io.avaje.inject.spi.GenericType;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.RegistriesTest;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.persistence.ConfigurationProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The column and the persistence module, discovered like in production, on a database that only
 * this test sees: one server per class, one freshly created database per test, so order does not
 * matter. What the runtime provides to the column is stubbed.
 */
// CI runners on macOS and Windows have no Docker; skip instead of failing there.
@Testcontainers(disabledWithoutDocker = true)
@RegistriesTest
public abstract class JumprunDatabaseTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private String database;
    private BeanScope scope;

    @BeforeEach
    void createFreshDatabase() throws SQLException {
        this.database = "t_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()); Statement statement = admin.createStatement()) {
            statement.execute("create database " + this.database);
        }
    }

    @AfterEach
    void closeScope() {
        if (this.scope != null) {
            this.scope.close();
        }
    }

    /**
     * Builds the scope; the migration runs and Hibernate validates the entity before it returns.
     */
    protected BeanScope start() {
        Configuration config = Configuration.builder().load("titan/defaults/database.yaml").build();
        config.setProperty("titan.database.url", url());
        config.setProperty("titan.database.user", POSTGRES.getUsername());
        config.setProperty("titan.database.password", POSTGRES.getPassword());
        return start(config);
    }

    /** Builds the scope of a lobby that configured no database: the shipped defaults only. */
    protected BeanScope startWithoutDatabase() {
        return start(Configuration.builder().load("titan/defaults/database.yaml").build());
    }

    private BeanScope start(Configuration config) {
        EventNode<Event> titan = EventNode.all(FeatureNode.TITAN_NODE);
        this.scope = ConfigurationProperties.scopeBuilder(config).bean(FeatureNode.TITAN_NODE, new GenericType<EventNode<Event>>() {
        }.type(), titan).bean(LobbySpawn.class, unused(LobbySpawn.class)).bean(LobbyPortals.class, unused(LobbyPortals.class)).bean(LobbyHeightBounds.class, unused(LobbyHeightBounds.class)).bean(Clock.class, Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)).bean(Scheduler.class, Scheduler.newScheduler()).bean(Telemetry.class, Telemetry.noop()).build();
        return this.scope;
    }

    /** A plain JDBC connection to this test's database, for looking at what the migration made. */
    Connection connect() throws SQLException {
        return DriverManager.getConnection(url(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private String url() {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/" + this.database;
    }

    @SuppressWarnings("unchecked")
    private static <T> T unused(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (_, method, _) -> {
            throw new UnsupportedOperationException(method.getName());
        });
    }
}

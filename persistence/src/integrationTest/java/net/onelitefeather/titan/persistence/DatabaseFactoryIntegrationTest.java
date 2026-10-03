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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zaxxer.hikari.HikariDataSource;
import io.avaje.config.Configuration;
import io.avaje.inject.BeanScope;
import io.avaje.inject.BeanScopeBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// CI runners on macOS and Windows have no Docker; skip instead of failing there.
@Testcontainers(disabledWithoutDocker = true)
class DatabaseFactoryIntegrationTest {

    private static final PersistenceUnit WIDGETS = new PersistenceUnit("widgets", List.of(Widget.class));
    private static final PersistenceUnit BROKEN = new PersistenceUnit("broken", List.of(BrokenWidget.class));

    // One server per class; every test gets its own database on it, so order does not matter.
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private String database;

    @BeforeEach
    void createFreshDatabase() throws SQLException {
        this.database = "t_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()); Statement statement = admin.createStatement()) {
            statement.execute("create database " + this.database);
        }
    }

    private String url() {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/" + this.database;
    }

    private BeanScope start(PersistenceUnit... units) {
        return start(configuration(), units);
    }

    // Built on the calling thread: loading the yaml from several threads at once is not safe.
    private Configuration configuration() {
        Configuration config = Configuration.builder().load("titan/defaults/database.yaml").build();
        config.setProperty("titan.database.url", url());
        config.setProperty("titan.database.user", POSTGRES.getUsername());
        config.setProperty("titan.database.password", POSTGRES.getPassword());
        return config;
    }

    private static BeanScope start(Configuration config, PersistenceUnit... units) {
        BeanScopeBuilder builder = ConfigurationProperties.scopeBuilder(config);
        for (PersistenceUnit unit : units) {
            builder.bean(unit.name(), PersistenceUnit.class, unit);
        }
        return builder.build();
    }

    private List<String> history(String unit) throws SQLException {
        List<String> versions = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url(), POSTGRES.getUsername(), POSTGRES.getPassword()); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("select version from flyway_" + unit + "_history where version is not null order by installed_rank")) {
            while (rows.next()) {
                versions.add(rows.getString(1));
            }
        }
        return versions;
    }

    @Test
    void emptyDatabase_getsTheTableAndAValidatedSessionFactory() throws SQLException {
        try (BeanScope scope = start(WIDGETS)) {
            SessionFactory sessions = scope.get(SessionFactory.class);

            sessions.inTransaction(session -> session.persist(new Widget(1L, "first")));
            Long count = sessions.fromTransaction(session -> session.createSelectionQuery("select count(w) from Widget w", Long.class).getSingleResult());
            assertEquals(1L, count, "the migrated table is usable through Hibernate");
        }
        assertEquals(List.of("1"), history("widgets"));
    }

    @Test
    void secondStart_appliesNoMigrationAgainAndKeepsTheData() throws SQLException {
        try (BeanScope first = start(WIDGETS)) {
            first.get(SessionFactory.class).inTransaction(session -> session.persist(new Widget(1L, "kept")));
        }

        try (BeanScope second = start(WIDGETS)) {
            Long count = second.get(SessionFactory.class).fromTransaction(session -> session.createSelectionQuery("select count(w) from Widget w", Long.class).getSingleResult());
            assertEquals(1L, count, "existing data survives a second start");
        }
        assertEquals(List.of("1"), history("widgets"), "version 1 is recorded exactly once");
    }

    // Soak check, repeated on a fresh database each time. The window it covers is too narrow to hit
    // reliably; migration_waitsWhileAnotherStartHoldsTheStartupLock pins the guarantee deterministically.
    @RepeatedTest(5)
    void fourStartsAtTheSameTime_applyEveryVersionExactlyOnce() throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        List<CompletableFuture<Void>> starts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Configuration config = configuration();
            starts.add(CompletableFuture.runAsync(() -> {
                try {
                    assertTrue(go.await(30, TimeUnit.SECONDS), "the start gate opens");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                try (BeanScope scope = start(config, WIDGETS)) {
                    assertTrue(scope.getOptional(SessionFactory.class).isPresent(), "both starts must come up");
                }
            }));
        }
        go.countDown();
        for (CompletableFuture<Void> start : starts) {
            start.get(60, TimeUnit.SECONDS);
        }

        assertEquals(List.of("1"), history("widgets"), "the migration ran once although four starts raced");
    }

    @Test
    void migration_waitsWhileAnotherStartHoldsTheStartupLock() throws Exception {
        try (Connection holder = DriverManager.getConnection(url(), POSTGRES.getUsername(), POSTGRES.getPassword()); Statement statement = holder.createStatement(); HikariDataSource pool = new HikariDataSource(HikariSettings.apply(DatabaseSettings.from(configuration())))) {
            statement.execute("select pg_advisory_lock(" + SchemaMigrations.LOCK_KEY + ")");
            CompletableFuture<SchemaMigrations> migration = CompletableFuture.supplyAsync(() -> SchemaMigrations.migrate(pool, List.of(WIDGETS)));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!advisoryLockWaiterExists(statement)) {
                assertTrue(System.nanoTime() < deadline && !migration.isDone(), "the migration must queue on the startup lock instead of running past it");
                Thread.onSpinWait();
            }
            assertFalse(migration.isDone(), "no migration step runs while the lock is held");

            statement.execute("select pg_advisory_unlock(" + SchemaMigrations.LOCK_KEY + ")");
            assertEquals(1, migration.get(30, TimeUnit.SECONDS).applied(), "the queued migration runs once the lock is free");
        }
        assertEquals(List.of("1"), history("widgets"));
    }

    private static boolean advisoryLockWaiterExists(Statement statement) throws SQLException {
        try (ResultSet rows = statement.executeQuery("select count(*) from pg_locks where locktype = 'advisory' and not granted")) {
            rows.next();
            return rows.getInt(1) > 0;
        }
    }

    @Test
    void entityFieldWithoutAColumn_abortsTheStart() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> start(BROKEN).close(), "validate must reject a mapping the schema does not have");

        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertTrue(root.getMessage().contains("broken_widget"), "the failure names the table: " + root.getMessage());
    }

    @Test
    void hikariMaximumPoolSize_reachesTheRunningPool() {
        Configuration config = configuration();
        config.setProperty("titan.database.hikari.maximumPoolSize", "10");

        try (BeanScope scope = start(config, WIDGETS)) {
            assertEquals(10, scope.get(HikariDataSource.class).getMaximumPoolSize(), "the configured pool size is the running one");
        }
    }

    @Test
    void hibernateSetting_isVisibleInTheSessionFactory() {
        Configuration config = configuration();
        config.setProperty("titan.database.hibernate.jdbc.batch_size", "20");

        try (BeanScope scope = start(config, WIDGETS)) {
            assertEquals("20", String.valueOf(scope.get(SessionFactory.class).getProperties().get("hibernate.jdbc.batch_size")), "batch size reaches Hibernate");
        }
    }

    @Test
    void hbm2ddlUpdate_abortsTheStartNamingTheSetting() {
        Configuration config = configuration();
        config.setProperty("titan.database.hibernate.hbm2ddl.auto", "update");

        RuntimeException failure = assertThrows(RuntimeException.class, () -> start(config, WIDGETS).close(), "Flyway owns the schema, so update is refused");

        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertTrue(root.getMessage().contains("hbm2ddl.auto"), "the failure names the setting: " + root.getMessage());
    }

    @Test
    void unitsKeepSeparateHistories() throws SQLException {
        PersistenceUnit other = new PersistenceUnit("gadgets", List.of(Gadget.class));

        try (BeanScope scope = start(WIDGETS, other)) {
            assertTrue(scope.getOptional(DataSource.class).isPresent());
        }
        assertEquals(List.of("1"), history("widgets"));
        assertEquals(List.of("0", "1"), history("gadgets"), "the second unit is baselined at 0 because the schema already holds the first unit's tables");
    }

    @Test
    void successfulStart_logsDatabaseReadyOnceWithoutCredentials() {
        Logger logger = (Logger) LoggerFactory.getLogger(DatabaseReady.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try (BeanScope scope = start(WIDGETS)) {
            assertTrue(scope.getOptional(SessionFactory.class).isPresent());
        } finally {
            logger.detachAppender(appender);
        }

        List<ILoggingEvent> ready = appender.list.stream().filter(event -> event.getFormattedMessage().equals("Database ready")).toList();
        assertEquals(1, ready.size(), "one 'Database ready' line");
        assertEquals(Level.INFO, ready.getFirst().getLevel(), "logged at INFO");
        Map<String, Object> keyValues = ready.getFirst().getKeyValuePairs().stream().collect(Collectors.toMap(kv -> kv.key, kv -> kv.value));
        assertEquals(Map.of("units", 1, "migrations", 1), keyValues, "only units and migrations, never url, user or password");
        appender.list.forEach(event -> {
            assertFalse(event.getFormattedMessage().contains(POSTGRES.getPassword()), "no password in the log");
            assertFalse(event.getFormattedMessage().contains(this.database), "no url in the log");
        });
    }

    @Test
    void anotherExecutorInTheScope_doesNotKeepTheWriterAway() throws Exception {
        // The runtime always provides Minestom's Scheduler, which is an Executor.
        Executor runtimeExecutor = Runnable::run;
        BeanScopeBuilder builder = ConfigurationProperties.scopeBuilder(configuration()).bean(Executor.class, runtimeExecutor);
        builder.bean(WIDGETS.name(), PersistenceUnit.class, WIDGETS);

        try (BeanScope scope = builder.build()) {
            DatabaseWriter writer = scope.getOptional(DatabaseWriter.class).orElseThrow(() -> new AssertionError("the writer must exist next to another Executor bean"));
            CompletableFuture<Boolean> ran = new CompletableFuture<>();
            writer.execute(() -> ran.complete(true));
            assertTrue(ran.get(10, TimeUnit.SECONDS), "the writer runs tasks");
            assertSame(runtimeExecutor, scope.get(Executor.class), "the runtime's executor stays the Executor");
        }
    }

    @Test
    void closingTheScope_drainsTheWriterBeforeTheSessionFactoryCloses() throws Exception {
        BeanScope scope = start(WIDGETS);
        SessionFactory sessions = scope.get(SessionFactory.class);
        DatabaseWriter writer = scope.get(DatabaseWriter.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Boolean> factoryOpenForTheWrite = new CompletableFuture<>();
        writer.execute(() -> {
            entered.countDown();
            try {
                release.await();
                sessions.inTransaction(session -> session.persist(new Widget(7L, "late")));
                factoryOpenForTheWrite.complete(sessions.isOpen());
            } catch (Throwable failure) {
                factoryOpenForTheWrite.completeExceptionally(failure);
            }
        });
        assertTrue(entered.await(10, TimeUnit.SECONDS), "the write is running");

        Thread closer = Thread.ofPlatform().start(scope::close);
        // Parked inside DatabaseWriter.close(): the factory has not been closed before it, or the
        // closer would be past it already. Only now may the write go on.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (Arrays.stream(closer.getStackTrace()).noneMatch(frame -> frame.getClassName().equals(DatabaseWriter.class.getName()) && frame.getMethodName().equals("close"))) {
            assertTrue(closer.isAlive() && System.nanoTime() < deadline, "scope.close() must block in DatabaseWriter.close() while a write is pending");
            Thread.onSpinWait();
        }
        assertFalse(sessions.isClosed(), "the factory is still open while the writer drains");
        release.countDown();

        assertTrue(factoryOpenForTheWrite.get(10, TimeUnit.SECONDS), "the pending write ran against an open factory");
        closer.join(TimeUnit.SECONDS.toMillis(10));
        assertFalse(closer.isAlive(), "the scope finished closing");
        assertTrue(sessions.isClosed(), "the factory is closed after the writer drained");
        try (Connection connection = DriverManager.getConnection(url(), POSTGRES.getUsername(), POSTGRES.getPassword()); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("select count(*) from widget where id = 7")) {
            rows.next();
            assertEquals(1, rows.getInt(1), "the late write reached the database");
        }
    }
}

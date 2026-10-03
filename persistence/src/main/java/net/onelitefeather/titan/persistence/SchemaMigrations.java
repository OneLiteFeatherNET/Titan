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

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * The outcome of migrating every unit. Being a bean, it orders the migrations before the {@code
 * SessionFactory} validates the schema.
 *
 * @param units   number of units migrated
 * @param applied number of migrations that were applied just now
 */
record SchemaMigrations(int units, int applied) {

    // "TITANMIG" as a long; every lobby must use the same key for the lock to exclude anything.
    private static final int MINIMUM_POOL_SIZE = 2;

    static final long LOCK_KEY = 0x5449_5441_4E4D_4947L;

    /**
     * Runs every unit's migrations while holding one database-wide lock, so lobbies that start
     * together apply each version once and never see each other half-way.
     */
    static SchemaMigrations migrate(HikariDataSource dataSource, List<PersistenceUnit> units) {
        // Flyway locks only its own migration step. Its checks before that (does the history table
        // exist, is the schema empty, baseline or create the table) run unlocked, so a second start
        // can decide on a stale view and fail on the first one's table. A session lock around the
        // whole run closes that window; it blocks without Flyway's one-second polling and retry
        // limit, and a crashed holder frees it by dropping the connection. The lock connection is
        // opened outside the pool so a pool of one connection still has one left for Flyway.
        // Flyway itself holds two pooled connections (main and migration), so a smaller pool can
        // never migrate; say so up front instead of timing out in Flyway.
        if (dataSource.getMaximumPoolSize() < MINIMUM_POOL_SIZE) {
            throw new IllegalStateException("titan.database.hikari.maximumPoolSize must be at least " + MINIMUM_POOL_SIZE + " because Flyway needs two connections");
        }
        try (Connection lock = DriverManager.getConnection(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword())) {
            execute(lock, "select pg_advisory_lock(" + LOCK_KEY + ")");
            try {
                return migrateAll(dataSource, units);
            } finally {
                execute(lock, "select pg_advisory_unlock(" + LOCK_KEY + ")");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not take the schema migration lock", e);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static SchemaMigrations migrateAll(DataSource dataSource, List<PersistenceUnit> units) {
        int applied = 0;
        for (PersistenceUnit unit : units) {
            // The schema is shared: a unit's first run finds other units' tables but no history of
            // its own, so baseline it at 0 instead of refusing to start. Version 0 must stay free:
            // V1 only runs above the baseline, so no unit may ship a V0 migration.
            applied += Flyway.configure().dataSource(dataSource).locations(unit.migrationLocation()).table(unit.historyTable()).baselineOnMigrate(true).baselineVersion("0").load().migrate().migrationsExecuted;
        }
        return new SchemaMigrations(units.size(), applied);
    }
}

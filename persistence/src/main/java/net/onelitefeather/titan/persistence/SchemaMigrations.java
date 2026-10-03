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

    private static final int MINIMUM_POOL_SIZE = 3;

    /**
     * Runs every unit's migrations while holding one database-wide lock, so lobbies that start
     * together apply each version once and never see each other half-way.
     *
     * @param lockTimeoutSeconds how long to wait for another lobby's migration before failing
     */
    static SchemaMigrations migrate(HikariDataSource dataSource, List<PersistenceUnit> units, int lockTimeoutSeconds) {
        // Flyway locks only its own migration step. Its checks before that (does the history table
        // exist, is the schema empty, baseline or create the table) run unlocked, so a second start
        // can decide on a stale view and fail on the first one's table. A lock around the whole run
        // closes that window. Flyway holds two pooled connections and the lock one, so a smaller
        // pool would time out inside Flyway; say so up front.
        if (dataSource.getMaximumPoolSize() < MINIMUM_POOL_SIZE) {
            throw new IllegalStateException("titan.database.hikari.maximumPoolSize must be at least " + MINIMUM_POOL_SIZE + " because Flyway needs two connections and the startup lock one");
        }
        // try-with-resources keeps a failed release as suppressed instead of hiding the migration failure.
        try (StartupLock lock = StartupLock.acquire(dataSource, lockTimeoutSeconds)) {
            return migrateAll(dataSource, units);
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

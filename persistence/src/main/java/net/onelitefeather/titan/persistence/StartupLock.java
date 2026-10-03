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
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A PostgreSQL session advisory lock that serializes the schema migration of lobbies starting
 * together. PostgreSQL is required. The lock belongs to one database session, so it does not work
 * behind PgBouncer in transaction pooling mode, which hands every statement a different session.
 * A crashed holder frees the lock by dropping its connection.
 */
final class StartupLock implements AutoCloseable {

    // "TITANMIG" as a long; every lobby must use the same key for the lock to exclude anything.
    static final long KEY = 0x5449_5441_4E4D_4947L;

    // SQLSTATE lock_not_available, raised when lock_timeout expires.
    private static final String LOCK_NOT_AVAILABLE = "55P03";

    private final HikariDataSource pool;
    private final Connection connection;

    private StartupLock(HikariDataSource pool, Connection connection) {
        this.pool = pool;
        this.connection = connection;
    }

    /**
     * Waits for the lock on one pooled connection.
     *
     * @throws IllegalStateException if the lock stays held for more than {@code timeoutSeconds}, or
     *                               cannot be taken at all
     */
    static StartupLock acquire(HikariDataSource pool, int timeoutSeconds) {
        Connection connection = null;
        try {
            connection = pool.getConnection();
            // Bounds the wait: the lock call itself would block for as long as the holder lives.
            execute(connection, "set lock_timeout = " + timeoutSeconds * 1000);
            execute(connection, "select pg_advisory_lock(" + KEY + ")");
            execute(connection, "reset lock_timeout");
            return new StartupLock(pool, connection);
        } catch (SQLException e) {
            evict(pool, connection);
            if (LOCK_NOT_AVAILABLE.equals(e.getSQLState())) {
                throw new IllegalStateException("another lobby has held the schema migration lock for more than " + timeoutSeconds + " s", e);
            }
            throw new IllegalStateException("Could not take the schema migration lock", e);
        }
    }

    @Override
    public void close() {
        try {
            execute(this.connection, "select pg_advisory_unlock(" + KEY + ")");
            this.connection.close();
        } catch (SQLException e) {
            // Never hand a connection that may still hold the lock back to the pool.
            evict(this.pool, this.connection);
            throw new IllegalStateException("Could not release the schema migration lock", e);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void evict(HikariDataSource pool, Connection connection) {
        if (connection != null) {
            pool.evictConnection(connection);
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Already evicted; nothing left to release.
            }
        }
    }
}

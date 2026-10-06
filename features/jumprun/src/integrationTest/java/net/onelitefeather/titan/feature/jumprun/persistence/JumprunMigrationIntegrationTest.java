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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

class JumprunMigrationIntegrationTest extends JumprunDatabaseTest {

    @Test
    void emptyDatabase_getsTheTableAndHibernateValidatesTheEntityAgainstIt() {
        // start() throws when the entity and the migrated table disagree (hbm2ddl=validate).
        assertNotNull(start().get(SessionFactory.class), "a validated SessionFactory exists");
    }

    @Test
    void table_hasTheColumnsOfTheDesign() throws SQLException {
        start();

        Map<String, String> columns = new LinkedHashMap<>();
        try (Connection connection = connect(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("select column_name, data_type || ':' || coalesce(character_maximum_length::text, '') || ':' || is_nullable from information_schema.columns where table_name = 'jumprun_run' order by ordinal_position")) {
            while (rows.next()) {
                columns.put(rows.getString(1), rows.getString(2));
            }
        }

        assertEquals(Map.of("id", "bigint::NO", "player_uuid", "uuid::NO", "player_name", "character varying:32:NO", "mode", "character varying:16:NO", "score", "integer::NO", "end_reason", "character varying:16:NO", "finished_at", "timestamp with time zone::NO"), columns, "column names, types, lengths and nullability");
    }

    @Test
    void table_hasBothIndexesInTheDesignedOrder() throws SQLException {
        start();

        Map<String, String> indexes = new LinkedHashMap<>();
        try (Connection connection = connect(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("select indexname, indexdef from pg_indexes where tablename = 'jumprun_run' and indexname like 'jumprun_run_%' and indexname <> 'jumprun_run_pkey'")) {
            while (rows.next()) {
                indexes.put(rows.getString(1), rows.getString(2));
            }
        }

        assertEquals(2, indexes.size(), "exactly the two designed indexes besides the primary key: " + indexes);
        assertTrue(indexes.get("jumprun_run_player_mode").endsWith("(player_uuid, mode, score DESC)"), "player index: " + indexes);
        assertTrue(indexes.get("jumprun_run_mode_player_best").endsWith("(mode, player_uuid, score DESC, finished_at)"), "mode index: " + indexes);
    }

    @Test
    void negativeScore_isRejectedByTheDatabase() throws SQLException {
        start();

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            SQLException rejected = assertThrows(SQLException.class, () -> statement.execute("insert into jumprun_run (player_uuid, player_name, mode, score, end_reason, finished_at) values (gen_random_uuid(), 'Alex', 'HARD', -1, 'FALL', now())"), "the check constraint refuses score < 0");
            assertEquals("23514", rejected.getSQLState(), "SQLSTATE check_violation");
        }
    }
}

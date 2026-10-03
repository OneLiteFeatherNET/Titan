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
package net.onelitefeather.titan.feature.jumprun;

import io.avaje.inject.External;
import io.avaje.inject.RequiresProperty;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.persistence.Tuple;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.onelitefeather.titan.persistence.DatabaseProperties;
import org.hibernate.SessionFactory;

/** {@link RunStore} on the shared PostgreSQL, through the {@code jumprun_run} table. */
@Singleton
@RequiresProperty(DatabaseProperties.URL)
final class HibernateRunStore implements RunStore {

    /**
     * HQL cannot say DISTINCT ON or a lateral lookup, so the ranking is native. Per mode and
     * player the best run wins, the earlier one on equal scores; the name comes from the player's
     * latest run. Ties between players resolve like {@link TopThree}: earlier, then player id.
     */
    private static final String TOP_THREE_OF_EVERY_MODE = """
            with best as (
                select distinct on (mode, player_uuid) mode, player_uuid, score, finished_at
                from jumprun_run
                order by mode, player_uuid, score desc, finished_at
            ), ranked as (
                select mode, player_uuid, score, finished_at,
                       row_number() over (partition by mode order by score desc, finished_at, player_uuid) as place
                from best
            )
            select r.mode, r.player_uuid, latest.player_name, r.score, r.finished_at
            from ranked r
            cross join lateral (
                select n.player_name
                from jumprun_run n
                where n.player_uuid = r.player_uuid
                order by n.finished_at desc, n.id desc
                limit 1
            ) latest
            where r.place <= 3
            order by r.mode, r.place
            """;

    /** One row of the bests query. */
    private record Best(Mode mode, Integer score) {
    }

    // A Provider, resolved on first use: the column's module is built before persistence's, which
    // needs the column's PersistenceUnit first, so the SessionFactory is not there at wiring time.
    private final Provider<SessionFactory> sessions;

    HibernateRunStore(@External Provider<SessionFactory> sessions) {
        this.sessions = sessions;
    }

    @Override
    public Map<Mode, Integer> bestsOf(UUID player) {
        List<Best> rows = this.sessions.get().fromSession(session -> {
            session.setDefaultReadOnly(true);
            return session.createSelectionQuery("select r.mode, max(r.score) from JumprunRunEntity r where r.playerUuid = :player group by r.mode", Best.class).setParameter("player", player).getResultList();
        });
        Map<Mode, Integer> bests = new EnumMap<>(Mode.class);
        rows.forEach(row -> bests.put(row.mode(), row.score()));
        return bests;
    }

    @Override
    public Map<Mode, TopThree> topThreeOfEveryMode() {
        List<Tuple> rows = this.sessions.get().fromSession(session -> {
            session.setDefaultReadOnly(true);
            return session.createNativeQuery(TOP_THREE_OF_EVERY_MODE, Tuple.class).getResultList();
        });
        Map<Mode, List<TopEntry>> byMode = new EnumMap<>(Mode.class);
        for (Tuple row : rows) {
            Mode mode = Mode.valueOf(row.get("mode", String.class));
            byMode.computeIfAbsent(mode, _ -> new ArrayList<>()).add(new TopEntry(row.get("player_uuid", UUID.class), row.get("player_name", String.class), row.get("score", Integer.class), row.get("finished_at", Instant.class)));
        }
        Map<Mode, TopThree> top = new EnumMap<>(Mode.class);
        byMode.forEach((mode, entries) -> top.put(mode, new TopThree(entries)));
        return top;
    }

    @Override
    public void append(FinishedRun run) {
        this.sessions.get().inTransaction(session -> session.persist(new JumprunRunEntity(run)));
    }
}

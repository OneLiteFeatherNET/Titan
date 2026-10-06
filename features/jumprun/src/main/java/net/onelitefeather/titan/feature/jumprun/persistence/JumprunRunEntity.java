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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import org.hibernate.annotations.Immutable;

/** One row of {@code jumprun_run}; written once by {@link HibernateRunStore}, never updated. */
@Entity
@Immutable
@Table(name = "jumprun_run")
class JumprunRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "player_uuid", nullable = false)
    UUID playerUuid;

    @Column(name = "player_name", nullable = false, length = 32)
    String playerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    Mode mode;

    @Column(nullable = false)
    int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", nullable = false, length = 16)
    EndReason endReason;

    @Column(name = "finished_at", nullable = false)
    Instant finishedAt;

    /** For Hibernate. */
    protected JumprunRunEntity() {
    }

    JumprunRunEntity(FinishedRun run) {
        this.playerUuid = run.player();
        this.playerName = run.name();
        this.mode = run.mode();
        this.score = run.score();
        this.endReason = run.endReason();
        this.finishedAt = run.finishedAt();
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.persistence.FakeRunStore;
import net.onelitefeather.titan.feature.jumprun.persistence.StoredRunRecords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The records of a player come from the store while the player joins, and leave with them. */
@ExtendWith(MicrotusExtension.class)
class JumprunStoredRecordsTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1ec");

    private final FakeRunStore store = new FakeRunStore();

    private static Player join(Env env) {
        return env.createConnection(new GameProfile(ALEX, "Alex")).connect(JumprunFixture.loadedInstance(env), new Pos(0.5, JumprunFixture.GROUND_Y, 0.5));
    }

    @Test
    void theStoredBestIsLoadedByTheTimeThePlayerHasJoined(Env env) {
        store.append(JumprunFixture.finished(ALEX, Mode.HARD, 15));
        StoredRunRecords records = new StoredRunRecords(store, Runnable::run);
        try (JumprunFixture fixture = JumprunFixture.start(env, records)) {
            join(env);

            assertEquals(OptionalInt.of(15), records.best(ALEX, Mode.HARD), "the record is there before the first run could start");
        }
    }

    @Test
    void aRunEqualToTheStoredBestIsNoNewRecord(Env env) {
        store.append(JumprunFixture.finished(ALEX, Mode.HARD, 15));
        StoredRunRecords records = new StoredRunRecords(store, Runnable::run);
        try (JumprunFixture fixture = JumprunFixture.start(env, records)) {
            join(env);

            assertFalse(records.submit(JumprunFixture.finished(ALEX, Mode.HARD, 15)), "15 ties the stored best");
            assertTrue(records.submit(JumprunFixture.finished(ALEX, Mode.HARD, 16)), "16 beats it");
        }
    }

    @Test
    void afterTheDisconnectTheCacheIsEmptyAndTheStoreKeepsTheRun(Env env) {
        StoredRunRecords records = new StoredRunRecords(store, Runnable::run);
        try (JumprunFixture fixture = JumprunFixture.start(env, records)) {
            Player player = join(env);
            records.submit(JumprunFixture.finished(ALEX, Mode.HARD, 20));

            env.process().eventHandler().call(new PlayerDisconnectEvent(player));

            assertEquals(OptionalInt.empty(), records.best(ALEX, Mode.HARD), "nothing stays in memory for a player who left");
            assertEquals(20, store.bestsOf(ALEX).get(Mode.HARD), "the run is in the store");
        }
    }

    @Test
    void aRunThatFailsToStoreStillEndsWithItsMessageAndStaysTheRecordInThisLobby(Env env) {
        StoredRunRecords records = new StoredRunRecords(store, Runnable::run);
        try (JumprunFixture fixture = JumprunFixture.start(env, records); CapturedLog log = new CapturedLog(StoredRunRecords.class)) {
            StartedRun run = StartedRun.start(env, fixture);
            store.failWith(new IllegalStateException("database is gone"));
            run.landOnNext(JumprunFixture.ASCENT_JUMPS + 1);
            Collector<SystemChatPacket> chat = run.connection().trackIncoming(SystemChatPacket.class);

            fixture.useItem(run.player());

            Component expected = fixture.messages().endRecord(run.player().getLocale(), Mode.MEDIUM, 1);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
            assertFalse(fixture.module().isRunning(run.player()), "the run is over, the failure did not hold it");
            assertEquals(OptionalInt.of(1), records.best(run.player().getUuid(), Mode.MEDIUM), "the record holds in this lobby");
            List<ILoggingEvent> warnings = log.warnings();
            assertEquals(1, warnings.size(), "the operator sees the failed write once");
            assertEquals("Could not store jump and run result", warnings.getFirst().getMessage());
        }
    }
}

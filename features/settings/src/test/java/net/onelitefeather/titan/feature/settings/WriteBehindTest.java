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
package net.onelitefeather.titan.feature.settings;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import net.onelitefeather.titan.core.settings.SettingKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WriteBehindTest {

    private static final SettingKey KEY = SettingKey.parse("titan:tag.enabled");
    private static final Duration DEBOUNCE = Duration.ofSeconds(2);

    private final AdjustableClock clock = new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC);
    private final WriteBehind writes = new WriteBehind(this.clock, DEBOUNCE, 3);

    @DisplayName("Several switches of one setting within the debounce cost one write with the last value")
    @Test
    void rapidSwitchesBecomeOneWrite() {
        UUID player = UUID.randomUUID();
        this.writes.changed(player, KEY, true);
        this.clock.advance(Duration.ofSeconds(1));
        this.writes.changed(player, KEY, false);
        this.clock.advance(Duration.ofSeconds(1));
        this.writes.changed(player, KEY, true);
        this.clock.advance(Duration.ofSeconds(2));
        List<WriteBehind.Write> due = this.writes.takeDue();
        Assertions.assertEquals(1, due.size(), "three switches of one setting must produce one write");
        Assertions.assertTrue(due.getFirst().value(), "the write must carry the last value switched to");
    }

    @DisplayName("A write is not due before the debounce has run out")
    @Test
    void writeWaitsForTheDebounce() {
        this.writes.changed(UUID.randomUUID(), KEY, true);
        this.clock.advance(Duration.ofMillis(1999));
        Assertions.assertTrue(this.writes.takeDue().isEmpty(), "the write must wait until the debounce has passed");
    }

    @DisplayName("Leaving takes the queued writes of that player at once, without waiting")
    @Test
    void leavingTakesThePlayersWritesAtOnce() {
        UUID leaving = UUID.randomUUID();
        UUID staying = UUID.randomUUID();
        this.writes.changed(leaving, KEY, true);
        this.writes.changed(staying, KEY, true);
        List<WriteBehind.Write> taken = this.writes.takeAll(leaving);
        Assertions.assertEquals(1, taken.size(), "only the leaving player's write must be taken");
        Assertions.assertEquals(leaving, taken.getFirst().player(), "the taken write must belong to the leaving player");
    }

    @DisplayName("A retryable failure is queued again after the retry gap")
    @Test
    void retryableFailureIsRetriedAfterTheGap() {
        UUID player = UUID.randomUUID();
        this.writes.changed(player, KEY, true);
        this.clock.advance(DEBOUNCE);
        WriteBehind.Write first = this.writes.takeDue().getFirst();
        Assertions.assertTrue(this.writes.failed(first, true), "a retryable failure with retries left must be queued again");
        this.clock.advance(Duration.ofMillis(499));
        Assertions.assertTrue(this.writes.takeDue().isEmpty(), "the retry must wait for the gap");
        this.clock.advance(Duration.ofMillis(1));
        List<WriteBehind.Write> retry = this.writes.takeDue();
        Assertions.assertEquals(1, retry.size(), "the retry must be due after the gap");
        Assertions.assertEquals(1, retry.getFirst().failures(), "the retry must count the failure it follows");
    }

    @DisplayName("After the retry limit a failing write is parked until the player leaves")
    @Test
    void exhaustedRetriesPark() {
        UUID player = UUID.randomUUID();
        this.writes.changed(player, KEY, true);
        this.clock.advance(DEBOUNCE);
        WriteBehind.Write write = this.writes.takeDue().getFirst();
        for (int attempt = 0; attempt < 3; attempt++) {
            Assertions.assertTrue(this.writes.failed(write, true), "attempt " + attempt + " must be queued again");
            this.clock.advance(WriteBehind.RETRY_GAP);
            write = this.writes.takeDue().getFirst();
        }
        Assertions.assertFalse(this.writes.failed(write, true), "the fourth failure must exhaust the retries");
        this.clock.advance(Duration.ofHours(1));
        Assertions.assertTrue(this.writes.takeDue().isEmpty(), "a parked write must not be retried on a timer");
        Assertions.assertEquals(1, this.writes.takeAll(player).size(), "a parked write must still be taken when the player leaves");
    }

    @DisplayName("A rejected write is not retried")
    @Test
    void rejectedWriteIsNotRetried() {
        UUID player = UUID.randomUUID();
        this.writes.changed(player, KEY, true);
        this.clock.advance(DEBOUNCE);
        WriteBehind.Write write = this.writes.takeDue().getFirst();
        Assertions.assertFalse(this.writes.failed(write, false), "a rejected write must not be queued again");
        this.clock.advance(Duration.ofHours(1));
        Assertions.assertTrue(this.writes.takeAll(player).isEmpty(), "a rejected write must not wait in the queue");
    }

    @DisplayName("A newer switch replaces a failed write, which is then not retried")
    @Test
    void newerSwitchSupersedesAFailedWrite() {
        UUID player = UUID.randomUUID();
        this.writes.changed(player, KEY, true);
        this.clock.advance(DEBOUNCE);
        WriteBehind.Write failed = this.writes.takeDue().getFirst();
        this.writes.changed(player, KEY, false);
        Assertions.assertFalse(this.writes.failed(failed, true), "the failed value is superseded and must not be queued again");
        List<WriteBehind.Write> queued = this.writes.takeAll(player);
        Assertions.assertEquals(1, queued.size(), "only the newer switch must remain queued");
        Assertions.assertFalse(queued.getFirst().value(), "the newer value must be the one written");
    }
}

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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.onelitefeather.titan.feature.jumprun.course.Mode;
import org.junit.jupiter.api.Test;

/** The fake obeys the same contract as the Postgres store, plus failing on demand. */
class FakeRunStoreTest implements RunStoreContract {

    private final FakeRunStore store = new FakeRunStore();

    @Override
    public RunStore store() {
        return this.store;
    }

    @Test
    void failing_throwsOnEveryAccess() {
        IllegalStateException failure = new IllegalStateException("database is gone");
        this.store.failWith(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> this.store.append(RunStoreContract.run(ALEX, "Alex", Mode.HARD, 1, 0)), "append"));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> this.store.bestsOf(ALEX), "bestsOf"));
        assertSame(failure, assertThrows(IllegalStateException.class, this.store::topThreeOfEveryMode, "topThreeOfEveryMode"));
    }

    @Test
    void failingStore_keepsNoRunThatFailedToAppend() {
        this.store.failWith(new IllegalStateException("database is gone"));
        assertThrows(IllegalStateException.class, () -> this.store.append(RunStoreContract.run(ALEX, "Alex", Mode.HARD, 1, 0)));

        this.store.recover();

        assertTrue(this.store.bestsOf(ALEX).isEmpty(), "the failed append left nothing behind");
    }
}

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
package net.onelitefeather.titan.runtime.lifecycle;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScopeGuardTest {

    @DisplayName("A failing body closes the scope, so its pre-destroy callbacks run")
    @Test
    void failingBodyClosesTheScope() {
        AtomicInteger closes = new AtomicInteger();

        Assertions.assertThrows(IllegalStateException.class, () -> ScopeGuard.closeOnFailure(closes::incrementAndGet, () -> {
            throw new IllegalStateException("column missing");
        }));

        Assertions.assertEquals(1, closes.get(), "the scope must be closed exactly once after a failed body");
    }

    @DisplayName("A failing body rethrows the very same exception instance")
    @Test
    void failingBodyRethrowsTheOriginalException() {
        AtomicInteger closes = new AtomicInteger();
        IllegalStateException original = new IllegalStateException("column missing");

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> ScopeGuard.closeOnFailure(closes::incrementAndGet, () -> {
            throw original;
        }));

        Assertions.assertSame(original, thrown, "the caller must see the original failure, not a wrapper");
    }

    @DisplayName("An exception thrown by close is attached as suppressed to the original failure")
    @Test
    void closeFailureIsSuppressedOnTheOriginalFailure() {
        IllegalStateException original = new IllegalStateException("column missing");
        IllegalArgumentException closeFailure = new IllegalArgumentException("pool already gone");

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> ScopeGuard.closeOnFailure(() -> {
            throw closeFailure;
        }, () -> {
            throw original;
        }));

        Assertions.assertSame(original, thrown);
        Assertions.assertArrayEquals(new Throwable[]{closeFailure}, thrown.getSuppressed());
    }

    @DisplayName("A successful body returns its result and leaves the scope open")
    @Test
    void successfulBodyReturnsResultWithoutClosing() {
        AtomicInteger closes = new AtomicInteger();

        String result = ScopeGuard.closeOnFailure(closes::incrementAndGet, () -> "started");

        Assertions.assertEquals("started", result);
        Assertions.assertEquals(0, closes.get(), "a successful start must keep the scope open");
    }
}

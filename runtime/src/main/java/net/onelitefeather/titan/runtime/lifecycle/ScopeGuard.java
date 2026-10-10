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

import java.util.function.Supplier;

/**
 * Closes a freshly built scope when the code that follows its construction throws, so the
 * {@code @PreDestroy} callbacks of already started beans still run and no non-daemon thread keeps
 * the JVM alive after a failed start.
 */
public final class ScopeGuard {

    private ScopeGuard() {
    }

    /**
     * Runs {@code body}; if it throws, closes {@code scope} and rethrows the original failure. A
     * failure of {@code close} is attached to it as suppressed, never the other way round.
     */
    public static <T> T closeOnFailure(AutoCloseable scope, Supplier<T> body) {
        try {
            return body.get();
        } catch (RuntimeException | Error failure) {
            closeSuppressed(scope, failure);
            throw failure;
        }
    }

    /** {@link #closeOnFailure(AutoCloseable, Supplier)} for a body without a result. */
    public static void closeOnFailure(AutoCloseable scope, Runnable body) {
        closeOnFailure(scope, () -> {
            body.run();
            return null;
        });
    }

    private static void closeSuppressed(AutoCloseable scope, Throwable failure) {
        try {
            scope.close();
        } catch (Throwable closeFailure) {
            // Even an Error from close must not replace the startup failure that caused it.
            failure.addSuppressed(closeFailure);
        }
    }
}

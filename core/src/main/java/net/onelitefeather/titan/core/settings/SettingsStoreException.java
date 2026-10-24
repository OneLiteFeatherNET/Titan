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
package net.onelitefeather.titan.core.settings;

import java.util.Objects;

/**
 * A failed store call. The message names the failure and the key at most, never a value or a
 * credential.
 */
public final class SettingsStoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Why a store call failed, and whether trying again can help. */
    public enum Failure {
        TIMEOUT(true, "timeout"), UNAVAILABLE(true, "error"), CIRCUIT_OPEN(true, "circuit_open"), NOT_FOUND(true, "error"), REJECTED(false, "error");

        private final boolean retryable;
        private final String outcome;

        Failure(boolean retryable, String outcome) {
            this.retryable = retryable;
            this.outcome = outcome;
        }

        public boolean retryable() {
            return this.retryable;
        }

        /** The telemetry outcome this failure is counted under. */
        public String outcome() {
            return this.outcome;
        }
    }

    private final Failure failure;

    public SettingsStoreException(Failure failure, String message) {
        this(failure, message, null);
    }

    public SettingsStoreException(Failure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public Failure failure() {
        return this.failure;
    }
}

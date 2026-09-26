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
package net.onelitefeather.titan.app.bootstrap.reload;

/**
 * Walks a {@link Throwable}'s cause chain down to its innermost cause and returns that cause's own
 * message - the piece {@link ConfigChangeHandler} needs for its WARN line, since a wrapping
 * exception's own message often names nothing but the fact that something was wrapped.
 *
 * <p>avaje-config 5.2's {@code Config.getAs(key, fn)} wraps any exception a module's conversion
 * function throws as {@code new IllegalStateException("Failed to convert key: " + key + " with
 * the provided function", e)} - so {@link ConfigChangeHandler} reusing this same walk is what
 * turns that wrapper back into the module's own rejection reason (e.g. "must not be negative, was
 * -5") instead of "Failed to convert key: ...". See
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1 (DRY).
 */
final class Causes {

    private Causes() {
    }

    /**
     * @param throwable the exception to walk
     * @return the innermost cause's message, or its {@link Throwable#toString()} if that cause has
     *         no message
     */
    static String rootMessage(Throwable throwable) {
        Throwable innermost = throwable;
        while (innermost.getCause() != null) {
            innermost = innermost.getCause();
        }
        String message = innermost.getMessage();
        return message != null ? message : innermost.toString();
    }
}

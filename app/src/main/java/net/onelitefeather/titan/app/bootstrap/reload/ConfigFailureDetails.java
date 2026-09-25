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
 * Extracts the broken file and the human-readable error position from an
 * {@code io.avaje.config.Configuration.Builder#includeResourceLoading().build()} failure - the
 * single place in production code that does this (design.md, decision 1: DRY).
 *
 * <p>avaje-config's own {@code InitialLoader} wraps every parser failure as
 * {@code new IllegalStateException("Error loading properties - " + resourcePath, cause)}, where
 * {@code cause} is the parser's own exception - for a syntactically broken YAML file, SnakeYAML's,
 * whose message already names the line and column (e.g. {@code "... in 'reader', line 3, column
 * 4: ..."}). This walks that cause chain: the first message starting with the loader's own prefix
 * names the file, and the innermost cause's message is the human-readable position - the same
 * pieces of information {@code app/src/test/.../bootstrap/ConfigurationPrintMain#printCauseChain}
 * prints for a broken startup load, extracted here as two separate values instead of a printed
 * chain, since {@link ConfigSnapshotSource}/{@link ReloadResult.Failed} need {@code file} and
 * {@code detail} as two fields, not a log line.
 */
final class ConfigFailureDetails {

    private static final String LOADING_PREFIX = "Error loading properties - ";
    private static final String FALLBACK_FILE = "application.yaml";

    private ConfigFailureDetails() {
    }

    /**
     * @param failure the exception {@code Configuration.builder().includeResourceLoading().build()}
     *                threw
     * @return a {@link ConfigSnapshotException} naming the broken file and the parser's error
     *         position, with {@code failure} as its cause
     */
    static ConfigSnapshotException toSnapshotException(RuntimeException failure) {
        return new ConfigSnapshotException(fileOf(failure), detailOf(failure), failure);
    }

    private static String fileOf(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.startsWith(LOADING_PREFIX)) {
                return message.substring(LOADING_PREFIX.length());
            }
        }
        // Should not normally happen - every load failure avaje-config throws carries this
        // message somewhere in its cause chain - but naming the shipped default file beats
        // throwing a second, unrelated exception while already handling one.
        return FALLBACK_FILE;
    }

    private static String detailOf(Throwable failure) {
        Throwable innermost = failure;
        while (innermost.getCause() != null) {
            innermost = innermost.getCause();
        }
        String message = innermost.getMessage();
        return message != null ? message : innermost.toString();
    }
}

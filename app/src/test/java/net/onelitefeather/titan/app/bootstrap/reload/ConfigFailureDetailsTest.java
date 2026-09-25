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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ConfigFailureDetails}, against the exact exception shape
 * avaje-config's {@code InitialLoader} throws for a broken load: an outer
 * {@link IllegalStateException} whose message is {@code "Error loading properties - <file>"},
 * caused by the parser's own exception (for YAML, SnakeYAML's, whose message names the line and
 * column) - see {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1.
 */
class ConfigFailureDetailsTest {

    @DisplayName("Names the file from the loader's own wrapping message and the position from the innermost cause")
    @Test
    void namesTheFileAndTheInnermostCausesMessage() {
        RuntimeException parserFailure = new RuntimeException("while parsing a block mapping\n in 'reader', line 3, column 4:\n     maxHeight: 310\n     ^");
        IllegalStateException failure = new IllegalStateException("Error loading properties - application.yaml", parserFailure);

        ConfigSnapshotException exception = ConfigFailureDetails.toSnapshotException(failure);

        Assertions.assertEquals("application.yaml", exception.file());
        Assertions.assertTrue(exception.detail().contains("line 3, column 4"), "must name the error position, was: " + exception.detail());
        Assertions.assertSame(failure, exception.getCause(), "must keep the original failure as its cause");
    }

    @DisplayName("Walks a longer cause chain (profile file, external file) to find the loader's message")
    @Test
    void walksALongerCauseChainToFindTheLoaderMessage() {
        RuntimeException parserFailure = new RuntimeException("line 7, column 1: mapping values are not allowed here");
        IllegalStateException loaderFailure = new IllegalStateException("Error loading properties - application-dev.yaml", parserFailure);
        RuntimeException wrapper = new RuntimeException("wrapped", loaderFailure);

        ConfigSnapshotException exception = ConfigFailureDetails.toSnapshotException(wrapper);

        Assertions.assertEquals("application-dev.yaml", exception.file());
        Assertions.assertEquals("line 7, column 1: mapping values are not allowed here", exception.detail());
    }

    @DisplayName("Falls back to the shipped default file name when no cause names the loading message")
    @Test
    void fallsBackToTheDefaultFileWhenNoCauseNamesTheLoadingMessage() {
        RuntimeException unrelatedFailure = new RuntimeException("boom");

        ConfigSnapshotException exception = ConfigFailureDetails.toSnapshotException(unrelatedFailure);

        Assertions.assertEquals("application.yaml", exception.file());
        Assertions.assertEquals("boom", exception.detail());
    }
}

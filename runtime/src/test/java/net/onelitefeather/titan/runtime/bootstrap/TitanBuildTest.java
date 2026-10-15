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
package net.onelitefeather.titan.runtime.bootstrap;

import java.util.jar.Attributes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit coverage for {@link TitanBuild}: the version and commit that the startup line names. */
class TitanBuildTest {

    @DisplayName("Falls back to dev and unknown when the version and commit are missing")
    @Test
    void missingVersionAndCommitFallBack() {
        TitanBuild build = new TitanBuild(null, null);

        Assertions.assertEquals("dev", build.version(), "a build without Implementation-Version must report dev");
        Assertions.assertEquals("unknown", build.commit(), "a build without Git-Commit must report unknown");
    }

    @DisplayName("Treats a blank version and commit like a missing one")
    @Test
    void blankVersionAndCommitFallBack() {
        TitanBuild build = new TitanBuild("  ", "");

        Assertions.assertEquals("dev", build.version(), "a blank version must fall back to dev");
        Assertions.assertEquals("unknown", build.commit(), "a blank commit must fall back to unknown");
    }

    @DisplayName("Keeps the version and commit it was given")
    @Test
    void keepsGivenVersionAndCommit() {
        TitanBuild build = new TitanBuild("3.2.1", "99a7f62");

        Assertions.assertEquals("3.2.1", build.version(), "the given version must be kept as is");
        Assertions.assertEquals("99a7f62", build.commit(), "the given commit must be kept as is");
    }

    @DisplayName("Reads the version and commit from the jar manifest attributes")
    @Test
    void readsVersionAndCommitFromManifest() {
        Attributes attributes = new Attributes();
        attributes.putValue("Implementation-Version", "3.2.1");
        attributes.putValue("Git-Commit", "99a7f62");

        TitanBuild build = TitanBuild.fromManifest(attributes);

        Assertions.assertEquals(new TitanBuild("3.2.1", "99a7f62"), build, "the version and commit must come from the manifest attributes");
    }

    @DisplayName("Falls back to dev and unknown when the manifest has neither attribute")
    @Test
    void manifestWithoutAttributesFallsBack() {
        TitanBuild build = TitanBuild.fromManifest(new Attributes());

        Assertions.assertEquals(new TitanBuild("dev", "unknown"), build, "a manifest without the two attributes must fall back");
    }
}

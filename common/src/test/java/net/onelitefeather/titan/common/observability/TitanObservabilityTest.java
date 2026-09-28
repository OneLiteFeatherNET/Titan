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
package net.onelitefeather.titan.common.observability;

import io.sentry.Sentry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers only what stayed in {@code common}: Sentry bootstrap. The listener guard/exception
 * attribution moved to {@code core}'s {@code ListenerGuardTest}.
 */
class TitanObservabilityTest {

    @DisplayName("Without a DSN Sentry is never initialised")
    @Test
    void bootstrapLeavesSentryDisabledWithoutADsn() {
        // The environment variable is not set for this build, which is the operator-without-Sentry
        // case: bootstrap must be a no-op rather than a failure.
        Assertions.assertNull(System.getenv(TitanObservability.DSN_ENVIRONMENT_VARIABLE), "this test asserts the disabled path - unset " + TitanObservability.DSN_ENVIRONMENT_VARIABLE);

        TitanObservability.bootstrap("test-release");

        Assertions.assertFalse(Sentry.isEnabled(), "no DSN must leave the SDK untouched");
    }

    @DisplayName("Outside a jar the release falls back to dev rather than null")
    @Test
    void releaseFallsBackToDevWhenNoManifestIsPresent() {
        // Tests run from a class directory, so the Implementation-Version attribute the shaded jar
        // carries is absent here.
        Assertions.assertEquals("dev", TitanObservability.release());
    }
}

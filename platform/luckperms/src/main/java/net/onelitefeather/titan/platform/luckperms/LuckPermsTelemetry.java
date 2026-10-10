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
package net.onelitefeather.titan.platform.luckperms;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import java.util.Collection;
import java.util.Locale;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Telemetry of the LuckPerms platform. A permission check runs on portal moves and command
 * conditions, so it is counted and, inside a running span, added to that span as an event; it
 * never opens a span of its own. The player is not recorded at all, to keep the counter's
 * attributes to the two outcomes.
 */
final class LuckPermsTelemetry {

    static final String CHECK_EVENT = "permission.check";
    static final String START_SPAN = "permission.platform.start";

    private static final String PLATFORM = "luckperms";
    private static final AttributeKey<String> PLATFORM_KEY = AttributeKey.stringKey("permission.platform");
    private static final AttributeKey<Boolean> EXTENSION_LOADED = AttributeKey.booleanKey("luckperms.extension.loaded");
    private static final AttributeKey<String> PERMISSION = AttributeKey.stringKey("permission");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");

    private final Telemetry telemetry;
    private final LongCounter checks;

    LuckPermsTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.checks = telemetry.meter().counterBuilder("permission.checks").setUnit("{check}").build();
    }

    /** @return {@code result}, so the caller can return the check's outcome unchanged */
    PermissionResult checked(String permission, PermissionResult result) {
        String outcome = result.name().toLowerCase(Locale.ROOT);
        this.checks.add(1, Attributes.of(RESULT, outcome));
        Span.current().addEvent(CHECK_EVENT, Attributes.of(PERMISSION, permission, RESULT, outcome));
        return result;
    }

    /**
     * Runs the platform start inside its span. A LuckPerms extension already loaded fails the
     * span and the start, and the exception reaches the caller.
     */
    void started(Collection<String> extensionNames, Runnable startPlatform) {
        Attributes attributes = Attributes.builder().put(PLATFORM_KEY, PLATFORM).put(EXTENSION_LOADED, LuckPermsExtensionCheck.isLoadedAsExtension(extensionNames)).build();
        this.telemetry.inSpan(START_SPAN, attributes, () -> {
            LuckPermsExtensionCheck.ensureNotLoadedTwice(extensionNames);
            startPlatform.run();
        });
    }
}

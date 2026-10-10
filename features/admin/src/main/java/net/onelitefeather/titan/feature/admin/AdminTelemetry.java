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
package net.onelitefeather.titan.feature.admin;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * One span and one counter per admin command run, a denial included. Admin commands are rare, so
 * each run gets a span; the player is a span attribute as a UUID, never a metric attribute. The
 * span is recorded as a completed marker before the shutdown is started, so it never encloses it.
 */
final class AdminTelemetry {

    static final String SPAN = "admin.command";
    static final String EXECUTED = "executed";
    static final String DENIED = "denied";

    private static final AttributeKey<String> COMMAND = AttributeKey.stringKey("admin.command");
    private static final AttributeKey<String> SENDER = AttributeKey.stringKey("admin.sender");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("admin.result");
    private static final AttributeKey<String> METRIC_COMMAND = AttributeKey.stringKey("command");
    private static final AttributeKey<String> METRIC_RESULT = AttributeKey.stringKey("result");

    private final Telemetry telemetry;
    private final LongCounter commands;

    AdminTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.commands = telemetry.meter().counterBuilder("admin.commands").setUnit("{command}").build();
    }

    /** Records a run that passed its condition; call before starting the shutdown. */
    void executed(String command, CommandSender sender) {
        observe(command, sender, EXECUTED);
    }

    /**
     * Wraps {@code condition} so that a refusal of a real command attempt is recorded as a denied
     * run. Minestom calls the condition with a null command string when it builds the command tree
     * for a client; that is no attempt and is not recorded.
     */
    CommandCondition guard(String command, CommandCondition condition) {
        return (sender, commandString) -> {
            boolean allowed = condition.canUse(sender, commandString);
            if (!allowed && commandString != null) {
                observe(command, sender, DENIED);
            }
            return allowed;
        };
    }

    private void observe(String command, CommandSender sender, String result) {
        this.commands.add(1, Attributes.of(METRIC_COMMAND, command, METRIC_RESULT, result));
        AttributesBuilder attributes = Attributes.builder().put(COMMAND, command).put(SENDER, senderKind(sender)).put(RESULT, result);
        if (sender instanceof Player player) {
            attributes.put(Telemetry.USER_ID, player.getUuid().toString());
        }
        this.telemetry.inSpan(SPAN, attributes.build(), () -> {
        });
    }

    private static String senderKind(CommandSender sender) {
        return sender instanceof Player ? "player" : "console";
    }
}

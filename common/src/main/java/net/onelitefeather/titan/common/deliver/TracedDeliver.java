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
package net.onelitefeather.titan.common.deliver;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import java.util.Objects;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Wraps the platform's {@link Deliver} so every transfer to another server or task is a span.
 * Portals and the navigator both deliver through it, so one decorator covers both; the CloudNet
 * bridge itself stays untouched.
 *
 * <p>The component must not be {@code null}: the callers always build one.
 */
public final class TracedDeliver implements Deliver {

    static final String SPAN = "deliver.send_player";

    private static final AttributeKey<String> TARGET_TYPE = AttributeKey.stringKey("titan.deliver.target_type");
    private static final AttributeKey<String> TARGET = AttributeKey.stringKey("titan.deliver.target");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("titan.deliver.result");

    private final Deliver delegate;
    private final Telemetry telemetry;

    public TracedDeliver(Deliver delegate, Telemetry telemetry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        this.telemetry.inSpan(SPAN, attributes(component), () -> {
            try {
                this.delegate.sendPlayer(player, component);
                Span.current().setAttribute(RESULT, "ok");
            } catch (RuntimeException e) {
                Span.current().setAttribute(RESULT, "error");
                throw e;
            }
        });
    }

    private static Attributes attributes(DeliverComponent component) {
        AttributesBuilder builder = Attributes.builder().put(Telemetry.USER_ID, component.playerId().toString());
        return switch (component) {
            case DeliverComponent.TaskComponent task ->
                builder.put(TARGET_TYPE, "task").put(TARGET, task.taskName()).build();
            case DeliverComponent.ServerDeliverComponent server ->
                builder.put(TARGET_TYPE, "server").put(TARGET, server.gameServer()).build();
        };
    }
}

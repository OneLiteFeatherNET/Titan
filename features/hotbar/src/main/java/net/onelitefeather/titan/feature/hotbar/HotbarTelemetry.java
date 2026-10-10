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
package net.onelitefeather.titan.feature.hotbar;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.UUID;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Names and attributes of the hotbar's spans, event and counter. Equipping is rare enough for a
 * span; a use is a click, so it gets a span too, while a click without a lobby item gets nothing.
 * The counter carries the item key only, a fixed small set.
 */
final class HotbarTelemetry {

    static final String EQUIP_SPAN = "hotbar.equip";
    static final String USE_SPAN = "hotbar.item.use";
    static final String CONFLICT_EVENT = "hotbar.item_conflict";

    static final AttributeKey<String> ITEM = AttributeKey.stringKey("hotbar.item");
    static final AttributeKey<String> CONFLICT_KIND = AttributeKey.stringKey("hotbar.conflict");

    private static final AttributeKey<Long> ITEMS = AttributeKey.longKey("hotbar.items");
    private static final AttributeKey<String> METRIC_ITEM = AttributeKey.stringKey("item");

    private final Telemetry telemetry;
    private final LongCounter uses;

    HotbarTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.uses = telemetry.meter().counterBuilder("titan.hotbar.item.uses").setUnit("{use}").build();
    }

    /** Runs {@code equip} inside the equip span, with the number of items it places. */
    void equip(UUID player, int items, Runnable equip) {
        Attributes attributes = Attributes.builder().put(Telemetry.USER_ID, player.toString()).put(ITEMS, (long) items).build();
        this.telemetry.inSpan(EQUIP_SPAN, attributes, equip);
    }

    /**
     * Counts the use and runs {@code handler} inside the use span, so a failure marks that span.
     */
    void use(String featureId, String key, UUID player, Runnable handler) {
        this.uses.add(1, Attributes.of(METRIC_ITEM, key));
        Attributes attributes = Attributes.builder().put(Telemetry.FEATURE, featureId).put(ITEM, key).put(Telemetry.USER_ID, player.toString()).build();
        this.telemetry.inSpan(USE_SPAN, attributes, handler);
    }
}

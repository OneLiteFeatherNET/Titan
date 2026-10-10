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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A conflicting item list adds {@code hotbar.item_conflict} to the span that is current. */
class ItemConflictTelemetryTest {

    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void close() {
        testTelemetry.close();
    }

    private static LobbyItem item(String key, Material material, ItemSlot placement) {
        return new LobbyItem("test-feature", Key.key(key), ItemStack.of(material), placement, (player, event) -> {
        });
    }

    @DisplayName("A slot conflict adds a hotbar.item_conflict event to the current span")
    @Test
    void aSlotConflictAddsAnEventToTheCurrentSpan() {
        List<LobbyItem> items = List.of(item("titan:navigator", Material.FEATHER, ItemSlot.hotbar(4)), item("titan:friends", Material.COMPASS, ItemSlot.hotbar(4)));

        testTelemetry.telemetry().inSpan("titan.startup", Attributes.empty(), () -> {
            Assertions.assertThrows(IllegalStateException.class, () -> ItemConflicts.check(items));
        });

        List<EventData> conflicts = conflictEvents(testTelemetry.span("titan.startup"));
        Assertions.assertEquals(1, conflicts.size(), "exactly one conflict event on the startup span");
        Assertions.assertEquals("slot", conflicts.getFirst().getAttributes().get(HotbarTelemetry.CONFLICT_KIND), "a shared placement is a slot conflict");
        Assertions.assertEquals("titan:friends", conflicts.getFirst().getAttributes().get(HotbarTelemetry.ITEM), "the event names the item that clashed");
    }

    @DisplayName("A key conflict is recorded with the kind key")
    @Test
    void aKeyConflictIsRecordedAsAKeyConflict() {
        List<LobbyItem> items = List.of(item("titan:navigator", Material.FEATHER, ItemSlot.hotbar(4)), item("titan:navigator", Material.COMPASS, ItemSlot.hotbar(5)));

        testTelemetry.telemetry().inSpan("titan.startup", Attributes.empty(), () -> {
            Assertions.assertThrows(IllegalStateException.class, () -> ItemConflicts.check(items));
        });

        Assertions.assertEquals("key", conflictEvents(testTelemetry.span("titan.startup")).getFirst().getAttributes().get(HotbarTelemetry.CONFLICT_KIND), "a shared key is a key conflict");
    }

    @DisplayName("Without a running span a conflict still aborts with its message")
    @Test
    void aConflictWithoutASpanStillAborts() {
        List<LobbyItem> items = List.of(item("titan:navigator", Material.FEATHER, ItemSlot.hotbar(4)), item("titan:friends", Material.COMPASS, ItemSlot.hotbar(4)));

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> ItemConflicts.check(items));

        Assertions.assertTrue(thrown.getMessage().contains("hotbar slot 4"), "the message still names the slot: " + thrown.getMessage());
    }

    private static List<EventData> conflictEvents(SpanData span) {
        return span.getEvents().stream().filter(event -> event.getName().equals(HotbarTelemetry.CONFLICT_EVENT)).toList();
    }
}

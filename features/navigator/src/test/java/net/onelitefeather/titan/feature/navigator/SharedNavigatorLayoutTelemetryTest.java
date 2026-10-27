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
package net.onelitefeather.titan.feature.navigator;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The layout span of {@link SharedNavigator}: a layout is spanned only when it actually changes.
 */
class SharedNavigatorLayoutTelemetryTest {

    private static final String LAYOUT_SPAN = "navigator.layout.apply";

    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void closeTelemetry() {
        testTelemetry.close();
    }

    private static InventoryLayout emptyLayout(Map<Destination, Integer> slots) {
        return InventoryLayout.fromType(NavigatorLayout.TYPE);
    }

    private long layoutSpans() {
        return testTelemetry.spans().stream().map(SpanData::getName).filter(LAYOUT_SPAN::equals).count();
    }

    @DisplayName("The layout of a menu is applied inside a navigator.layout.apply span")
    @Test
    void layoutIsSpanned() {
        SharedNavigator navigator = new SharedNavigator(Component.empty(), NavigatorLayout.TYPE, NavigatorLayout.mainSlots(Destination.visible(Set.of())), testTelemetry.telemetry());

        navigator.applyLayout(SharedNavigatorLayoutTelemetryTest::emptyLayout);

        Assertions.assertEquals(1, layoutSpans(), "applying the layout must produce one layout span");
    }

    @DisplayName("A menu lays out the slots it was built with, Slender included")
    @Test
    void layoutFollowsTheVisibleDestinations() {
        Map<Destination, Integer> slots = NavigatorLayout.mainSlots(Destination.visible(Set.of(Destination.SLENDER)));
        SharedNavigator navigator = new SharedNavigator(Component.empty(), NavigatorLayout.TYPE, slots, testTelemetry.telemetry());
        List<Map<Destination, Integer>> laidOut = new ArrayList<>();

        navigator.applyLayout(laid -> {
            laidOut.add(laid);
            return emptyLayout(laid);
        });

        Assertions.assertEquals(List.of(slots), laidOut, "the layout must place exactly the destinations the menu was built with");
    }
}

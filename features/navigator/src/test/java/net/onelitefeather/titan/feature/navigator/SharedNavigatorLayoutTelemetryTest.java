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
import java.util.List;
import net.minestom.server.inventory.InventoryType;
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

    private static InventoryLayout emptyLayout(List<Destination> visible) {
        return InventoryLayout.fromType(InventoryType.CHEST_1_ROW);
    }

    private long layoutSpans() {
        return testTelemetry.spans().stream().map(SpanData::getName).filter(LAYOUT_SPAN::equals).count();
    }

    @DisplayName("The first layout for a menu is applied inside a navigator.layout.apply span")
    @Test
    void firstLayoutIsSpanned() {
        SharedNavigator navigator = new SharedNavigator(false, testTelemetry.telemetry());

        navigator.applyLayoutIfChanged(new FakeFeatureFlags(), SharedNavigatorLayoutTelemetryTest::emptyLayout);

        Assertions.assertEquals(1, layoutSpans(), "the first application must produce one layout span");
    }

    @DisplayName("Applying an unchanged layout again produces no layout span")
    @Test
    void unchangedLayoutProducesNoSpan() {
        SharedNavigator navigator = new SharedNavigator(false, testTelemetry.telemetry());
        FakeFeatureFlags flags = new FakeFeatureFlags();
        navigator.applyLayoutIfChanged(flags, SharedNavigatorLayoutTelemetryTest::emptyLayout);

        navigator.applyLayoutIfChanged(flags, SharedNavigatorLayoutTelemetryTest::emptyLayout);

        Assertions.assertEquals(1, layoutSpans(), "the second, unchanged application must not produce a span");
    }

    @DisplayName("A changed set of visible destinations applies a new layout and spans it")
    @Test
    void changedLayoutIsSpannedAgain() {
        SharedNavigator navigator = new SharedNavigator(false, testTelemetry.telemetry());
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", false);
        navigator.applyLayoutIfChanged(flags, SharedNavigatorLayoutTelemetryTest::emptyLayout);

        flags.set("NAVIGATOR_SLENDER", true);
        navigator.applyLayoutIfChanged(flags, SharedNavigatorLayoutTelemetryTest::emptyLayout);

        Assertions.assertEquals(2, layoutSpans(), "a changed layout must produce a span again");
    }
}

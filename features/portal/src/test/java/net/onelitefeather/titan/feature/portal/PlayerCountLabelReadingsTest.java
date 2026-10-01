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
package net.onelitefeather.titan.feature.portal;

import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.SourceType;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerCountLabelReadingsTest {

    private final FakePlayerCounts counts = new FakePlayerCounts();
    private final PlayerCountLabelReadings readings = new PlayerCountLabelReadings(this.counts, () -> 9);

    private LabelReading read(@Nullable LabelSource source) {
        PortalLabel label = new PortalLabel(new Vec(0, 64, 0), "x", null, source, Billboard.CENTER, 0f);
        return this.readings.read(new Portal("p", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "Survival", null, label), label);
    }

    @DisplayName("A missing source counts the task of the portal")
    @Test
    void missingSourceIsThePortalsTask() {
        this.counts.set(SourceType.TASK, "Survival", new PlayerCount(22, 60, true));

        Assertions.assertEquals(new LabelReading.Remote(new PlayerCount(22, 60, true)), read(null));
    }

    @DisplayName("Group and service sources ask the provider with their type and name")
    @Test
    void groupAndServiceAskTheProvider() {
        this.counts.set(SourceType.GROUP, "Games", new PlayerCount(5, 10, true));
        this.counts.set(SourceType.SERVICE, "Survival-1", new PlayerCount(4, 20, true));

        Assertions.assertEquals(new LabelReading.Remote(new PlayerCount(5, 10, true)), read(new LabelSource.Group("Games")));
        Assertions.assertEquals(new LabelReading.Remote(new PlayerCount(4, 20, true)), read(new LabelSource.Service("Survival-1")));
    }

    @DisplayName("The local source never asks the provider")
    @Test
    void localDoesNotAskTheProvider() {
        Assertions.assertEquals(new LabelReading.Local(9), read(new LabelSource.Local()));
        Assertions.assertEquals(0, this.counts.reads(), "local comes from the connections of this lobby");
    }

    @DisplayName("A type the provider does not support reads as not running without asking it")
    @Test
    void unsupportedTypeIsNotRunning() {
        this.counts.doNotSupport(SourceType.GROUP);

        Assertions.assertEquals(new LabelReading.Remote(PlayerCount.NOT_RUNNING), read(new LabelSource.Group("Games")));
        Assertions.assertEquals(0, this.counts.reads(), "an unsupported type is not asked");
    }

    @DisplayName("A name the provider does not know reads as not running")
    @Test
    void unknownNameIsNotRunning() {
        Assertions.assertEquals(new LabelReading.Remote(PlayerCount.NOT_RUNNING), read(new LabelSource.Task("Nope")));
    }
}

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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TitanPlayerLimitTest {

    // The holder is static by nature; every test leaves it empty again.
    @AfterEach
    void emptyHolder() {
        TitanPlayerLimit.clear();
    }

    @DisplayName("A limit announced after the listener registered reaches it")
    @Test
    void announceReachesListener() {
        List<Integer> seen = new ArrayList<>();
        TitanPlayerLimit.onAnnounce(seen::add);

        TitanPlayerLimit.announce(100);

        assertEquals(List.of(100), seen, "the bridge extension registers first, the application announces later");
    }

    @DisplayName("A limit announced before the listener registered is delivered on registration")
    @Test
    void earlyAnnounceIsReplayed() {
        List<Integer> seen = new ArrayList<>();
        TitanPlayerLimit.announce(50);

        TitanPlayerLimit.onAnnounce(seen::add);

        assertEquals(List.of(50), seen, "replayed to the late listener");
    }

    @DisplayName("Announcing without a listener does not fail")
    @Test
    void announceWithoutListener() {
        TitanPlayerLimit.announce(10);
    }

    @DisplayName("Clearing drops the listener")
    @Test
    void clearDropsListener() {
        List<Integer> seen = new ArrayList<>();
        TitanPlayerLimit.onAnnounce(seen::add);

        TitanPlayerLimit.clear();
        TitanPlayerLimit.announce(20);

        assertEquals(List.of(), seen, "the cleared listener hears nothing");
    }
}

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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import net.onelitefeather.titan.core.testfixtures.PlayerCountsContract;
import org.junit.jupiter.api.AfterEach;

/** The real provider, with a bridge lookup installed in the holder, against the shared contract. */
class HolderPlayerCountsContractTest implements PlayerCountsContract {

    // The holder is static by nature; every test leaves it empty again.
    @AfterEach
    void emptyHolder() {
        TitanPlayerCountLookup.setLookup(null);
    }

    @Override
    public PlayerCounts countsOf(SourceType type, String name, ServiceCount... running) {
        List<Map<String, Object>> rows = Arrays.stream(running).<Map<String, Object>>map(service -> Map.of(PlayerCountLookup.NAME, service.name(), PlayerCountLookup.ONLINE, service.online(), PlayerCountLookup.MAX, service.max())).toList();
        TitanPlayerCountLookup.setLookup(new PlayerCountLookup() {
            @Override
            public boolean supports(String requested) {
                return true;
            }

            @Override
            public List<Map<String, Object>> running(String requested, String requestedName) {
                return requested.equals(type.id()) && requestedName.equals(name) ? rows : List.of();
            }
        });
        return new HolderPlayerCounts();
    }
}

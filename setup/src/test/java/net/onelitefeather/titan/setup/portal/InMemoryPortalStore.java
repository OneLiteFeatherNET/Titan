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
package net.onelitefeather.titan.setup.portal;

import net.onelitefeather.titan.core.portal.Portal;

import java.util.ArrayList;
import java.util.List;

/** A {@link PortalStore} that keeps the list in memory and counts how often it was written. */
public final class InMemoryPortalStore implements PortalStore {

    private List<Portal> portals;
    private int saves;

    public InMemoryPortalStore(Portal... portals) {
        this.portals = List.of(portals);
    }

    @Override
    public List<Portal> portals() {
        return portals;
    }

    @Override
    public void save(List<Portal> portals) {
        this.portals = new ArrayList<>(portals);
        this.saves++;
    }

    @Override
    public String world() {
        return "test-world";
    }

    public int saves() {
        return saves;
    }
}

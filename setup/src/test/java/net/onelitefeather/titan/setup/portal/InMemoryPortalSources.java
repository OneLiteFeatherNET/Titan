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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** A {@link PortalSources} with fixed worlds; a world can be marked as unreadable. */
public final class InMemoryPortalSources implements PortalSources {

    private final String active;
    private final Map<String, List<Portal>> worlds = new LinkedHashMap<>();
    private final Set<String> unreadable = new HashSet<>();

    public InMemoryPortalSources(String active) {
        this.active = active;
    }

    public InMemoryPortalSources world(String name, Portal... portals) {
        worlds.put(name, List.of(portals));
        return this;
    }

    public InMemoryPortalSources unreadable(String name) {
        unreadable.add(name);
        return this;
    }

    @Override
    public String active() {
        return active;
    }

    @Override
    public List<String> worlds() {
        Set<String> names = new TreeSet<>(worlds.keySet());
        names.addAll(unreadable);
        names.remove(active);
        return new ArrayList<>(names);
    }

    @Override
    public Optional<List<Portal>> portalsOf(String world) {
        if (unreadable.contains(world)) {
            throw new PortalSourceException("map.json of '" + world + "' is not valid JSON");
        }
        return Optional.ofNullable(worlds.get(world));
    }
}

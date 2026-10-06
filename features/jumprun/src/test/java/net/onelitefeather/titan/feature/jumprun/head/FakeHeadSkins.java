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
package net.onelitefeather.titan.feature.jumprun.head;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minestom.server.entity.PlayerSkin;

/** Skins from a table instead of Mojang; counts what was asked, so tests can assert on it. */
public final class FakeHeadSkins implements HeadSkins {

    private final Map<UUID, Object> table;
    private final List<UUID> asked = new ArrayList<>();

    /** Each value is a {@link PlayerSkin}, or a {@link RuntimeException} the lookup throws. */
    public FakeHeadSkins(Map<UUID, Object> table) {
        this.table = table;
    }

    @Override
    public Optional<PlayerSkin> skinOf(UUID id) {
        asked.add(id);
        Object found = table.get(id);
        if (found instanceof RuntimeException failure) {
            throw failure;
        }
        return Optional.ofNullable((PlayerSkin) found);
    }

    public List<UUID> asked() {
        return List.copyOf(asked);
    }

    public static PlayerSkin skin(String name) {
        return new PlayerSkin("textures-" + name, "signature-" + name);
    }
}

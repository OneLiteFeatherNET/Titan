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
package net.onelitefeather.titan.common.blockhandler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class BeaconHandler implements BlockHandler {

    private static final Key NAMESPACE_ID = Key.key("minecraft:beacon");
    private static final List<Tag<?>> TAG_LIST = List.of(Tag.String("CustomName"), Tag.String("Lock"), Tag.Integer("Levels"), Tag.Integer("Primary"), Tag.Integer("Secondary"));

    @Override
    public @NotNull Key getKey() {
        return NAMESPACE_ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAG_LIST;
    }
}

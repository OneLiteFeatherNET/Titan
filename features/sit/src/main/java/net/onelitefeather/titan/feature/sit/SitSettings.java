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
package net.onelitefeather.titan.feature.sit;

import java.util.List;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;

/**
 * Pure parsing and validation for the {@code sit} config section, kept apart from how those values
 * are read ({@link SitModule#start()}).
 *
 * <p>{@code sit.offset} has no separate validation: it is built as a {@link Vec} from three
 * {@code Double::parseDouble} reads, which can never produce {@code null}.
 */
final class SitSettings {

    static final String OFFSET_X_KEY = "sit.offset.x";
    static final String OFFSET_Y_KEY = "sit.offset.y";
    static final String OFFSET_Z_KEY = "sit.offset.z";
    static final String ALLOWED_BLOCKS_KEY = "sit.allowedBlocks";

    private SitSettings() {
    }

    static Key parseBlock(String raw) {
        Key key;
        try {
            key = Key.key(raw);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(ALLOWED_BLOCKS_KEY + ": invalid block key '" + raw + "'");
        }
        if (Block.fromKey(key) == null) {
            throw new IllegalArgumentException(ALLOWED_BLOCKS_KEY + ": unknown block '" + raw + "'");
        }
        return key;
    }

    static List<Key> allowedBlocks(List<Key> allowedBlocks) {
        if (allowedBlocks == null) {
            throw new IllegalArgumentException(ALLOWED_BLOCKS_KEY + " must not be null");
        }
        if (allowedBlocks.isEmpty()) {
            throw new IllegalArgumentException(ALLOWED_BLOCKS_KEY + " must not be empty - a player could never sit down otherwise");
        }
        return allowedBlocks;
    }

}

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
package net.onelitefeather.titan.app.feature.sit;

import java.util.List;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.common.config.ConfigException;

/**
 * Pure validation for the {@code sit} section's values, kept apart from however those values are
 * read ({@link SitModule#enable}, via {@code io.avaje.config.Config}, including
 * {@code Config.getAs(key, Double::parseDouble)} for numbers).
 *
 * <p>Every method takes a plain value and either returns it (unchanged, or - for
 * {@link #parseBlock(String)} - converted) or throws
 * {@link ConfigException#invalid(String, String)} naming the value's full section key, e.g.
 * {@code sit.allowedBlocks}. None of these methods touch {@code io.avaje.config.Config} or a
 * server, so they are unit-testable on their own. There is no separate validation for
 * {@code sit.offset}: {@link SitModule#enable} builds it as a {@link Vec} from three
 * {@code Config.getAs(key, Double::parseDouble)} reads, which can never produce {@code null}, so
 * there is nothing to reject.
 *
 * <p>The keys themselves are declared here as constants, the one place this module's config
 * section is named (see {@code design.md}, decision 3), and reused by {@link SitModule#enable} to
 * read the raw values.
 */
final class SitSettings {

    static final String OFFSET_X_KEY = "sit.offset.x";
    static final String OFFSET_Y_KEY = "sit.offset.y";
    static final String OFFSET_Z_KEY = "sit.offset.z";
    static final String ALLOWED_BLOCKS_KEY = "sit.allowedBlocks";

    private SitSettings() {
    }

    /**
     * Parses one raw value of the {@code sit.allowedBlocks} list as a {@link Key} - a plain
     * string such as {@code minecraft:spruce_stairs}, read directly from
     * {@code io.avaje.config.Config} by {@link SitModule#enable}. Applies exactly the same rule
     * {@link Key#key(String)} always has: a string with characters a {@link Key} cannot contain
     * (e.g. spaces or uppercase letters) is invalid. Beyond syntax, the key must also name a block
     * Minestom knows about ({@link Block#fromKey(Key)}) - the {@code lobby-module-config} spec
     * lists
     * "ein unbekannter Block" alongside a syntactically invalid one as an invalid value.
     *
     * @param raw one raw entry of the configured {@code sit.allowedBlocks} list
     * @return {@code raw}, parsed as a {@link Key}
     * @throws ConfigException if {@code raw} is not a syntactically valid key, or is valid but
     *                         names no known block
     */
    static Key parseBlock(String raw) {
        Key key;
        try {
            key = Key.key(raw);
        } catch (InvalidKeyException e) {
            throw ConfigException.invalid(ALLOWED_BLOCKS_KEY, "must be a valid block key, was '" + raw + "'");
        }
        if (Block.fromKey(key) == null) {
            throw ConfigException.invalid(ALLOWED_BLOCKS_KEY, "must name a known block, was '" + raw + "'");
        }
        return key;
    }

    /**
     * @param allowedBlocks the block keys a player may sit down on
     * @return {@code allowedBlocks}, unchanged
     * @throws ConfigException if {@code allowedBlocks} is {@code null} or empty - a player could
     *                         never sit down otherwise
     */
    static List<Key> allowedBlocks(List<Key> allowedBlocks) {
        if (allowedBlocks == null) {
            throw ConfigException.invalid(ALLOWED_BLOCKS_KEY, "must not be null");
        }
        if (allowedBlocks.isEmpty()) {
            throw ConfigException.invalid(ALLOWED_BLOCKS_KEY, "must not be empty - a player could never sit down otherwise");
        }
        return allowedBlocks;
    }
}

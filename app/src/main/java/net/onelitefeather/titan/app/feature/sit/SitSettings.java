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

/**
 * Pure parsing and validation for the {@code sit} section's values, kept apart from however those
 * values are read ({@link SitModule#start()}, via {@code io.avaje.config.Config}, including
 * {@code Config.getAs(key, Double::parseDouble)} for numbers).
 *
 * <p>Neither method here is read through {@code Config.getAs}'s mapping function:
 * {@link #parseBlock(String)} runs once per entry of the {@code sit.allowedBlocks} list, read via
 * {@code Config.list().of(key)} - a plain list of strings, not wrapped by {@code getAs} - and
 * {@link #allowedBlocks(List)} is a cross-check over the whole, already-parsed list. Both therefore
 * self-name {@link #ALLOWED_BLOCKS_KEY} in their own message. There is no separate validation for
 * {@code sit.offset}: {@link SitModule#start()} builds it as a {@link Vec} from three
 * {@code Config.getAs(key, Double::parseDouble)} reads, which can never produce {@code null}, so
 * there is nothing to reject.
 *
 * <p>The keys themselves are declared here as constants, the one place this module's config
 * section is named (see {@code design.md}, decision 3), reused both by {@link SitModule#start()}'s
 * one strict, startup-only read and by its {@code PlayerBlockInteractEvent} listener's live,
 * unvalidated read on every interaction (see {@code openspec/changes/config-reload-feature-flags/
 * design.md}, decision 2, as amended by {@code refactor/drop-runtime-fallback}: a runtime read is
 * never re-validated and never falls back to a shipped default). {@link #parseBlock(String)} is
 * reused at both points because it is the only way to turn a raw string into a {@link Key} at all,
 * not because the runtime read is validated - {@link #allowedBlocks(List)}'s empty-list check, by
 * contrast, only ever runs once, in {@link SitModule#start()}.
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
     * {@code io.avaje.config.Config} by {@link SitModule#start()}. Applies exactly the same rule
     * {@link Key#key(String)} always has: a string with characters a {@link Key} cannot contain
     * (e.g. spaces or uppercase letters) is invalid. Beyond syntax, the key must also name a block
     * Minestom knows about ({@link Block#fromKey(Key)}) - the {@code lobby-module-config} spec
     * lists "ein unbekannter Block" alongside a syntactically invalid one as an invalid value.
     *
     * @param raw one raw entry of the configured {@code sit.allowedBlocks} list
     * @return {@code raw}, parsed as a {@link Key}
     * @throws IllegalArgumentException if {@code raw} is not a syntactically valid key, or is valid
     *                                  but names no known block; either way the message names
     *                                  {@link #ALLOWED_BLOCKS_KEY}
     */
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

    /**
     * @param allowedBlocks the block keys a player may sit down on
     * @return {@code allowedBlocks}, unchanged
     * @throws IllegalArgumentException if {@code allowedBlocks} is {@code null} or empty - a player
     *                                  could never sit down otherwise; the message names
     *                                  {@link #ALLOWED_BLOCKS_KEY}
     */
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

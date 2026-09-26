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

import io.avaje.config.Config;
import java.util.List;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.common.config.RuntimeConfigFallback;

/**
 * Pure parsing and validation for the {@code sit} section's values, kept apart from however those
 * values are read ({@link SitModule#enable}, via {@code io.avaje.config.Config}, including
 * {@code Config.getAs(key, Double::parseDouble)} for numbers).
 *
 * <p>Neither method here is read through {@code Config.getAs}'s mapping function:
 * {@link #parseBlock(String)} runs once per entry of the {@code sit.allowedBlocks} list, read via
 * {@code Config.list().of(key)} - a plain list of strings, not wrapped by {@code getAs} - and
 * {@link #allowedBlocks(List)} is a cross-check over the whole, already-parsed list. Both therefore
 * self-name {@link #ALLOWED_BLOCKS_KEY} in their own message. There is no separate validation for
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

    /**
     * The {@link RuntimeConfigFallback#resolve} {@code parseAndValidate} function for one
     * {@code sit.offset.*} component.
     *
     * @param raw the configured value, as text
     * @return {@code raw}, parsed
     * @throws NumberFormatException if {@code raw} does not parse as a {@code double}
     */
    static double parseOffsetComponent(String raw) {
        return Double.parseDouble(raw);
    }

    /**
     * The {@link RuntimeConfigFallback#resolve} {@code parseAndValidate} function for
     * {@code sit.allowedBlocks}, built on {@link #parseBlock(String)} and
     * {@link #allowedBlocks(List)} (see design.md, decision 2): every raw entry is parsed and the
     * resulting list is cross-checked as a whole - a single bad entry, or an empty list, fails
     * the whole raw list, falling it back to the shipped default as a whole rather than dropping
     * just the one bad entry, since there is no meaningful "this one entry's own shipped default"
     * to substitute.
     *
     * @param raw the configured block keys, as text
     * @return {@code raw}, parsed and validated
     * @throws IllegalArgumentException if any entry is invalid, unknown, or the list is empty
     */
    static List<Key> parseAllowedBlocks(List<String> raw) {
        return allowedBlocks(raw.stream().map(SitSettings::parseBlock).toList());
    }

    /**
     * Reads {@code sit.offset} live through the static facade, resolving an invalid or
     * persistently-invalid runtime value per component to its own shipped default via the
     * process-wide {@link RuntimeConfigFallback}. Called directly by {@link SitModule}'s
     * {@code PlayerBlockInteractEvent} listener on every block interaction (see design.md,
     * decision 1), so a changed offset applies the next time a player sits down, without a
     * module restart - a player already sitting is unaffected, since their seat entity was
     * already placed at the offset that applied when they sat down. The shipped default for a
     * component is read only if that component's live value turns out invalid, never on every
     * call.
     *
     * @return the current, valid seat offset
     */
    static Vec currentOffset() {
        RuntimeConfigFallback fallback = RuntimeConfigFallback.shared();
        double x = fallback.resolve(OFFSET_X_KEY, Config.get(OFFSET_X_KEY), SitSettings::parseOffsetComponent, () -> fallback.shippedDefaults().getAs(OFFSET_X_KEY, Double::parseDouble));
        double y = fallback.resolve(OFFSET_Y_KEY, Config.get(OFFSET_Y_KEY), SitSettings::parseOffsetComponent, () -> fallback.shippedDefaults().getAs(OFFSET_Y_KEY, Double::parseDouble));
        double z = fallback.resolve(OFFSET_Z_KEY, Config.get(OFFSET_Z_KEY), SitSettings::parseOffsetComponent, () -> fallback.shippedDefaults().getAs(OFFSET_Z_KEY, Double::parseDouble));
        return new Vec(x, y, z);
    }

    /**
     * Reads {@code sit.allowedBlocks} live through the static facade, resolving an invalid or
     * persistently-invalid runtime value to the shipped default list via the process-wide
     * {@link RuntimeConfigFallback}. Called directly by {@link SitModule}'s
     * {@code PlayerBlockInteractEvent} listener on every block interaction (see design.md,
     * decision 1) - the shipped default list is read only if the live list turns out invalid,
     * never on every call.
     *
     * @return the current, valid list of allowed block keys
     */
    static List<Key> currentAllowedBlocks() {
        RuntimeConfigFallback fallback = RuntimeConfigFallback.shared();
        return fallback.resolve(ALLOWED_BLOCKS_KEY, Config.list().of(ALLOWED_BLOCKS_KEY), SitSettings::parseAllowedBlocks, () -> fallback.shippedDefaults().list().of(ALLOWED_BLOCKS_KEY).stream().map(SitSettings::parseBlock).toList());
    }
}

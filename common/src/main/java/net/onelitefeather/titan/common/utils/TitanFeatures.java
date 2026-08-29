/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.utils;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.context.FeatureContext;

/**
 * Every flag Titan reads from {@code flags.properties}.
 *
 * <p>Most of these are visibility flags: they decide whether a player is shown a navigator
 * entry. {@link #RESOURCE_PACK_REQUIRED_KICK} is not one of them - it switches a
 * <em>behaviour</em> on and off for the whole lobby and is documented as such on the constant
 * itself.
 *
 * <p>NFR-009 caps this enum at twelve constants. A flag that only ever has one value in
 * production does not belong here.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public enum TitanFeatures implements Feature, ThreadHelper {

    NAVIGATOR_CREATIVE, NAVIGATOR_SLENDER, NAVIGATOR_MANIS, NAVIGATOR_SURVIVAL, NAVIGATOR_ELYTRA,

    /**
     * Whether a resource pack configured {@code "required": true} is actually enforced, meaning
     * the lobby disconnects a client that does not load it.
     *
     * <p>This is a server-wide switch for a behaviour, not a question about who may see
     * something. It is therefore read as a plain kill switch: only the enabled/disabled bit of
     * the flag is consulted, never a release stage and never the player's permissions. Two
     * reasons for that. A stage would mean the same silent client is disconnected or admitted
     * depending on which group it belongs to, which is not something an operator can reason
     * about while an incident is running. And the population a stage would test the kick on
     * first - the team - is the least representative sample there is for the question this
     * behaviour actually raises, namely whether ordinary clients on ordinary connections manage
     * to download the pack in time.
     *
     * <p><b>Active</b> (the default, see {@link EnabledByDefault}): the pack is pushed with the
     * {@code forced} bit set, so the client shows the "you must accept this pack" dialog and
     * Minestom disconnects it on any terminal answer other than success. That includes the
     * {@code DISCARDED} answer the timeout guard gives on behalf of a client that never
     * answered at all.
     *
     * <p><b>Inactive</b>: the very same pack is pushed as optional. Nothing about the pack
     * changes except that neither a declined download nor an expired timeout costs the player
     * their session; they reach the lobby without the pack.
     *
     * <p>The flag deliberately covers both terminal answers rather than the timeout alone.
     * Minestom stores the {@code required} bit when the pack is pushed and consults it when the
     * answer arrives, so there is no supported way to enforce a pack against a decline but not
     * against a timeout - and a lobby that showed a forced-pack dialog it does not back up
     * would mislead the player as much as it would the operator.
     *
     * <p>The default is active, so a lobby with no {@code flags.properties} - or one whose file
     * cannot be read - behaves exactly as it did before this flag existed and keeps the promise
     * an operator made by writing {@code "required": true}. Failing the other way would silently
     * downgrade an explicit configuration to a suggestion. A kick is also the recoverable
     * outcome of the two: the player reconnects and tries again, whereas a lobby that quietly
     * stops enforcing its pack is a state nobody notices until the textures are wrong for
     * everyone.
     */
    @EnabledByDefault RESOURCE_PACK_REQUIRED_KICK,;

    @Override
    public boolean isActive() {
        return syncThreadForServiceLoader(() -> FeatureContext.getFeatureManager().isActive(this));
    }
}

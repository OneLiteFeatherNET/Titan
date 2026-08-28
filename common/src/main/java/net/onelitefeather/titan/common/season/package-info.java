/**
 * Seasons as data (spec stage 4).
 *
 * <p>The premise the package is built on is measured rather than assumed: cosmetic seasonal events
 * move concurrent players by roughly nothing, while content moves them by a lot. A season that
 * only decorates therefore has to be nearly free to add, or it is a net loss. InnoGames, six
 * iterations into the same event, put the same thing the other way round — the currency and the
 * 21-day runtime stayed constant, everything else was open to change.
 *
 * <p>So the split here is deliberate and it is the whole design:
 *
 * <ul>
 * <li><b>Stable code</b> — the time window, the release gate, the priority order and the world
 * selection. They live in this package and in
 * {@link net.onelitefeather.titan.common.feature.FeatureGate}, and they do not change when a
 * season changes.</li>
 * <li><b>Data</b> — one JSON file per season next to the process, plus the world directory it
 * names. Adding a season is dropping a file in; it requires no Java, and if it ever does, this
 * design has failed.</li>
 * </ul>
 *
 * <p>The boundary between the two is the question "is this a new <em>value</em> or a new
 * <em>verb</em>?". A pumpkin being orange is a value and belongs in JSON. A pumpkin exploding when
 * somebody walks over it is a verb and belongs in a
 * {@link net.onelitefeather.titan.common.season.SeasonEffect}. The moment the JSON grows an
 * {@code if}, it has become a programming language without a type checker, a debugger or a stack
 * trace.
 *
 * <p>What keeps that honest is the sealed hierarchy: an effect type nobody handles does not reach
 * production, it breaks the compile, and an effect type nobody implements does not reach the world
 * either — the loader refuses the file and names the type (US-4.04).
 *
 * <p>{@link net.onelitefeather.titan.common.season.SeasonalContent#deactivate()} is the other load
 * bearing part. Everything a season places is written down as it is placed, and taken back in the
 * reverse order, so the end of a season is not a cleanup task somebody has to remember.
 */
@NotNullByDefault
package net.onelitefeather.titan.common.season;

import org.jetbrains.annotations.NotNullByDefault;

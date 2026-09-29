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
package net.onelitefeather.titan.feature.season;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the active season for a point in time. Pure: no configuration, no clock.
 *
 * <p>Overlapping windows resolve to the season with the earlier {@code from}, and on a tie to the
 * smaller id, so the outcome never depends on the order the configuration lists them in.
 */
final class SeasonCalendar {

    /** Earlier start first, then smaller id. */
    private static final Comparator<Season> PRECEDENCE = Comparator.comparing(Season::from).thenComparing(Season::id);

    /** Two enabled seasons whose windows share time; {@code winner} takes precedence. */
    record Overlap(Season winner, Season loser) {
    }

    Optional<Season> activeAt(List<Season> seasons, Instant now, ZoneId zone) {
        LocalDateTime local = LocalDateTime.ofInstant(now, zone);
        return seasons.stream().filter(Season::enabled).filter(season -> !local.isBefore(season.from()) && local.isBefore(season.to())).min(PRECEDENCE);
    }

    List<Overlap> overlaps(List<Season> seasons) {
        List<Season> enabled = seasons.stream().filter(Season::enabled).sorted(PRECEDENCE).toList();
        List<Overlap> overlaps = new ArrayList<>();
        for (int i = 0; i < enabled.size(); i++) {
            for (int j = i + 1; j < enabled.size(); j++) {
                Season winner = enabled.get(i);
                Season loser = enabled.get(j);
                if (loser.from().isBefore(winner.to())) {
                    overlaps.add(new Overlap(winner, loser));
                }
            }
        }
        return overlaps;
    }
}

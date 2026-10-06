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
package net.onelitefeather.titan.feature.jumprun.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.onelitefeather.titan.feature.jumprun.course.Mode;

/**
 * A {@link RunStore} in memory with the semantics of the Postgres one, which {@link
 * RunStoreContract} checks, and the option to fail like a lost database.
 */
public final class FakeRunStore implements RunStore {

    private final List<FinishedRun> runs = new CopyOnWriteArrayList<>();
    private volatile RuntimeException failure;

    /** Every following access throws {@code failure}, until {@link #recover()}. */
    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    void recover() {
        this.failure = null;
    }

    @Override
    public Map<Mode, Integer> bestsOf(UUID player) {
        failIfBroken();
        Map<Mode, Integer> bests = new EnumMap<>(Mode.class);
        this.runs.stream().filter(run -> run.player().equals(player)).forEach(run -> bests.merge(run.mode(), run.score(), Math::max));
        return bests;
    }

    @Override
    public Map<Mode, TopThree> topThreeOfEveryMode() {
        failIfBroken();
        Map<Mode, TopThree> top = new EnumMap<>(Mode.class);
        this.runs.stream().collect(Collectors.groupingBy(FinishedRun::mode)).forEach((mode, ofMode) -> {
            TopThree ranking = TopThree.EMPTY;
            for (TopEntry entry : bestPerPlayer(ofMode)) {
                ranking = ranking.with(entry);
            }
            top.put(mode, ranking);
        });
        return top;
    }

    @Override
    public void append(FinishedRun run) {
        failIfBroken();
        this.runs.add(run);
    }

    private List<TopEntry> bestPerPlayer(List<FinishedRun> ofMode) {
        List<TopEntry> entries = new ArrayList<>();
        ofMode.stream().collect(Collectors.groupingBy(FinishedRun::player)).forEach((player, ofPlayer) -> {
            FinishedRun best = ofPlayer.stream().min(Comparator.comparingInt(FinishedRun::score).reversed().thenComparing(FinishedRun::finishedAt)).orElseThrow();
            // Like "order by finished_at desc, id desc": on equal times the later appended run wins.
            String name = this.runs.stream().filter(run -> run.player().equals(player)).reduce((a, b) -> b.finishedAt().isBefore(a.finishedAt()) ? a : b).orElseThrow().name();
            entries.add(new TopEntry(player, name, best.score(), best.finishedAt()));
        });
        return entries;
    }

    private void failIfBroken() {
        if (this.failure != null) {
            throw this.failure;
        }
    }
}

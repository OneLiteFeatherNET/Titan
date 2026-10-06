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
package net.onelitefeather.titan.feature.jumprun;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.scoreboard.Sidebar.NumberFormat;
import net.minestom.server.scoreboard.Sidebar.ScoreboardLine;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.persistence.TopThree;

/**
 * The sidebar of the runner: score, record and the top three of the mode. Presentation only; what
 * it says comes from {@link RunSidebarContent}. Minestom sends the lines, this only decides which
 * lines changed, so an unchanged line costs no packet.
 *
 * <p>Not thread-safe by itself: the caller holds the lock of the run.
 */
final class RunSidebar {

    private final Player runner;
    private final Sidebar sidebar;
    private final RunSidebarContent content;
    private List<SidebarLine> shown = List.of();
    private boolean attached;

    RunSidebar(Player runner, Mode mode, RunSidebarContent content) {
        this.runner = runner;
        this.content = content;
        this.sidebar = new Sidebar(RunSidebarContent.title(mode));
    }

    /**
     * Shows the values, creating the sidebar for the runner on first use.
     *
     * @param top the leaderboard of the run's mode, empty without a leaderboard
     */
    void show(int score, OptionalInt best, Optional<TopThree> top) {
        List<SidebarLine> lines = content.lines(score, best, top, runner.getUuid(), runner.getLocale());
        for (int index = 0; index < lines.size(); index++) {
            SidebarLine line = lines.get(index);
            if (index >= shown.size()) {
                // A fixed score per position keeps the order when the number of places changes.
                sidebar.createLine(new ScoreboardLine(lineId(index), line.text(), RunSidebarContent.MAX_LINES - index, numberFormat(line)));
            } else {
                update(lineId(index), shown.get(index), line);
            }
        }
        for (int index = lines.size(); index < shown.size(); index++) {
            sidebar.removeLine(lineId(index));
        }
        shown = lines;
        if (!attached) {
            attached = true;
            sidebar.addViewer(runner);
        }
    }

    private void update(String id, SidebarLine before, SidebarLine now) {
        if (!now.text().equals(before.text())) {
            sidebar.updateLineContent(id, now.text());
        }
        if (!now.value().equals(before.value())) {
            sidebar.updateLineNumberFormat(id, numberFormat(now));
        }
    }

    /** The value takes the place of the number; without one the number stays hidden. */
    private static NumberFormat numberFormat(SidebarLine line) {
        return line.value().map(NumberFormat::fixed).orElseGet(NumberFormat::blank);
    }

    void remove() {
        if (attached) {
            attached = false;
            // Minestom's removeViewer resets the scores after destroying the objective; the client warns about that.
            for (int index = 0; index < shown.size(); index++) {
                sidebar.removeLine(lineId(index));
            }
            shown = List.of();
            sidebar.removeViewer(runner);
        }
    }

    private static String lineId(int index) {
        return "line-" + index;
    }
}

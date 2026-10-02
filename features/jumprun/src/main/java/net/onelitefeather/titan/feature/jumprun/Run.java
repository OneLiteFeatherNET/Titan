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

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

/**
 * One player's running course. The start block is a real block of the world, so it is never part
 * of what is shown or reset.
 *
 * <p>The player's thread moves the course while the scheduler, the chunk events and the shutdown
 * read it, so a run is its own lock: every access to the course goes through a synchronized method
 * here, and a caller that must keep "still registered" and "show" or "remove" and "reset" together
 * holds {@code synchronized (run)} around both.
 */
final class Run {

    private final Player player;
    private final Course course;
    private final BlockPos startBlock;
    private final Spectators spectators;
    private final ScoreLabel label;
    private final OptionalInt previousBest;
    private boolean recordSounded;

    Run(Player player, Course course, BlockPos startBlock, OptionalInt previousBest) {
        this.previousBest = previousBest;
        this.player = player;
        this.course = course;
        this.startBlock = startBlock;
        this.spectators = new Spectators(player);
        this.label = new ScoreLabel(player);
    }

    Player player() {
        return player;
    }

    synchronized Course.Advance advanceTo(Pos feet) {
        return course.advanceTo(feet);
    }

    synchronized boolean hasFallen(double y) {
        return course.hasFallen(y);
    }

    synchronized double fallThreshold() {
        return course.fallThreshold();
    }

    synchronized int currentIndex() {
        return course.currentIndex();
    }

    synchronized int score() {
        return course.score();
    }

    /**
     * Whether the score has just passed the best from before the run, which is true once per run.
     * Without a previous best the end of the run announces the record instead.
     */
    synchronized boolean passesPreviousBest() {
        if (recordSounded || previousBest.isEmpty() || course.score() <= previousBest.getAsInt()) {
            return false;
        }
        recordSounded = true;
        return true;
    }

    boolean hadNoRecord() {
        return previousBest.isEmpty();
    }

    /** What the other players see; touch it only while holding the lock of the run. */
    Spectators spectators() {
        return spectators;
    }

    /**
     * The score over the runner for the others; touch it only while holding the lock of the run.
     */
    ScoreLabel label() {
        return label;
    }

    Pos startPoint() {
        return course.startPoint();
    }

    /** The blocks of the visible window that exist only on the player's screen. */
    synchronized List<CourseBlock> fakeWindow() {
        return fake(course.window());
    }

    List<CourseBlock> fake(Collection<CourseBlock> blocks) {
        return blocks.stream().filter(block -> !block.pos().equals(startBlock)).toList();
    }

    synchronized boolean showsFakeBlockAt(int x, int y, int z) {
        BlockPos target = new BlockPos(x, y, z);
        return fakeWindow().stream().anyMatch(block -> block.pos().equals(target));
    }
}

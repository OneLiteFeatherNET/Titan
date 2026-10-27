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
package net.onelitefeather.titan.feature.radio;

/**
 * Where a player is in the radio window: the overview or one category, on which page, and which
 * category Next continues in. Pure state, so the paging and the generation are tested without a
 * player.
 */
final class RadioSessionState {

    private RadioCategory openCategory;
    private RadioCategory lastCategory;
    private int page = 1;
    private long generation;

    /** The category on screen, {@code null} in the overview. */
    RadioCategory openCategory() {
        return this.openCategory;
    }

    boolean inOverview() {
        return this.openCategory == null;
    }

    /** The category the player opened last, kept after going back; Next starts in it. */
    RadioCategory lastCategory() {
        return this.lastCategory;
    }

    int page() {
        return this.page;
    }

    void page(int page) {
        this.page = page;
    }

    void openCategory(RadioCategory category) {
        this.openCategory = category;
        this.lastCategory = category;
        this.page = 1;
    }

    void showOverview() {
        this.openCategory = null;
        this.page = 1;
    }

    long generation() {
        return this.generation;
    }

    /**
     * Moves to a new generation on every start, stop, next and logout; a late result is then
     * dropped.
     */
    long advance() {
        return ++this.generation;
    }
}

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
 * Where the controls sit in the six-row window: slots 0 to 44 hold the content, slots 45 to 53 the
 * nine controls. A control keeps its slot when it is not available, so the row never shifts.
 */
final class RadioMenuLayout {

    static final int SIZE = PageLayout.SLOT_COUNT;
    static final int CONTENT = PageLayout.PAGE_SIZE;

    static final int BACK = PageLayout.CONTROL_ROW_FIRST;
    static final int PREVIOUS = BACK + 1;
    static final int PAGE_INFO = BACK + 2;
    static final int STOP = BACK + 3;
    static final int NOW_PLAYING = BACK + 4;
    static final int NEXT = BACK + 5;
    static final int MODE = BACK + 6;
    static final int VOLUME = BACK + 7;
    static final int FORWARD = BACK + 8;

    /** One control of the bottom row. */
    enum Control {
        BACK, PREVIOUS, PAGE_INFO, STOP, NOW_PLAYING, NEXT, MODE, VOLUME, FORWARD, NONE
    }

    private RadioMenuLayout() {
    }

    /** The control under {@code slot}, or {@link Control#NONE} for a content slot. */
    static Control controlAt(int slot) {
        return switch (slot - BACK) {
            case 0 -> Control.BACK;
            case 1 -> Control.PREVIOUS;
            case 2 -> Control.PAGE_INFO;
            case 3 -> Control.STOP;
            case 4 -> Control.NOW_PLAYING;
            case 5 -> Control.NEXT;
            case 6 -> Control.MODE;
            case 7 -> Control.VOLUME;
            case 8 -> Control.FORWARD;
            default -> Control.NONE;
        };
    }
}

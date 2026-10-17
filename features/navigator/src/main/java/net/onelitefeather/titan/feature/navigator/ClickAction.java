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
package net.onelitefeather.titan.feature.navigator;

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.inventory.click.Click;

/**
 * What a click on a navigator entry does, and the lore line that names it. Shift only matters when
 * the entry has a portal; without one a shift click acts like the same click without shift.
 */
enum ClickAction {

    /** A minigame's left click: join the best ready round directly. */
    QUICK_JOIN(NavigatorMessages.KEY_LEFT, NavigatorMessages.ACTION_QUICK_JOIN),
    /** A minigame's right click: list its services. */
    PICK_SERVER(NavigatorMessages.KEY_RIGHT, NavigatorMessages.ACTION_CHOOSE_SERVER),
    /** A server's click: connect to its task. */
    CONNECT(NavigatorMessages.KEY_LEFT, NavigatorMessages.ACTION_CONNECT),
    /** A shift click on an entry with a portal: walk to the portal. */
    PORTAL(NavigatorMessages.KEY_SHIFT, NavigatorMessages.ACTION_PORTAL);

    private final String keyKey;
    private final String labelKey;

    ClickAction(String keyKey, String labelKey) {
        this.keyKey = keyKey;
        this.labelKey = labelKey;
    }

    /** The translation key of the mouse button this action is named by. */
    String keyKey() {
        return this.keyKey;
    }

    /** The translation key of what the click does. */
    String labelKey() {
        return this.labelKey;
    }

    /**
     * The action of one click on an entry; {@code minigame} is whether the entry offers the server
     * choice.
     */
    static ClickAction of(boolean minigame, Click click, boolean portal) {
        if (portal && isShift(click)) {
            return PORTAL;
        }
        if (!minigame) {
            return CONNECT;
        }
        return isRight(click) ? PICK_SERVER : QUICK_JOIN;
    }

    /**
     * The click lines an entry lists, in the order they are shown; the portal line only with a
     * portal.
     */
    static List<ClickAction> hints(boolean minigame, boolean portal) {
        List<ClickAction> hints = new ArrayList<>();
        if (minigame) {
            hints.add(QUICK_JOIN);
            hints.add(PICK_SERVER);
        } else {
            hints.add(CONNECT);
        }
        if (portal) {
            hints.add(PORTAL);
        }
        return List.copyOf(hints);
    }

    private static boolean isShift(Click click) {
        return click instanceof Click.LeftShift || click instanceof Click.RightShift;
    }

    private static boolean isRight(Click click) {
        return click instanceof Click.Right || click instanceof Click.RightShift;
    }
}

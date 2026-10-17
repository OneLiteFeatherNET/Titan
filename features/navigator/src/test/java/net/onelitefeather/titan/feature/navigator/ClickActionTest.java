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

import java.util.List;
import java.util.Optional;
import net.minestom.server.inventory.click.Click;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClickActionTest {

    private static final int SLOT = 0;

    @DisplayName("A left click on a minigame quick joins")
    @Test
    void leftClickOnMinigameQuickJoins() {
        Assertions.assertEquals(Optional.of(ClickAction.QUICK_JOIN), ClickAction.of(true, new Click.Left(SLOT), false), "the left click is the quick join");
    }

    @DisplayName("A right click on a minigame opens the server picker")
    @Test
    void rightClickOnMinigamePicksServer() {
        Assertions.assertEquals(Optional.of(ClickAction.PICK_SERVER), ClickAction.of(true, new Click.Right(SLOT), false), "the right click is the server choice");
    }

    @DisplayName("A right click on a minigame with a portal still opens the server picker")
    @Test
    void rightClickOnMinigameWithPortalPicksServer() {
        Assertions.assertEquals(Optional.of(ClickAction.PICK_SERVER), ClickAction.of(true, new Click.Right(SLOT), true), "the portal moved to shift, so a plain right click keeps the picker");
    }

    @DisplayName("A shift click on a minigame walks to the portal when there is one")
    @Test
    void shiftClickOnMinigameWithPortalWalks() {
        Assertions.assertEquals(Optional.of(ClickAction.PORTAL), ClickAction.of(true, new Click.LeftShift(SLOT), true), "shift-left walks to the portal");
        Assertions.assertEquals(Optional.of(ClickAction.PORTAL), ClickAction.of(true, new Click.RightShift(SLOT), true), "shift-right walks to the portal");
    }

    @DisplayName("A shift click on a minigame without a portal acts like the unshifted click")
    @Test
    void shiftClickOnMinigameWithoutPortalActsLikeTheUnshiftedClick() {
        Assertions.assertEquals(Optional.of(ClickAction.QUICK_JOIN), ClickAction.of(true, new Click.LeftShift(SLOT), false), "shift-left without a portal quick joins");
        Assertions.assertEquals(Optional.of(ClickAction.PICK_SERVER), ClickAction.of(true, new Click.RightShift(SLOT), false), "shift-right without a portal opens the picker");
    }

    @DisplayName("Only left and right clicks, with or without shift, name an action on a minigame")
    @Test
    void unnamedClickTypesNameNoActionOnMinigame() {
        Assertions.assertEquals(Optional.empty(), ClickAction.of(true, new Click.HotbarSwap(0, SLOT), false), "a number-key swap is no click the navigator acts on");
        Assertions.assertEquals(Optional.empty(), ClickAction.of(true, new Click.DropSlot(SLOT, false), false), "a Q drop is no click the navigator acts on");
        Assertions.assertEquals(Optional.empty(), ClickAction.of(true, new Click.OffhandSwap(SLOT), true), "an F swap is no click the navigator acts on, even with a portal");
        Assertions.assertEquals(Optional.empty(), ClickAction.of(true, new Click.Middle(SLOT), true), "a middle click is no click the navigator acts on, even with a portal");
        Assertions.assertEquals(Optional.empty(), ClickAction.of(true, new Click.Double(SLOT), false), "a double click is no click the navigator acts on");
    }

    @DisplayName("Only left and right clicks, with or without shift, name an action on a server")
    @Test
    void unnamedClickTypesNameNoActionOnServer() {
        Assertions.assertEquals(Optional.empty(), ClickAction.of(false, new Click.HotbarSwap(0, SLOT), true), "a number-key swap on a server does nothing");
        Assertions.assertEquals(Optional.empty(), ClickAction.of(false, new Click.DropSlot(SLOT, true), false), "a Q drop on a server does nothing");
        Assertions.assertEquals(Optional.empty(), ClickAction.of(false, new Click.Middle(SLOT), false), "a middle click on a server does nothing");
    }

    @DisplayName("A left click on a server connects")
    @Test
    void leftClickOnServerConnects() {
        Assertions.assertEquals(Optional.of(ClickAction.CONNECT), ClickAction.of(false, new Click.Left(SLOT), false), "the left click connects to the task");
    }

    @DisplayName("A right click on a server connects too, with or without a portal")
    @Test
    void rightClickOnServerConnects() {
        Assertions.assertEquals(Optional.of(ClickAction.CONNECT), ClickAction.of(false, new Click.Right(SLOT), false), "a right click connects to the server too");
        Assertions.assertEquals(Optional.of(ClickAction.CONNECT), ClickAction.of(false, new Click.Right(SLOT), true), "the portal needs shift, not a right click");
    }

    @DisplayName("A shift click on a server walks to its portal and connects without one")
    @Test
    void shiftClickOnServer() {
        Assertions.assertEquals(Optional.of(ClickAction.PORTAL), ClickAction.of(false, new Click.LeftShift(SLOT), true), "shift walks to the portal when one exists");
        Assertions.assertEquals(Optional.of(ClickAction.CONNECT), ClickAction.of(false, new Click.LeftShift(SLOT), false), "shift without a portal connects");
    }

    @DisplayName("A minigame without a portal lists quick join and the picker")
    @Test
    void minigameHintsWithoutPortal() {
        Assertions.assertEquals(List.of(ClickAction.QUICK_JOIN, ClickAction.PICK_SERVER), ClickAction.hints(true, false), "the hints follow the click mapping in order");
    }

    @DisplayName("A minigame with a portal adds the shift line last")
    @Test
    void minigameHintsWithPortal() {
        Assertions.assertEquals(List.of(ClickAction.QUICK_JOIN, ClickAction.PICK_SERVER, ClickAction.PORTAL), ClickAction.hints(true, true), "the portal line comes after the two click lines");
    }

    @DisplayName("A server lists only connect, and the portal line when a portal exists")
    @Test
    void serverHints() {
        Assertions.assertEquals(List.of(ClickAction.CONNECT), ClickAction.hints(false, false), "no right-click line on a server");
        Assertions.assertEquals(List.of(ClickAction.CONNECT, ClickAction.PORTAL), ClickAction.hints(false, true), "the portal line follows the connect line");
    }
}

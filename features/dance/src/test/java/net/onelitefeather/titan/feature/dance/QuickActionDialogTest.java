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
package net.onelitefeather.titan.feature.dance;

import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The quick-action dialog: one button that opens the per-player choice. */
class QuickActionDialogTest {

    @DisplayName("The quick-action dialog has exactly one button, which opens the choice")
    @Test
    void quickActionHasOneOpenButton() {
        Dialog.Notice notice = (Dialog.Notice) QuickActionDialog.quickAction();

        Assertions.assertInstanceOf(DialogAction.Custom.class, notice.action().action(), "the one button must send a custom action");
        Assertions.assertEquals(DanceKeys.OPEN, ((DialogAction.Custom) notice.action().action()).key(), "the button opens titan:dances/open");
    }
}

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

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The per-player choice: only allowed dances, Stop last, labels in the player's language. */
class DanceMenuTest {

    private static final UUID PLAYER = new UUID(7, 7);

    private DanceMessages messages;

    @BeforeEach
    void register() {
        this.messages = new DanceMessages();
        this.messages.register();
    }

    @AfterEach
    void unregister() {
        this.messages.close();
    }

    private Dialog.MultiAction dialog(FakePermissionService permissions, Locale locale) {
        return new DanceMenu(this.messages, new DancePermissions(permissions)).dialogFor(PLAYER, locale);
    }

    private static List<String> labels(Dialog.MultiAction dialog) {
        return dialog.actions().stream().map(button -> PlainTextComponentSerializer.plainText().serialize(button.label())).toList();
    }

    @DisplayName("A player without the spin permission sees the free dances and Stop, but not spin")
    @Test
    void hidesDancesWithoutPermission() {
        Dialog.MultiAction dialog = dialog(new FakePermissionService(), Locale.ENGLISH);

        Assertions.assertEquals(List.of("Wave", "Groove", "Party", "Stop"), labels(dialog), "the free dances, then Stop");
    }

    @DisplayName("A player with the spin permission also sees spin, before Stop")
    @Test
    void showsSpinWithPermission() {
        Dialog.MultiAction dialog = dialog(new FakePermissionService().allowEveryone("titan.dance.spin"), Locale.ENGLISH);

        Assertions.assertEquals(List.of("Wave", "Groove", "Party", "Spin", "Stop"), labels(dialog), "spin appears once the permission is there");
    }

    @DisplayName("The labels follow the player's language")
    @Test
    void labelsAreGerman() {
        Dialog.MultiAction dialog = dialog(new FakePermissionService(), Locale.GERMAN);

        Assertions.assertEquals(List.of("Winken", "Groove", "Party", "Stopp"), labels(dialog), "the German labels");
    }

    @DisplayName("Each dance button sends its id as the payload of titan:dance")
    @Test
    void danceButtonCarriesItsId() {
        Dialog.MultiAction dialog = dialog(new FakePermissionService(), Locale.ENGLISH);

        DialogActionButton wave = dialog.actions().getFirst();

        Assertions.assertInstanceOf(DialogAction.Custom.class, wave.action(), "a dance button must send a custom action");
        DialogAction.Custom action = (DialogAction.Custom) wave.action();
        Assertions.assertEquals(DanceKeys.DANCE, action.key(), "the dance button sends titan:dance");
        Assertions.assertEquals(Dance.WAVE, DancePayload.dance(action.payload()).orElseThrow(), "its payload names the dance");
    }

    @DisplayName("The Stop button sends titan:dances/stop and is the last button")
    @Test
    void stopButtonIsLast() {
        Dialog.MultiAction dialog = dialog(new FakePermissionService(), Locale.ENGLISH);

        DialogActionButton stop = dialog.actions().getLast();

        Assertions.assertEquals(new DialogAction.Custom(DanceKeys.STOP, null), stop.action(), "Stop sends titan:dances/stop without payload");
    }
}

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
package net.onelitefeather.titan.setup.listener;

import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.titan.setup.portal.DraftPreview;
import net.onelitefeather.titan.setup.portal.PortalEditor;
import net.onelitefeather.titan.setup.portal.PortalShow;

import java.util.UUID;
import java.util.function.Consumer;

/** Drops a leaving player's portal drafts and stops their preview and show. */
public final class PortalDisconnectListener implements Consumer<PlayerDisconnectEvent> {

    private final PortalEditor editor;
    private final DraftPreview preview;
    private final PortalShow show;

    public PortalDisconnectListener(PortalEditor editor, DraftPreview preview, PortalShow show) {
        this.editor = editor;
        this.preview = preview;
        this.show = show;
    }

    @Override
    public void accept(PlayerDisconnectEvent event) {
        UUID player = event.getPlayer().getUuid();
        editor.discardAll(player);
        preview.stop(player);
        show.stop(player);
    }
}

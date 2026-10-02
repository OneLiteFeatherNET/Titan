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

import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.onelitefeather.titan.setup.portal.LabelPreview;

import java.util.function.Consumer;

/** Ends a player's text previews when they leave an instance: the display belongs to that world. */
public final class PortalInstanceChangeListener implements Consumer<RemoveEntityFromInstanceEvent> {

    private final LabelPreview labelPreview;

    public PortalInstanceChangeListener(LabelPreview labelPreview) {
        this.labelPreview = labelPreview;
    }

    @Override
    public void accept(RemoveEntityFromInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            labelPreview.clear(player.getUuid());
        }
    }
}

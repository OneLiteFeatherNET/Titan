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

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * The server picker: a left click on a mode that runs on several CloudNet services opens a list of
 * those services, and a click on one entry connects to exactly that service. Only the modes in
 * {@link #DESTINATIONS} offer it; every other mode, and every other click, works as before.
 */
final class ServerPicker {

    /** The modes whose left click opens the picker when they run on several services. */
    static final Set<Destination> DESTINATIONS = Set.of(Destination.ELYTRA_RACE, Destination.SLENDER);

    static final int COLUMNS = 9;
    static final int MAX_ROWS = 6;

    private static final String GRADIENT = "<gradient:#00ddff:#ffffff>";

    private ServerPicker() {
    }

    /** The title of the picker's inventory. */
    static Component title() {
        return MiniMessage.miniMessage().deserialize(GRADIENT + "Choose a server</gradient>");
    }

    /**
     * A left click opens the picker only with a choice: one service, or none, connects to the task.
     */
    static boolean opensFor(Destination destination, List<ServiceCount> running) {
        return DESTINATIONS.contains(destination) && running.size() > 1;
    }

    /** The services the picker lists, by name, at most as many as six rows hold. */
    static List<ServiceCount> entries(List<ServiceCount> running) {
        return running.stream().sorted(Comparator.comparing(ServiceCount::name)).limit((long) COLUMNS * MAX_ROWS).toList();
    }

    /**
     * The inventory rows that hold {@code entries} services: at least one, at most
     * {@link #MAX_ROWS}.
     */
    static int rows(int entries) {
        return Math.clamp((entries + COLUMNS - 1) / COLUMNS, 1, MAX_ROWS);
    }

    /**
     * One entry: its name, its players against the limit, and the state as the colour of its icon.
     */
    static ItemStack item(ServiceCount service) {
        MiniMessage mini = MiniMessage.miniMessage();
        TagResolver name = Placeholder.unparsed("name", service.name());
        TagResolver online = Placeholder.unparsed("online", String.valueOf(service.online()));
        TagResolver max = Placeholder.unparsed("max", service.limitAnnounced() ? String.valueOf(service.max()) : "?");
        Material material;
        String state;
        if (service.full()) {
            material = Material.RED_CONCRETE;
            state = "<red>Full</red>";
        } else if (!service.limitAnnounced()) {
            material = Material.YELLOW_CONCRETE;
            state = "<yellow>Not ready yet</yellow>";
        } else {
            material = Material.LIME_CONCRETE;
            state = "<green>Click to join</green>";
        }
        return ItemStack.builder(material).customName(mini.deserialize("<!i>" + GRADIENT + "<name></gradient>", name)).lore(List.of(mini.deserialize("<!i><gray>Players: <white><online></white>/<white><max></white></gray>", online, max), mini.deserialize("<!i>" + state))).build();
    }

    /** What the player is told when a picked server cannot be joined. */
    static Component refusal(ServiceCount service) {
        String reason = service.limitAnnounced() ? "That server is full." : "That server is not ready yet.";
        return MiniMessage.miniMessage().deserialize(GRADIENT + "Servers</gradient> <gray>»</gray> <red>" + reason + "</red>");
    }

    /** Whether a picked server can be joined: it has announced a limit and has a free slot. */
    static boolean joinable(ServiceCount service) {
        return service.limitAnnounced() && !service.full();
    }
}

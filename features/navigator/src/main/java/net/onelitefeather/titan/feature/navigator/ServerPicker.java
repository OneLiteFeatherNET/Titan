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
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * The server picker: the list of the CloudNet services of a minigame, opened by its right click,
 * and
 * the target of a quick join that found no ready service. A click on one entry connects to exactly
 * that service. Only the modes in {@link #DESTINATIONS} offer it.
 */
final class ServerPicker {

    /** The minigames: the modes that offer the server choice and the quick join. */
    static final Set<Destination> DESTINATIONS = Set.of(Destination.ELYTRA_RACE, Destination.SLENDER, Destination.BOUNCE);

    static final int COLUMNS = 9;
    static final int MAX_ROWS = 6;
    /** The slot of the explanation when the picker lists no service: the middle of its one row. */
    static final int EMPTY_SLOT = COLUMNS / 2;

    private static final String BRAND = "<gradient:#00ddff:#ffffff>";

    private ServerPicker() {
    }

    /** The state of an entry, which sets its icon and its state line. */
    private enum State {
        FULL(Material.RED_CONCRETE, NavigatorMessages.PICKER_FULL, "red"), NOT_READY(Material.YELLOW_CONCRETE, NavigatorMessages.PICKER_NOT_READY, "yellow"), JOINABLE(Material.LIME_CONCRETE, NavigatorMessages.PICKER_JOIN, "green");

        private final Material material;
        private final String key;
        private final String color;

        State(Material material, String key, String color) {
            this.material = material;
            this.key = key;
            this.color = color;
        }
    }

    /** The title of the picker's inventory in {@code locale}. */
    static Component title(Locale locale) {
        return NavigatorMessages.text(NavigatorMessages.PICKER_TITLE, locale);
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
     * One entry: the service's name in its destination's gradient, with the lore of {@link #lore}.
     */
    static ItemStack item(Destination destination, ServiceCount service, Locale locale) {
        return ItemStack.builder(material(service)).customName(name(destination, service)).lore(lore(service, locale)).build();
    }

    /** The name of an entry, in the gradient of the destination it belongs to. */
    static Component name(Destination destination, ServiceCount service) {
        TagResolver name = Placeholder.unparsed("name", service.name());
        return MiniMessage.miniMessage().deserialize("<!i>" + destination.gradient() + "<name></gradient>", name);
    }

    /**
     * The players of the service against its limit, a blank line, and the state as a line of its
     * own.
     */
    static List<Component> lore(ServiceCount service, Locale locale) {
        State state = stateOf(service);
        OptionalInt max = service.limitAnnounced() ? OptionalInt.of(service.max()) : OptionalInt.empty();
        Component stateLine = MiniMessage.miniMessage().deserialize("<!i>" + NavigatorMessages.ARROW + "<" + state.color + "><state></" + state.color + ">", Placeholder.component("state", NavigatorMessages.text(state.key, locale)));
        return List.of(NavigatorMessages.playersLine(service.online(), max, locale), Component.empty(), stateLine);
    }

    /**
     * The icon colour of an entry: red when full, yellow when it has no limit yet, lime when it can
     * be joined.
     */
    static Material material(ServiceCount service) {
        return stateOf(service).material;
    }

    /**
     * The one entry of a picker that lists no service: it says that nothing runs and does nothing
     * when clicked.
     */
    static ItemStack emptyItem(Locale locale) {
        Component name = MiniMessage.miniMessage().deserialize("<!i><gray><text></gray>", Placeholder.component("text", NavigatorMessages.text(NavigatorMessages.PICKER_EMPTY, locale)));
        return ItemStack.builder(Material.BARRIER).customName(name).build();
    }

    /** What the player is told when a picked server cannot be joined. */
    static Component refusal(ServiceCount service, Locale locale) {
        return refusalOf(service.limitAnnounced() ? NavigatorMessages.REFUSAL_FULL : NavigatorMessages.REFUSAL_NOT_READY, locale);
    }

    /** What the player is told when the services could not be read, so no list or join follows. */
    static Component unavailable(Locale locale) {
        return refusalOf(NavigatorMessages.REFUSAL_UNAVAILABLE, locale);
    }

    private static Component refusalOf(String key, Locale locale) {
        return MiniMessage.miniMessage().deserialize(BRAND + "Servers</gradient> <gray>»</gray> <red><reason></red>", Placeholder.component("reason", NavigatorMessages.text(key, locale)));
    }

    /** Whether a service can be joined: it has announced a limit and has a free slot. */
    static boolean joinable(ServiceCount service) {
        return service.limitAnnounced() && !service.full();
    }

    private static State stateOf(ServiceCount service) {
        if (service.full()) {
            return State.FULL;
        }
        return service.limitAnnounced() ? State.JOINABLE : State.NOT_READY;
    }
}

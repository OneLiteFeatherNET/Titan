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
    static final Set<Destination> DESTINATIONS = Set.of(Destination.ELYTRA_RACE, Destination.SLENDER, Destination.BOUNCE, Destination.TAMIAS);

    static final int COLUMNS = 9;
    /** The slot of the explanation when the picker lists no service: the middle of its one row. */
    static final int EMPTY_SLOT = COLUMNS / 2;

    /** The states in the order of the status sort: joinable first, then not ready, then full. */
    static final List<State> STATUS_ORDER = List.of(State.JOINABLE, State.NOT_READY, State.FULL);

    private static final String BRAND = "<gradient:#00ddff:#ffffff>";

    private ServerPicker() {
    }

    /** The state of an entry, which sets its icon and its state line. */
    enum State {
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

    /** Every service the picker lists, in the order {@code sort} gives. */
    static List<ServiceCount> entries(List<ServiceCount> running, PickerSort sort) {
        return running.stream().sorted(sort.comparator()).toList();
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

    /**
     * The sort item: its name carries the active order, its lore lists all four and, when there is
     * more than one page, the page. {@code page} is counted from zero.
     */
    static ItemStack sortItem(PickerSort active, Locale locale, int page, int pages) {
        Component name = NavigatorMessages.text(NavigatorMessages.SORT_ITEM, locale, NavigatorMessages.text(active.key(), locale));
        return ItemStack.builder(Material.COMPARATOR).customName(name).lore(sortLore(active, locale, page, pages)).build();
    }

    /**
     * Every order in the sort order, the active one marked with the arrow and in cyan, the others
     * grey, then a blank line, the page when there is more than one, and the click line.
     */
    static List<Component> sortLore(PickerSort active, Locale locale, int page, int pages) {
        MiniMessage mini = MiniMessage.miniMessage();
        List<Component> lore = new ArrayList<>();
        for (PickerSort sort : PickerSort.values()) {
            TagResolver order = Placeholder.component("order", NavigatorMessages.text(sort.key(), locale));
            if (sort == active) {
                lore.add(mini.deserialize("<!i>" + NavigatorMessages.ARROW + "<white><order></white>", order));
            } else {
                lore.add(mini.deserialize("<!i><gray>  <order></gray>", order));
            }
        }
        lore.add(Component.empty());
        if (pages > 1) {
            lore.add(pageText(page, pages, locale));
        }
        lore.add(mini.deserialize("<!i>" + NavigatorMessages.ARROW + "<gray><hint></gray>", Placeholder.component("hint", NavigatorMessages.text(NavigatorMessages.SORT_HINT, locale))));
        return List.copyOf(lore);
    }

    /**
     * The back button: it leads to the navigator, not to a page, so it does not name one. Its
     * material differs from the arrows' so that the two are told apart at a glance.
     */
    static ItemStack backItem(Locale locale) {
        return ItemStack.builder(Material.DARK_OAK_DOOR).customName(NavigatorMessages.text(NavigatorMessages.PICKER_BACK, locale)).build();
    }

    /**
     * The previous arrow, which names the page it leads to. {@code page} is the page shown, counted
     * from zero.
     */
    static ItemStack previousItem(int page, int pages, Locale locale) {
        return pageArrow(NavigatorMessages.PAGE_PREVIOUS, page - 1, pages, locale);
    }

    /**
     * The next arrow, which names the page it leads to. {@code page} is the page shown, counted
     * from zero.
     */
    static ItemStack nextItem(int page, int pages, Locale locale) {
        return pageArrow(NavigatorMessages.PAGE_NEXT, page + 1, pages, locale);
    }

    private static ItemStack pageArrow(String key, int target, int pages, Locale locale) {
        return ItemStack.builder(Material.ARROW).customName(NavigatorMessages.text(key, locale, pageText(target, pages, locale))).build();
    }

    /** "Page x/y" in {@code locale}; {@code page} is counted from zero and shown from one. */
    private static Component pageText(int page, int pages, Locale locale) {
        return NavigatorMessages.text(NavigatorMessages.PAGE_INDICATOR, locale, Component.text(String.valueOf(page + 1)), Component.text(String.valueOf(pages)));
    }

    /** What the player is told when a picked server cannot be joined. */
    static Component refusal(ServiceCount service, Locale locale) {
        return refusalOf(service.limitAnnounced() ? NavigatorMessages.REFUSAL_FULL : NavigatorMessages.REFUSAL_NOT_READY, locale);
    }

    /** What the player is told when the services could not be read, so no list or join follows. */
    static Component unavailable(Locale locale) {
        return refusalOf(NavigatorMessages.REFUSAL_UNAVAILABLE, locale);
    }

    /** What the player is told when a staging service was not ready within its wait. */
    static Component stagingTimeout(Destination destination, Locale locale) {
        return refusalOf(NavigatorMessages.text(NavigatorMessages.STAGING_TIMEOUT, locale, Component.text(destination.label())));
    }

    private static Component refusalOf(String key, Locale locale) {
        return refusalOf(NavigatorMessages.text(key, locale));
    }

    private static Component refusalOf(Component reason) {
        return MiniMessage.miniMessage().deserialize(BRAND + "Servers</gradient> <gray>»</gray> <red><reason></red>", Placeholder.component("reason", reason));
    }

    static State stateOf(ServiceCount service) {
        if (service.full()) {
            return State.FULL;
        }
        return service.limitAnnounced() ? State.JOINABLE : State.NOT_READY;
    }
}

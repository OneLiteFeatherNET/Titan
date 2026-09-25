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
package net.onelitefeather.titan.app.module.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.onelitefeather.titan.common.observability.TitanObservability;

/**
 * Platform-wide registry for {@link LobbyItem}s, one per lobby (see {@code design.md}, decision 7).
 *
 * <p>Three things live here, deliberately kept apart in {@link SlotConflictDetector} and
 * {@link EquipPlan}: stamping every registered stack with {@link #IDENTITY_TAG} so a used item can
 * be traced back to the module that registered it regardless of material or display name;
 * dispatching every {@link PlayerUseItemEvent} through a single listener on the platform node
 * instead of one per module; and computing, then applying, the standard lobby loadout on
 * {@link #equip(Player)}.
 *
 * <p>A module never talks to this class directly - it goes through the {@code ModuleItems} view
 * {@link #contextView} hands back, which ties every registration to that module's cleanup hooks.
 *
 * <p><b>Threading contract:</b> {@link #register}, {@link #unregister} and {@link #validate} run on
 * the thread that enables or disables modules (typically the main/startup thread), while
 * {@link #dispatch} runs on the tick thread for every {@link PlayerUseItemEvent} and
 * {@link #currentPlan()} (via {@link #equip(Player)}) may be called from either. Every read or
 * write
 * of {@link #registrations} - including the lookup {@link #dispatch} does before handing off to a
 * module's own handler - is {@code synchronized} on this instance, the same approach {@link
 * net.onelitefeather.titan.app.module.navigator.NavigatorEntries} takes, so a read during dispatch
 * never observes a registration or unregistration half-applied. A module's own handler itself runs
 * outside that lock, so a slow or reentrant handler cannot block a concurrent register() or
 * unregister(). The backing map stays a {@link LinkedHashMap} so {@link #currentPlan()} keeps
 * building {@link EquipPlan} in registration order.
 */
public final class ItemRegistry {

    /**
     * Stamped onto every registered item's stack on {@link #register}. Its value is that item's own
     * {@link LobbyItem#key()}, as a string - the identity {@link #dispatch} resolves a used stack
     * back to its handler with. A stack without this tag - a plain feather, say - is not a
     * registered item and is left alone.
     */
    public static final Tag<String> IDENTITY_TAG = Tag.String("titan:item");

    private final Map<String, Registration> registrations = new LinkedHashMap<>();
    private final List<DuplicateItemKeyDetector.Claim> keyClaims = new ArrayList<>();
    private final DuplicateItemKeyDetector duplicateKeyDetector = new DuplicateItemKeyDetector();
    private final SlotConflictDetector conflictDetector = new SlotConflictDetector();

    /**
     * @param platformNode the platform's shared event node; this registry's single dispatch
     *                     listener for {@link PlayerUseItemEvent} attaches to it immediately
     */
    public ItemRegistry(EventNode<Event> platformNode) {
        platformNode.addListener(PlayerUseItemEvent.class, this::dispatch);
    }

    /**
     * @param moduleId  the id of the module the returned view registers and unregisters items for
     * @param onDisable called with a cleanup action every time {@link ModuleItems#register} is
     *                  used, so the caller can run it when that module is disabled
     * @return a view of this registry scoped to that module
     */
    public ModuleItems contextView(String moduleId, Consumer<Runnable> onDisable) {
        return new ModuleItemsImpl(moduleId, this, onDisable);
    }

    /**
     * Checks every currently registered item's placement for conflicts and aborts startup if two
     * modules claimed the same one. Called by {@code ModuleRegistry.enableAll()} once every module
     * has enabled.
     *
     * @throws DuplicateItemKeyException      if two registrations - from any combination of
     *                                        modules - claimed the same {@link LobbyItem#key()}
     * @throws ItemPlacementConflictException if two modules registered an item for the same
     *                                        {@link ItemSlot}
     */
    public synchronized void validate() {
        this.duplicateKeyDetector.findConflict(this.keyClaims).ifPresent(conflict -> {
            throw new DuplicateItemKeyException(conflict);
        });
        List<SlotConflictDetector.Claim> claims = this.registrations.values().stream().map(registration -> new SlotConflictDetector.Claim(registration.moduleId(), registration.item().placement())).toList();
        this.conflictDetector.findConflict(claims).ifPresent(conflict -> {
            throw new ItemPlacementConflictException(conflict);
        });
    }

    /**
     * Clears {@code player}'s inventory and sets every currently registered item that has a fixed
     * placement.
     *
     * @param player the player to equip
     */
    public void equip(Player player) {
        currentPlan().applyTo(player);
    }

    /**
     * @param moduleId the id of the module registering {@code item}
     * @param item     the item to register
     * @return the stamped stack a module hands out itself for an unplaced item
     */
    synchronized ItemStack register(String moduleId, LobbyItem item) {
        this.keyClaims.add(new DuplicateItemKeyDetector.Claim(moduleId, item.key().asString()));
        ItemStack stamped = item.itemStack().withTag(IDENTITY_TAG, item.key().asString());
        LobbyItem stampedItem = new LobbyItem(item.key(), stamped, item.placement(), item.onUse());
        this.registrations.put(item.key().asString(), new Registration(moduleId, stampedItem));
        return stamped;
    }

    /**
     * @param key the registered item's key
     */
    synchronized void unregister(Key key) {
        this.registrations.remove(key.asString());
        // Also drop this key's claim, not only its registration - otherwise a module that
        // registers, unregisters (its own disable) and registers the same key again (a restart,
        // see ModuleRegistry#restart) would leave two claims for one key behind, and the next
        // validate() would report that as a conflict with itself.
        this.keyClaims.removeIf(claim -> claim.key().equals(key.asString()));
    }

    /**
     * @return the equip plan computed from every currently registered item, kept apart from
     *         {@link #equip(Player)} so the layout itself is testable without a {@link Player}
     */
    synchronized EquipPlan currentPlan() {
        List<LobbyItem> items = this.registrations.values().stream().map(Registration::item).toList();
        return EquipPlan.from(items);
    }

    private void dispatch(PlayerUseItemEvent event) {
        String keyValue = event.getItemStack().getTag(IDENTITY_TAG);
        if (keyValue == null) {
            return;
        }
        // Only the lookup itself needs the lock - running the module's own handler while holding
        // it would block register()/unregister() on other threads for as long as that handler
        // takes.
        Registration registration;
        synchronized (this) {
            registration = this.registrations.get(keyValue);
        }
        if (registration == null) {
            return;
        }
        Consumer<PlayerUseItemEvent> handler = TitanObservability.guard(registration.moduleId(), (PlayerUseItemEvent guardedEvent) -> registration.item().onUse().handle(guardedEvent.getPlayer(), guardedEvent));
        handler.accept(event);
    }

    private record Registration(String moduleId, LobbyItem item) {
    }
}

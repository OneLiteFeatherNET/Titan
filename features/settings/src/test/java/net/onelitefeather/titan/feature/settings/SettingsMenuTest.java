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
package net.onelitefeather.titan.feature.settings;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.minestom.server.inventory.click.Click;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class SettingsMenuTest {

    // The menu's gradient: the brand colours of the navigator and the hotbar items.
    private static final TextColor GRADIENT_START = TextColor.fromHexString("#00ddff");
    private static final TextColor GRADIENT_END = TextColor.fromHexString("#ffffff");

    // With two options the first sits in column 3 and the second in column 5: icons in row 1 (slots 9-17), switches in row 2 (slots 18-26).
    private static final int TAG_ICON = 12;
    private static final int TAG_SWITCH = 21;
    private static final int THROW_ICON = 14;
    private static final int THROW_SWITCH = 23;
    // With one option it sits in the middle column (4).
    private static final int SINGLE_ICON = 13;
    private static final int SINGLE_SWITCH = 22;

    @DisplayName("Each option shows its feature icon in the first row and its switch directly below it")
    @Test
    void iconAndSwitchStackPerOption(Env env) {
        Player player = connect(env);
        menu(SettingsFixtures.registry(SettingsFixtures.TAG, SettingsFixtures.THROW), new SettingsMessages()).open(player);
        AbstractInventory inventory = player.getOpenInventory();
        Assertions.assertEquals(Material.NAME_TAG, inventory.getItemStack(TAG_ICON).material(), "the tag option must show a name tag as its icon");
        Assertions.assertEquals(Material.ENDER_PEARL, inventory.getItemStack(THROW_ICON).material(), "the throw option must show an ender pearl as its icon");
        Assertions.assertEquals(Material.GRAY_DYE, inventory.getItemStack(TAG_SWITCH).material(), "the switch of an option that is off must be grey dye");
        Assertions.assertEquals(Material.LIME_DYE, inventory.getItemStack(THROW_SWITCH).material(), "the switch of an option that is on must be lime dye");
    }

    @DisplayName("Only the icon of an option that is on carries the enchantment glint")
    @Test
    void glintFollowsTheState(Env env) {
        Player player = connect(env);
        menu(SettingsFixtures.registry(SettingsFixtures.TAG, SettingsFixtures.THROW), new SettingsMessages()).open(player);
        AbstractInventory inventory = player.getOpenInventory();
        Assertions.assertFalse(hasGlint(inventory.getItemStack(TAG_ICON)), "the icon of an option that is off must not glint");
        Assertions.assertTrue(hasGlint(inventory.getItemStack(THROW_ICON)), "the icon of an option that is on must glint");
    }

    @DisplayName("An option name is not italic and runs through the brand gradient")
    @Test
    void optionNameIsGradientAndNotItalic(Env env) {
        Player player = connect(env);
        try (SettingsMessages messages = new SettingsMessages()) {
            messages.register();
            menu(SettingsFixtures.registry(SettingsFixtures.TAG, SettingsFixtures.THROW), messages).open(player);
            Component name = player.getOpenInventory().getItemStack(TAG_ICON).get(DataComponents.CUSTOM_NAME);
            Assertions.assertEquals(TextDecoration.State.FALSE, name.decoration(TextDecoration.ITALIC), "the option name must not be italic");
            Assertions.assertEquals(GRADIENT_START, firstColour(name), "the option name must start with the gradient's first colour");
            Assertions.assertEquals(GRADIENT_END, lastColour(name), "the option name must end with the gradient's last colour");
        }
    }

    @DisplayName("A click on an option's icon switches the option and shows the new state")
    @Test
    void clickOnIconSwitches(Env env) {
        Player player = connect(env);
        PlayerSettingsService service = service();
        menu(service, SettingsFixtures.registry(SettingsFixtures.TAG, SettingsFixtures.THROW), new SettingsMessages()).open(player);
        click(env, player, TAG_ICON);
        Assertions.assertTrue(service.isEnabled(player.getUuid(), SettingsFixtures.TAG), "a click on the icon must switch the option on");
        Assertions.assertEquals(Material.LIME_DYE, player.getOpenInventory().getItemStack(TAG_SWITCH).material(), "the switch after the click must show the new state");
    }

    @DisplayName("A click on an option's switch switches the option and shows the new state")
    @Test
    void clickOnSwitchSwitches(Env env) {
        Player player = connect(env);
        PlayerSettingsService service = service();
        menu(service, SettingsFixtures.registry(SettingsFixtures.TAG, SettingsFixtures.THROW), new SettingsMessages()).open(player);
        click(env, player, TAG_SWITCH);
        Assertions.assertTrue(service.isEnabled(player.getUuid(), SettingsFixtures.TAG), "a click on the switch must switch the option on");
        Assertions.assertTrue(hasGlint(player.getOpenInventory().getItemStack(TAG_ICON)), "the icon after the click must glint, because the option is on");
    }

    @DisplayName("A click plays the UI button click for the player who clicked")
    @Test
    void clickPlaysTheButtonSound(Env env) {
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        menu(service(), SettingsFixtures.registry(SettingsFixtures.TAG, SettingsFixtures.THROW), new SettingsMessages()).open(player);
        Collector<EntitySoundEffectPacket> heard = connection.trackIncoming(EntitySoundEffectPacket.class);
        click(env, player, TAG_SWITCH);
        heard.assertSingle(packet -> {
            Assertions.assertEquals(SoundEvent.UI_BUTTON_CLICK, packet.soundEvent(), "a switch must play the UI button click");
            Assertions.assertEquals(player.getEntityId(), packet.entityId(), "the click must be heard at the player");
        });
    }

    @DisplayName("A click switches the option and shows the menu of the new state")
    @Test
    void clickSwitchesAndShowsTheNewState(Env env) {
        Player player = connect(env);
        PlayerSettingsService service = service();
        SettingsMenu menu = menu(service, SettingsFixtures.registry(SettingsFixtures.TAG), new SettingsMessages());
        menu.open(player);
        menu.toggle(player, SettingsFixtures.TAG);
        Assertions.assertTrue(service.isEnabled(player.getUuid(), SettingsFixtures.TAG), "the click must switch the option on");
        Assertions.assertEquals(Material.LIME_DYE, player.getOpenInventory().getItemStack(SINGLE_SWITCH).material(), "the menu after the click must show the new state");
    }

    @DisplayName("A German player reads the option name in German")
    @Test
    void germanPlayerReadsGermanNames(Env env) {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "settings-test"));
        store.register(SettingsMessages.optionKey(SettingsFixtures.TAG.key(), "name"), Locale.GERMAN, "Fangen");
        store.register(SettingsMessages.optionKey(SettingsFixtures.TAG.key(), "name"), Locale.ENGLISH, "Tagging");
        GlobalTranslator.translator().addSource(store);
        try {
            Player player = connect(env);
            player.setLocale(Locale.GERMANY);
            menu(service(), SettingsFixtures.registry(SettingsFixtures.TAG), new SettingsMessages()).open(player);
            String name = PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().getItemStack(SINGLE_ICON).get(DataComponents.CUSTOM_NAME));
            Assertions.assertEquals("Fangen", name, "a German player must see the German option name");
        } finally {
            GlobalTranslator.translator().removeSource(store);
        }
    }

    private static void click(Env env, Player player, int slot) {
        env.process().eventHandler().call(new InventoryPreClickEvent(player.getOpenInventory(), player, new Click.Left(slot)));
    }

    private static SettingsMenu menu(PlayerSettings settings, SettingsRegistry registry, SettingsMessages messages) {
        return new SettingsMenu(settings, registry, messages);
    }

    private static SettingsMenu menu(SettingsRegistry registry, SettingsMessages messages) {
        return menu(service(), registry, messages);
    }

    private static boolean hasGlint(net.minestom.server.item.ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    private static TextColor firstColour(Component component) {
        List<TextColor> colours = coloursOfText(component);
        Assertions.assertFalse(colours.isEmpty(), "the text must carry a colour");
        return colours.getFirst();
    }

    private static TextColor lastColour(Component component) {
        List<TextColor> colours = coloursOfText(component);
        Assertions.assertFalse(colours.isEmpty(), "the text must carry a colour");
        return colours.getLast();
    }

    // The colour of every text leaf in order; a gradient colours each character separately.
    private static List<TextColor> coloursOfText(Component component) {
        List<TextColor> colours = new ArrayList<>();
        collectColours(component, null, colours);
        return colours;
    }

    private static void collectColours(Component component, TextColor inherited, List<TextColor> colours) {
        TextColor colour = component.color() != null ? component.color() : inherited;
        if (component instanceof TextComponent text && !text.content().isEmpty() && colour != null) {
            colours.add(colour);
        }
        for (Component child : component.children()) {
            collectColours(child, colour, colours);
        }
    }

    private static Player connect(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        return connection.connect(instance);
    }

    private static PlayerSettingsService service() {
        return new PlayerSettingsService(new WriteBehind(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC), Duration.ofSeconds(2), 3), new SettingsTelemetry(Telemetry.noop()));
    }
}

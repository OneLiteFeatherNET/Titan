package net.onelitefeather.titan.common.blockhandler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class SkullHandler implements BlockHandler {

    private static final @NotNull Key NAMESPACE_ID = Key.key("minecraft","skull");
    private static final List<Tag<?>> TAG_LIST = List.of(
            Tag.String("custom_name"),
            Tag.String("note_block_sound"),
            Tag.NBT("profile"));

    @Override
    public @NotNull Key getKey() {
        return NAMESPACE_ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAG_LIST;
    }
}

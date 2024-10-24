package net.onelitefeather.titan.blockhandler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class SkullHandler implements BlockHandler {

    private static final @NotNull NamespaceID NAMESPACE_ID = NamespaceID.from(Key.key("minecraft:skull"));
    private static final List<Tag<?>> TAG_LIST = List.of(Tag.String("ExtraType"),
            Tag.NBT("SkullOwner"));

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NAMESPACE_ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAG_LIST;
    }
}

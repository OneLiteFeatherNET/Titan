package net.onelitefeather.titan.common.blockhandler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class BedHandler implements BlockHandler {

    private static final @NotNull NamespaceID NAMESPACE_ID = NamespaceID.from(Key.key("minecraft:bed"));
    private static final List<Tag<?>> TAG_LIST = List.of(Tag.Boolean("IsPlaying"), Tag.String("RecordItem"), Tag.Integer("RecordStartTick"), Tag.Integer("TickCount"));

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NAMESPACE_ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAG_LIST;
    }
}

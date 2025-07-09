package net.onelitefeather.titan.common.blockhandler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class BeaconHandler implements BlockHandler {

    private static final Key NAMESPACE_ID = Key.key("minecraft:beacon");
    private static final List<Tag<?>> TAG_LIST = List.of(Tag.String("CustomName"),
            Tag.String("Lock"),
            Tag.Integer("Levels"),
            Tag.Integer("Primary"),
            Tag.Integer("Secondary"));

    @Override
    public @NotNull Key getKey() {
        return NAMESPACE_ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAG_LIST;
    }
}

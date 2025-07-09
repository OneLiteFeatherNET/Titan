package net.onelitefeather.titan.common.blockhandler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import net.onelitefeather.titan.common.blockhandler.objects.SignSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public final class SignHandler implements BlockHandler {

    private static final @NotNull Key NAMESPACE_ID = Key.key("minecraft:sign");
    private static final List<Tag<?>> TAG_LIST = List.of(Tag.Byte("is_waxed"),
            Tag.Structure("front_text", new SignTagSerializer()),
            Tag.Structure("back_text", new SignTagSerializer()));

    @Override
    public @NotNull Key getKey() {
        return NAMESPACE_ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAG_LIST;
    }

    private static final class SignTagSerializer implements TagSerializer<SignSide> {

        @Override
        public @Nullable SignSide read(@NotNull TagReadable reader) {
            return SignSide.of(
                    reader.getTag(Tag.Boolean("has_glowing_text")),
                    reader.getTag(Tag.String("color")),
                    reader.getTag(Tag.String("messages").list())
            );
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull SignSide value) {
            writer.setTag(Tag.Boolean("has_glowing_text"), value.glowing());
            writer.setTag(Tag.String("color"), value.color());
            writer.setTag(Tag.String("messages").list(), value.message());
        }
    }
}

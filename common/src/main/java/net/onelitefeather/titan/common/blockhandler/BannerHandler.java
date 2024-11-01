package net.onelitefeather.titan.common.blockhandler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.utils.NamespaceID;
import net.onelitefeather.titan.common.blockhandler.objects.BannerPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public final class BannerHandler implements BlockHandler {

    private static final NamespaceID NAMESPACE_ID = NamespaceID.from("minecraft:banner");
    private static final Tag<List<BannerPattern>> PATTERN_TAG = Tag.Structure("Patterns", new BannerTagSerializer()).list();
    private static final List<Tag<?>> TAG_LIST = List.of(Tag.String("CustomName"), PATTERN_TAG);

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NAMESPACE_ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAG_LIST;
    }

    private static class BannerTagSerializer implements TagSerializer<BannerPattern> {

        @Override
        public @Nullable BannerPattern read(@NotNull TagReadable reader) {
            return BannerPattern.from(reader.getTag(Tag.Integer("Color")), reader.getTag(Tag.String("Pattern")));
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull BannerPattern value) {
            writer.setTag(Tag.Integer("Color"), value.color());
            writer.setTag(Tag.String("Pattern"), value.pattern());
        }
    }
}

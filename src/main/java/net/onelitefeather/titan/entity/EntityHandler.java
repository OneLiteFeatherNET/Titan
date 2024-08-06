package net.onelitefeather.titan.entity;

import net.kyori.adventure.util.Codec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface EntityHandler<E extends Entity, DX extends EntityDecodeException , EX extends EntityEncodeException> {

    /**
     * Gets the id of this handler.
     * <p>
     * Used to write the block entity in the anvil world format.
     *
     * @return the namespace id of this handler
     */
    @NotNull NamespaceID getNamespaceId();

    Codec<E, NBTCompound, DX, EX> entityCodec();
    Codec<Point, NBTCompound, DX, EX> posCodec();

    @ApiStatus.Internal
    final class Dummy implements EntityHandler<Entity, EntityDecodeException, EntityEncodeException> {
        private static final Map<String, EntityHandler<Entity, EntityDecodeException, EntityEncodeException>> DUMMY_CACHE = new ConcurrentHashMap<>();

        public static @NotNull EntityHandler<Entity, EntityDecodeException, EntityEncodeException> get(@NotNull String namespace) {
            return DUMMY_CACHE.computeIfAbsent(namespace, Dummy::new);
        }

        private final NamespaceID namespace;

        private Dummy(String name) {
            namespace = NamespaceID.from(name);
        }

        @Override
        public @NotNull NamespaceID getNamespaceId() {
            return namespace;
        }

        @Override
        public Codec<Entity, org.jglrxavpok.hephaistos.nbt.NBTCompound, EntityDecodeException, EntityEncodeException> entityCodec() {
            return null;
        }

        @Override
        public Codec<Point, NBTCompound, EntityDecodeException, EntityEncodeException> posCodec() {
            return null;
        }
    }
}

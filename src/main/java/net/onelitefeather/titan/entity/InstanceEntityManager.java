package net.onelitefeather.titan.entity;

import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class InstanceEntityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceEntityManager.class);  //Microtus - update java keyword usage

    private static InstanceEntityManager INSTANCE;

    // Namespace -> handler supplier
    private final Map<String, Supplier<EntityHandler<?, ?, ?>>> entityHandlerMap = new ConcurrentHashMap<>();

    private final Set<String> dummyWarning = ConcurrentHashMap.newKeySet(); // Prevent warning spam

    private InstanceEntityManager() {
    }

    public void registerHandler(@NotNull String namespace, @NotNull Supplier<@NotNull EntityHandler<?, ? ,?>> handlerSupplier) {
        entityHandlerMap.put(namespace, handlerSupplier);
    }

    public void registerHandler(@NotNull NamespaceID namespace, @NotNull Supplier<@NotNull EntityHandler<?, ? ,?>> handlerSupplier) {
        registerHandler(namespace.toString(), handlerSupplier);
    }

    public @Nullable EntityHandler<?, ? ,?> getHandler(@NotNull String namespace) {
        final var handler = entityHandlerMap.get(namespace);
        return handler != null ? handler.get() : null;
    }

    @ApiStatus.Internal
    public @NotNull EntityHandler<?, ? ,?> getHandlerOrDummy(@NotNull String namespace) {
        EntityHandler<?, ? ,?> handler = getHandler(namespace);
        if (handler == null) {
            if (dummyWarning.add(namespace)) {
                LOGGER.warn("""
                        Entity {} does not have any corresponding handler, default to dummy.
                        You may want to register a handler for this namespace to prevent any data loss.""", namespace);
            }
            handler = EntityHandler.Dummy.get(namespace);
        }
        return handler;
    }


    public synchronized static InstanceEntityManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new InstanceEntityManager();
        }
        return INSTANCE;
    }
}

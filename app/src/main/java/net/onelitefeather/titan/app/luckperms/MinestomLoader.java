package net.onelitefeather.titan.app.luckperms;


import me.lucko.luckperms.minestom.LPMinestomBootstrap;
import me.lucko.luckperms.minestom.app.LuckPermsApplication;

public final class MinestomLoader {
    private static final LuckPermsApplication APPLICATION = new LuckPermsApplication();
    private static MinestomLoader instance;
    private final LPMinestomBootstrap plugin;

    public MinestomLoader() {
        this.plugin = new LPMinestomBootstrap(APPLICATION);
    }

    public MinestomLoader registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this.plugin::onDisable, "luckperms-shutdown-hook"));
        return this;
    }

    public MinestomLoader load() {
        this.plugin.onLoad();
        return this;
    }

    public void start() {
        this.plugin.onEnable();
    }

    public static MinestomLoader get() {
        MinestomLoader localInstance = instance;
        if (localInstance != null) {
            return localInstance;
        }
        synchronized (MinestomLoader.class) {
            if (instance == null) {
                instance = new MinestomLoader();
            }
            return instance;
        }
    }
}

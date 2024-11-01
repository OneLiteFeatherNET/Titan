package net.onelitefeather.titan.common.map;

import net.minestom.server.coordinate.Pos;

final class LobbyMapBuilder implements LobbyMap.Builder {

    private Pos spawn;
    private String name;
    private String[] author;

    @Override
    public LobbyMap.Builder spawn(Pos spawn) {
        this.spawn = spawn;
        return this;
    }

    @Override
    public LobbyMap.Builder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public LobbyMap.Builder author(String... author) {
        this.author = author;
        return this;
    }

    @Override
    public LobbyMap build() {
        LobbyMap lobbyMap = new LobbyMap();
        if (spawn != null) {
            lobbyMap.setSpawn(spawn);
        }
        lobbyMap.setSpawn(spawn);
        if (name != null) {
            lobbyMap.setName(name);;
        }
        if (author != null) {
            lobbyMap.setBuilders(author);
        }
        return lobbyMap;
    }
}

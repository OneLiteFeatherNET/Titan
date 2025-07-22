/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

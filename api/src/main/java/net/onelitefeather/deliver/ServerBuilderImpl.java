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
package net.onelitefeather.deliver;

import java.util.UUID;

final class ServerBuilderImpl implements DeliverComponent.ServerBuilder {

	private String serverName;
	private UUID playerId;

	@Override
	public DeliverComponent.ServerBuilder serverName(String serverName) {
		this.serverName = serverName;
		return this;
	}

	@Override
	public DeliverComponent.ServerBuilder playerId(UUID playerId) {
		this.playerId = playerId;
		return this;
	}

	@Override
	public DeliverComponent build() {
		if (serverName == null) {
			throw new IllegalArgumentException("Server cannot be null");
		}
		if (playerId == null) {
			throw new IllegalArgumentException("playerId cannot be null");
		}
		return new ServerDeliverComponentImpl(DeliverType.SERVER, serverName, playerId);
	}
}

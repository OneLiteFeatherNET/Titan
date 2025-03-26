package net.onelitefeather.deliver;

import java.io.Serializable;
import java.util.UUID;

record TaskComponentImpl(DeliverType type, String taskName, UUID playerId) implements DeliverComponent, DeliverComponent.TaskComponent, Serializable {
}

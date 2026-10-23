/**
 * The {@code npc} column: join NPCs that send a player into a CloudNet task on a click, and
 * ambience zones that make an area of the lobby feel alive. Both are map data of the lobby world
 * (see {@code docs/halloween-slender-showcase.md} for an example). Only vanilla client content is
 * used: no resource pack, no custom sound events, models or fonts.
 */
@InjectModule(name = "npcColumn", requires = {EventNode.class, Deliver.class, PermissionService.class, LobbyNpcs.class, LobbyAmbiences.class, Instance.class, Scheduler.class, Telemetry.class, Clock.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
package net.onelitefeather.titan.feature.npc;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.ambience.LobbyAmbiences;
import net.onelitefeather.titan.core.npc.LobbyNpcs;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Portals: configured regions of the lobby that hand a player to another server instead of
 * asking them to open the navigator.
 *
 * <p>The pieces are deliberately separate.
 * {@link net.onelitefeather.titan.common.portal.PortalDefinition}
 * is what an operator writes; {@link net.onelitefeather.titan.common.portal.Portal} is what
 * survived validation; {@link net.onelitefeather.titan.common.portal.PortalIndex} answers "which
 * portal is this position in" cheaply enough for every movement packet; and
 * {@link net.onelitefeather.titan.common.portal.PortalService} owns the decision - gate, then
 * reachability, then delivery.
 *
 * <p>Geometry is not written here. Regions are Coris shapes
 * ({@link net.onelitefeather.coris.shape.CuboidShape}), which is the org's shape library
 * (OLF-L2-04).
 */
@NotNullByDefault
package net.onelitefeather.titan.common.portal;

import org.jetbrains.annotations.NotNullByDefault;

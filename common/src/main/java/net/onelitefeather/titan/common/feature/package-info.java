/**
 * Staged feature delivery: one gate that decides whether a player sees a feature, and the time
 * window that gate honours.
 *
 * <p>This package is the only place in Titan that talks to Togglz. Everything else asks
 * {@link net.onelitefeather.titan.common.feature.FeatureGate} and never touches a
 * {@code FeatureManager} itself.
 */
@NotNullByDefault
package net.onelitefeather.titan.common.feature;

import org.jetbrains.annotations.NotNullByDefault;

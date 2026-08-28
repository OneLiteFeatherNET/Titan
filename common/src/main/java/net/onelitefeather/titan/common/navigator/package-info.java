/**
 * The navigator menu and the build servers it may offer (spec stage 5).
 *
 * <p>The package holds three things that belong together:
 *
 * <ul>
 * <li>{@link net.onelitefeather.titan.common.navigator.NavigatorEntry} and
 * {@link net.onelitefeather.titan.common.navigator.NavigatorLayout} — what a player sees and
 * where it sits. The layout is computed from the entries a player may see and from nothing
 * else, which is how a hidden entry leaves no gap behind (US-5.02, NFR-005).</li>
 * <li>{@link net.onelitefeather.titan.common.navigator.BuildServerDirectory} and
 * {@link net.onelitefeather.titan.common.navigator.TitanBuildServerDirectory} — the list of
 * reachable build servers, filled in by the CloudNet bridge extension across the classloader
 * boundary with JDK types only (US-5.04).</li>
 * <li>{@link net.onelitefeather.titan.common.navigator.GuardedDeliver} — the second permission
 * check, made when a switch is requested rather than when the menu is drawn (US-5.03).</li>
 * </ul>
 */
@NotNullByDefault
package net.onelitefeather.titan.common.navigator;

import org.jetbrains.annotations.NotNullByDefault;

/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.utils;

import java.util.function.Supplier;

/**
 * Runs a {@link java.util.ServiceLoader}-backed lookup with the context classloader temporarily
 * pointed at the classloader of the caller, so an SPI shipped by this jar is found even when the
 * calling thread carries an unrelated context classloader.
 *
 * <p>This type stays in {@code common/utils} on purpose. It is not Titan code that lost its home
 * (OLF-L3-02): it is the fourth byte-identical copy of the same helper in the OneLiteFeather
 * estate (Titan, Butterfly Minestom, Butterfly Bukkit, ManisGame) and its destination is
 * Butterfly, not another Titan package (OLF-L2-04, open point 4 of the OLF standard). Moving it
 * inside Titan first would only make the eventual deletion harder to spot.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ThreadHelper {
    default void syncThreadForServiceLoader(Runnable runnable) {
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        ClassLoader pluginClassLoader = this.getClass().getClassLoader();
        try {
            currentThread.setContextClassLoader(pluginClassLoader);
            runnable.run();
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }

    default <T> T syncThreadForServiceLoader(Supplier<T> supplier) {
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        ClassLoader pluginClassLoader = this.getClass().getClassLoader();
        try {
            currentThread.setContextClassLoader(pluginClassLoader);
            return supplier.get();
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }
}

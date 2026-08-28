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
package net.onelitefeather.titan.common.season;

import org.jetbrains.annotations.Nullable;

/**
 * Thrown when a season file cannot be read into a {@link SeasonDefinition}.
 *
 * <p>This is the type that makes US-4.04 a load-time failure rather than a runtime surprise. Every
 * message names the file and the value that could not be used, because the alternative — a season
 * that loads with one effect silently missing — is the failure mode the requirement exists to
 * rule out.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class SeasonConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final @Nullable String source;

    /**
     * Creates an exception describing a problem in one season file.
     *
     * @param source  the file or season the problem was found in, {@code null} when unknown
     * @param message what could not be read, naming the offending value
     */
    public SeasonConfigurationException(@Nullable String source, String message) {
        super(source == null ? message : source + ": " + message);
        this.source = source;
    }

    /**
     * Creates an exception describing a problem in one season file, caused by another failure.
     *
     * @param source  the file or season the problem was found in, {@code null} when unknown
     * @param message what could not be read, naming the offending value
     * @param cause   the underlying failure
     */
    public SeasonConfigurationException(@Nullable String source, String message, Throwable cause) {
        super(source == null ? message : source + ": " + message, cause);
        this.source = source;
    }

    /**
     * Returns the file or season the problem was found in.
     *
     * @return the source, or {@code null} when the problem could not be attributed to one
     */
    public @Nullable String source() {
        return this.source;
    }
}

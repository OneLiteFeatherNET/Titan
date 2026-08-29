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
package net.onelitefeather.titan.common.time;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Thrown when {@value TimeConfig#TIME_FILE_NAME} cannot be turned into a pair of strategies.
 *
 * <p>It is deliberately unchecked and deliberately not caught anywhere: a lobby that starts on a
 * strategy nobody chose is worse than a lobby that does not start. The message always carries the
 * value that was rejected and the values that would have been accepted, because the only person
 * who ever reads it is looking at their own typo.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class TimeConfigException extends RuntimeException {

    private TimeConfigException(String message) {
        super(message);
    }

    private TimeConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Reports a value that is present but not one this lobby knows.
     *
     * @param key      the configuration key the value was written under
     * @param value    the value as written in the file
     * @param accepted the values that would have been accepted
     * @return the exception to throw
     */
    @Contract(pure = true, value = "_, _, _ -> new")
    @NotNull
    static TimeConfigException unknownValue(@NotNull String key, @Nullable String value, @NotNull List<String> accepted) {
        return new TimeConfigException("Unknown " + key + " \"" + value + "\" in " + TimeConfig.TIME_FILE_NAME + "; valid values are: " + String.join(", ", accepted));
    }

    /**
     * Reports a {@value TimeConfig#FIXED} season strategy that never says which season.
     *
     * @param accepted the season identifiers that would have been accepted
     * @return the exception to throw
     */
    @Contract(pure = true, value = "_ -> new")
    @NotNull
    static TimeConfigException missingFixedSeason(@NotNull List<String> accepted) {
        return new TimeConfigException("seasonStrategy \"" + TimeConfig.FIXED + "\" in " + TimeConfig.TIME_FILE_NAME + " needs a fixedSeason; valid values are: " + String.join(", ", accepted));
    }

    /**
     * Reports a file that is not readable as JSON at all.
     *
     * @param path  the file that could not be read
     * @param cause what the parser complained about
     * @return the exception to throw
     */
    @Contract(pure = true, value = "_, _ -> new")
    @NotNull
    static TimeConfigException unreadable(@NotNull Object path, @NotNull Throwable cause) {
        return new TimeConfigException("Unable to read " + path + "; fix the file or delete it to fall back to the defaults", cause);
    }
}

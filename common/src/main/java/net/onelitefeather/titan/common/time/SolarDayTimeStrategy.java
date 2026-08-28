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
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Lays the real sunrise and sunset of a geographic position onto the Minecraft day, so that the
 * lobby gets light late in December and stays bright until well past nine in June (US-2.07).
 *
 * <h2>The mapping</h2>
 *
 * Real sunrise becomes tick {@code 0}, real sunset becomes tick {@value DayTimeStrategy#DUSK_TICK},
 * and the night in between two days is stretched over the remaining half of the tick range. Both
 * halves are linear in themselves, so the mapping is continuous in real time and strictly
 * increasing — unlike {@link LinearDayTimeStrategy} it does not follow the wall clock and therefore
 * neither skips nor repeats an hour when daylight saving changes.
 *
 * <p>Minecraft renders its own sunrise across ticks 23000&#8230;0 and its sunset across
 * 12000&#8230;13000. Anchoring the real events at 0 and 12000 puts the visible transition within
 * roughly 1000 ticks — about one real hour — of the astronomical event. Anchoring them in the
 * middle
 * of the rendered transition instead would be just as defensible; this class picks the segment
 * boundaries because they keep day and night exactly half the tick range each.
 *
 * <h2>The calculation and its limits</h2>
 *
 * Sunrise and sunset come from the low-precision sunrise equation in its published closed form (the
 * form the NOAA solar calculator is derived from). Checked against the Berlin values for the 2026
 * solstices and equinoxes it agrees to within about one minute; the equation's own stated bound is
 * a
 * few minutes. One Minecraft tick is worth 3.6 real seconds in a 24-hour cycle, so the error is
 * visible in principle — this is a lighting effect, not an ephemeris.
 *
 * <p>Known limits, stated rather than hidden:
 *
 * <ul>
 * <li>The horizon is the conventional -0.833°, which folds in mean refraction and the solar
 * radius. Local terrain, air pressure and temperature are not modelled.
 * <li>The equation returns UT without a &#916;T correction. At roughly 70 seconds that stays well
 * inside the equation's own error.
 * <li>Above the polar circles there are dates without a sunrise or a sunset. For those this class
 * falls back to {@link LinearDayTimeStrategy} instead of inventing a boundary. Berlin never
 * reaches that case; a future position might.
 * </ul>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class SolarDayTimeStrategy implements DayTimeStrategy {

    /** Latitude of Berlin in degrees, north positive. */
    public static final double BERLIN_LATITUDE = 52.520008;

    /** Longitude of Berlin in degrees, east positive. */
    public static final double BERLIN_LONGITUDE = 13.404954;

    private static final SolarDayTimeStrategy BERLIN = new SolarDayTimeStrategy(BERLIN_LATITUDE, BERLIN_LONGITUDE);

    /** Epoch day of 2000-01-01, the date J2000.0 falls on. */
    private static final double J2000_EPOCH_DAY = 10_957.0;

    /** Julian date of the Unix epoch. */
    private static final double UNIX_EPOCH_JULIAN_DATE = 2_440_587.5;

    /** Julian date of J2000.0. */
    private static final double J2000_JULIAN_DATE = 2_451_545.0;

    /** Leap second and clock correction of the sunrise equation, in days. */
    private static final double MEAN_SOLAR_TIME_CORRECTION = 0.0009;

    /** Obliquity of the ecliptic in degrees. */
    private static final double EARTH_OBLIQUITY = 23.4397;

    /** Argument of perihelion of the Earth in degrees. */
    private static final double PERIHELION_ARGUMENT = 102.9372;

    /**
     * Altitude of the centre of the solar disc when its upper limb touches the horizon, degrees.
     */
    private static final double HORIZON_ALTITUDE = -0.833;

    private static final double MILLIS_PER_DAY = 86_400_000.0;

    private final double latitude;
    private final double longitude;

    private SolarDayTimeStrategy(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Returns the strategy for Berlin, the position the lobby is written for.
     *
     * @return the shared solar strategy for Berlin
     */
    @Contract(pure = true)
    public static SolarDayTimeStrategy berlin() {
        return BERLIN;
    }

    /**
     * Returns a strategy for an arbitrary position on Earth.
     *
     * @param latitude  the latitude in degrees, north positive, within {@code [-90, 90]}
     * @param longitude the longitude in degrees, east positive, within {@code [-180, 180]}
     * @return a solar strategy for that position
     * @throws IllegalArgumentException if either coordinate is outside its range
     */
    @Contract(pure = true, value = "_, _ -> new")
    public static SolarDayTimeStrategy at(double latitude, double longitude) {
        if (!(latitude >= -90.0 && latitude <= 90.0)) {
            throw new IllegalArgumentException("latitude out of range: " + latitude);
        }
        if (!(longitude >= -180.0 && longitude <= 180.0)) {
            throw new IllegalArgumentException("longitude out of range: " + longitude);
        }
        return new SolarDayTimeStrategy(latitude, longitude);
    }

    @Override
    @Contract(pure = true)
    public long ticksAt(Instant instant, ZoneId zone) {
        LocalDate date = instant.atZone(zone).toLocalDate();
        SolarDay yesterday = solarDay(date.minusDays(1));
        SolarDay today = solarDay(date);
        SolarDay tomorrow = solarDay(date.plusDays(1));
        if (yesterday == null || today == null || tomorrow == null) {
            // No sunrise or no sunset in the surrounding days; see the class javadoc.
            return LinearDayTimeStrategy.instance().ticksAt(instant, zone);
        }

        // Alternating boundaries: sunrise, sunset, sunrise, ... An instant that falls into an
        // even-indexed gap is between a sunrise and a sunset and therefore belongs to the day half.
        Instant[] boundaries = {yesterday.sunrise(), yesterday.sunset(), today.sunrise(), today.sunset(), tomorrow.sunrise(), tomorrow.sunset(),
        };
        for (int i = 0; i < boundaries.length - 1; i++) {
            if (instant.isBefore(boundaries[i]) || !instant.isBefore(boundaries[i + 1])) {
                continue;
            }
            return i % 2 == 0 ? dayTicks(boundaries[i], boundaries[i + 1], instant) : nightTicks(boundaries[i], boundaries[i + 1], instant);
        }
        // The instant lies outside the three computed days, which only a zone that disagrees wildly
        // with the position can produce. Falling back keeps the result defined.
        return LinearDayTimeStrategy.instance().ticksAt(instant, zone);
    }

    /**
     * Returns the sunrise of the given date at this position.
     *
     * <p>Exposed because a mapping that cannot be held against a published sunrise table is a
     * mapping nobody can check.
     *
     * @param date the date to compute for
     * @return the instant of sunrise, or {@code null} if the sun neither rises nor sets that date
     */
    @Contract(pure = true)
    public @Nullable Instant sunrise(LocalDate date) {
        SolarDay day = solarDay(date);
        return day == null ? null : day.sunrise();
    }

    /**
     * Returns the sunset of the given date at this position.
     *
     * @param date the date to compute for
     * @return the instant of sunset, or {@code null} if the sun neither rises nor sets that date
     */
    @Contract(pure = true)
    public @Nullable Instant sunset(LocalDate date) {
        SolarDay day = solarDay(date);
        return day == null ? null : day.sunset();
    }

    private static long dayTicks(Instant sunrise, Instant sunset, Instant instant) {
        double progress = fraction(sunrise, sunset, instant);
        return clamp((long) (progress * DUSK_TICK), 0L, DUSK_TICK - 1L);
    }

    private static long nightTicks(Instant sunset, Instant nextSunrise, Instant instant) {
        double progress = fraction(sunset, nextSunrise, instant);
        long night = TICKS_PER_DAY - (long) DUSK_TICK;
        return clamp(DUSK_TICK + (long) (progress * night), DUSK_TICK, TICKS_PER_DAY - 1L);
    }

    private static double fraction(Instant from, Instant to, Instant instant) {
        double span = to.toEpochMilli() - (double) from.toEpochMilli();
        if (span <= 0.0) {
            return 0.0;
        }
        return (instant.toEpochMilli() - (double) from.toEpochMilli()) / span;
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Solves the sunrise equation for one calendar date at this position.
     *
     * @param date the date, taken as the local date at this position's longitude
     * @return the two solar events of that date, or {@code null} if the sun neither rises nor sets
     */
    private @Nullable SolarDay solarDay(LocalDate date) {
        double days = date.toEpochDay() - J2000_EPOCH_DAY;
        // Mean solar time at this longitude, in days since J2000.0.
        double meanSolarTime = days + MEAN_SOLAR_TIME_CORRECTION + (-this.longitude) / 360.0;
        double meanAnomalyDegrees = (357.5291 + 0.98560028 * meanSolarTime) % 360.0;
        double meanAnomaly = Math.toRadians(meanAnomalyDegrees);
        double equationOfCentre = 1.9148 * Math.sin(meanAnomaly) + 0.0200 * Math.sin(2.0 * meanAnomaly) + 0.0003 * Math.sin(3.0 * meanAnomaly);
        double eclipticLongitude = Math.toRadians(
                (meanAnomalyDegrees + equationOfCentre + PERIHELION_ARGUMENT + 180.0) % 360.0);
        double transit = J2000_JULIAN_DATE + meanSolarTime + 0.0053 * Math.sin(meanAnomaly) - 0.0069 * Math.sin(2.0 * eclipticLongitude);

        double sinDeclination = Math.sin(eclipticLongitude) * Math.sin(Math.toRadians(EARTH_OBLIQUITY));
        double cosDeclination = Math.cos(Math.asin(sinDeclination));
        double latitudeRadians = Math.toRadians(this.latitude);
        double cosHourAngle = (Math.sin(Math.toRadians(HORIZON_ALTITUDE)) - Math.sin(latitudeRadians) * sinDeclination) / (Math.cos(latitudeRadians) * cosDeclination);
        if (Double.isNaN(cosHourAngle) || cosHourAngle > 1.0 || cosHourAngle < -1.0) {
            return null;
        }
        double hourAngle = Math.toDegrees(Math.acos(cosHourAngle));
        return new SolarDay(
                fromJulianDate(transit - hourAngle / 360.0), fromJulianDate(transit + hourAngle / 360.0));
    }

    private static Instant fromJulianDate(double julianDate) {
        return Instant.ofEpochMilli(Math.round((julianDate - UNIX_EPOCH_JULIAN_DATE) * MILLIS_PER_DAY));
    }

    @Override
    public String toString() {
        return "SolarDayTimeStrategy[latitude=" + this.latitude + ", longitude=" + this.longitude + ']';
    }

    /**
     * The two solar events of one calendar date at one position.
     *
     * @param sunrise the moment the upper limb of the sun appears
     * @param sunset  the moment it disappears again
     */
    private record SolarDay(Instant sunrise, Instant sunset) {
    }
}

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
package net.onelitefeather.titan.common.time.season;

import org.jetbrains.annotations.Contract;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Puts the season boundaries on the equinoxes and solstices (US-2.12).
 *
 * <p>A season starts on the calendar day its event falls on, read in the zone this strategy was
 * built for: spring on the March equinox, summer on the June solstice, autumn on the September
 * equinox, winter on the December solstice. The events move by up to a day and a half between
 * years,
 * which is exactly why they cannot be hard-coded as 20 March and 21 June.
 *
 * <h2>The calculation and its limits</h2>
 *
 * The instants come from Meeus, <em>Astronomical Algorithms</em>, chapter 27: a polynomial for the
 * mean event plus the published table of 24 periodic terms. Checked against the 2025 and 2026
 * events
 * the result is within about twenty seconds. &#916;T is subtracted with the Espenak/Meeus
 * polynomial
 * for 2005&#8230;2050, which turns the dynamical time the algorithm produces into UT.
 *
 * <p>Known limits, stated rather than hidden:
 *
 * <ul>
 * <li>The polynomials are published for the years 1000 to 3000. Outside that range the result is
 * an extrapolation and this class does not pretend otherwise.
 * <li>The &#916;T polynomial is fitted to 2005&#8230;2050. Past 2050 the correction degrades — it
 * is a correction of about a minute on an event whose calendar day is what matters, so the
 * season boundary survives long after the seconds stop being right.
 * <li>An event falling within a minute of local midnight can land on either side of the day
 * boundary. No season boundary the lobby cares about is that sharp; a seasonal package is
 * switched by its configured window, not by this class.
 * </ul>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class AstronomicalSeasonStrategy implements SeasonBoundaryStrategy {

    /**
     * The periodic terms of Meeus chapter 27, as {@code {A, B, C}} triples.
     *
     * <p>{@code A} is an amplitude in units of 10<sup>-5</sup> days, {@code B} a phase in degrees
     * and
     * {@code C} an angular speed in degrees per Julian century.
     */
    private static final double[][] PERIODIC_TERMS = {{485, 324.96, 1934.136}, {203, 337.23, 32964.467}, {199, 342.08, 20.186}, {182, 27.85, 445267.112}, {156, 73.14, 45036.886}, {136, 171.52, 22518.443}, {77, 222.54, 65928.934}, {74, 296.72, 3034.906}, {70, 243.58, 9037.513}, {58, 119.81, 33718.147}, {52, 297.17, 150.678}, {50, 21.02, 2281.226}, {45, 247.54, 29929.562}, {44, 325.15, 31555.956}, {29, 60.93, 4443.417}, {18, 155.12, 67555.328}, {17, 288.79, 4562.452}, {16, 198.04, 62894.029}, {14, 199.76, 31436.921}, {12, 95.39, 14577.848}, {12, 287.11, 31931.756}, {12, 320.81, 34777.259}, {9, 227.73, 1222.114}, {8, 15.45, 16859.074},
    };

    /** Julian date of the Unix epoch. */
    private static final double UNIX_EPOCH_JULIAN_DATE = 2_440_587.5;

    /** Julian date of J2000.0. */
    private static final double J2000_JULIAN_DATE = 2_451_545.0;

    private static final double DAYS_PER_JULIAN_CENTURY = 36_525.0;

    private static final double MILLIS_PER_DAY = 86_400_000.0;

    private static final double SECONDS_PER_DAY = 86_400.0;

    private final ZoneId zone;

    private AstronomicalSeasonStrategy(ZoneId zone) {
        this.zone = zone;
    }

    /**
     * Returns the strategy that resolves the event instants in the given zone.
     *
     * @param zone the zone the calendar day of an event is read in
     * @return an astronomical strategy for that zone
     */
    @Contract(pure = true, value = "_ -> new")
    public static AstronomicalSeasonStrategy of(ZoneId zone) {
        return new AstronomicalSeasonStrategy(zone);
    }

    @Override
    @Contract(pure = true)
    public Season seasonAt(LocalDate date) {
        int year = date.getYear();
        if (date.isBefore(eventDate(year, Event.MARCH_EQUINOX))) {
            // Still in the winter that started in the previous December.
            return Season.WINTER;
        }
        if (date.isBefore(eventDate(year, Event.JUNE_SOLSTICE))) {
            return Season.SPRING;
        }
        if (date.isBefore(eventDate(year, Event.SEPTEMBER_EQUINOX))) {
            return Season.SUMMER;
        }
        if (date.isBefore(eventDate(year, Event.DECEMBER_SOLSTICE))) {
            return Season.AUTUMN;
        }
        return Season.WINTER;
    }

    /**
     * Returns the calendar day, in this strategy's zone, that the given event of the given year
     * falls
     * on.
     *
     * @param year  the calendar year
     * @param event the event to locate
     * @return the local date of the event
     */
    @Contract(pure = true)
    public LocalDate eventDate(int year, Event event) {
        return eventInstant(year, event).atZone(this.zone).toLocalDate();
    }

    /**
     * Returns the instant of the given event of the given year.
     *
     * <p>Exposed because an implementation that cannot be held against a published equinox table is
     * one nobody can check.
     *
     * @param year  the calendar year
     * @param event the event to locate
     * @return the instant of the event in UT
     */
    @Contract(pure = true)
    public static Instant eventInstant(int year, Event event) {
        double meanJulianDate = event.meanJulianDate(year);
        double centuries = (meanJulianDate - J2000_JULIAN_DATE) / DAYS_PER_JULIAN_CENTURY;
        double w = Math.toRadians(35999.373 * centuries - 2.47);
        double lambdaCorrection = 1.0 + 0.0334 * Math.cos(w) + 0.0007 * Math.cos(2.0 * w);
        double periodic = 0.0;
        for (double[] term : PERIODIC_TERMS) {
            periodic += term[0] * Math.cos(Math.toRadians(term[1] + term[2] * centuries));
        }
        // Dynamical time; shift to UT so that the calendar day is the one a clock in the zone shows.
        double dynamical = meanJulianDate + (0.00001 * periodic) / lambdaCorrection;
        double universal = dynamical - deltaTSeconds(year) / SECONDS_PER_DAY;
        return Instant.ofEpochMilli(Math.round((universal - UNIX_EPOCH_JULIAN_DATE) * MILLIS_PER_DAY));
    }

    /**
     * Espenak and Meeus' polynomial for the difference between dynamical time and UT, fitted to
     * 2005&#8230;2050.
     *
     * @param year the calendar year
     * @return &#916;T in seconds
     */
    private static double deltaTSeconds(int year) {
        double t = year - 2000.0;
        return 62.92 + 0.32217 * t + 0.005589 * t * t;
    }

    /**
     * Returns the zone the event instants are resolved in.
     *
     * @return the zone
     */
    @Contract(pure = true)
    public ZoneId zone() {
        return this.zone;
    }

    @Override
    public String toString() {
        return "AstronomicalSeasonStrategy[zone=" + this.zone + ']';
    }

    /**
     * The four astronomical events that open a season.
     *
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    public enum Event {

        /** The moment spring begins, around 20 March. */
        MARCH_EQUINOX(2451623.80984, 365242.37404, 0.05169, -0.00411, -0.00057),

        /** The moment summer begins, around 21 June. */
        JUNE_SOLSTICE(2451716.56767, 365241.62603, 0.00325, 0.00888, -0.00030),

        /** The moment autumn begins, around 22 September. */
        SEPTEMBER_EQUINOX(2451810.21715, 365242.01767, -0.11575, 0.00337, 0.00078),

        /** The moment winter begins, around 21 December. */
        DECEMBER_SOLSTICE(2451900.05952, 365242.74049, -0.06223, -0.00823, 0.00032);

        private final double a0;
        private final double a1;
        private final double a2;
        private final double a3;
        private final double a4;

        Event(double a0, double a1, double a2, double a3, double a4) {
            this.a0 = a0;
            this.a1 = a1;
            this.a2 = a2;
            this.a3 = a3;
            this.a4 = a4;
        }

        /**
         * Returns the mean Julian date of this event, before the periodic terms are applied.
         *
         * @param year the calendar year, meant for 1000&#8230;3000
         * @return the mean Julian ephemeris date
         */
        @Contract(pure = true)
        double meanJulianDate(int year) {
            double y = (year - 2000) / 1000.0;
            return this.a0 + this.a1 * y + this.a2 * y * y + this.a3 * y * y * y + this.a4 * y * y * y * y;
        }
    }
}

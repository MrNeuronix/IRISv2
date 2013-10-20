package fi.ceci.util;

import fi.ceci.model.RecordType;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility for converting values to for display.
 */
public final class DisplayValueConversionUtil {
    /**
     * The decimal format.
     */
    private static DecimalFormat decimalFormat = new DecimalFormat("#.##");;

    /**
     * Private constructor to disable construction.
     */
    private DisplayValueConversionUtil() {
    }

    /**
     * Converts UTC time to local time.
     * @param utcTime the time in UTC
     * @return the time in local time zone time
     */
    public static Date convertTime(final Date utcTime) {
        final TimeZone tz = TimeZone.getTimeZone("Europe/Helsinki");
        Date ret = new Date(utcTime.getTime() + tz.getRawOffset());

        // if we are now in DST, back off by the delta.  Note that we are checking the GMT date, this is the KEY.
        if (tz.inDaylightTime(ret)) {
            Date dstDate = new Date(ret.getTime() + tz.getDSTSavings());

            // check to make sure we have not crossed back into standard time
            // this happens when we are on the cusp of DST (7pm the day before the change for PDT)
            if (tz.inDaylightTime(dstDate))
            {
                ret = dstDate;
            }
        }

        return ret;
    }

    /**
     * Convert unit to display unit.
     * @param recordType the record type
     * @param recordUnit the record unit
     * @return the display unit
     */
    public static String getDisplayUnit(final RecordType recordType, final String recordUnit) {
        if (recordType == RecordType.TEMPERATURE) {
            return PropertiesUtil.getProperty("ago-control-vaadin-site", "display-temperature-unit");
        }
        return recordUnit;
    }

    /**
     * Convert record value to display value.
     * @param recordType the record type
     * @param recordUnit the record unit
     * @param displayUnit the display unit
     * @param recordValue the record value
     * @return the display value
     */
    public static double convertValue(final RecordType recordType, final String recordUnit,
                                      final String displayUnit, final double recordValue) {

        if (recordUnit.equals("F") && displayUnit.equals("C")) {
            return (5.0 / 9.0) * (recordValue-32);
        }
        return recordValue;
    }

    /**
     * Format double to display value.
     *
     * @param value the value
     * @return the value as string
     */
    public static String formatDouble(final double value) {
        synchronized (decimalFormat) {
            return decimalFormat.format(value);
        }
    }

}


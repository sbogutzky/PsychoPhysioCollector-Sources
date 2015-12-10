package de.bogutzky.psychophysiocollector.app;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

/**
 * This class offers a facility method to convert time from format used by BioHarness to Unix Epoch.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class TimeConverter {

    public static long timeToEpoch(int year, byte month, byte day, long millisOfDay) {

        int hours, minutes, seconds, milliseconds;
        long epoch;

        hours = (int) TimeUnit.MILLISECONDS.toHours(millisOfDay);
        minutes = (int) (TimeUnit.MILLISECONDS.toMinutes(millisOfDay) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisOfDay)));
        seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(millisOfDay) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisOfDay)));
        milliseconds = (int) millisOfDay % 1000;

        epoch = new GregorianCalendar(year, ((int) month) -1, (int) day, hours, minutes, seconds).getTimeInMillis();
        epoch += milliseconds;

        return epoch;
    }
}
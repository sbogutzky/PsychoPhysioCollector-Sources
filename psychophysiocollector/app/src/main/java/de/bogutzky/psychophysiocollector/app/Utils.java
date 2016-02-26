package de.bogutzky.psychophysiocollector.app;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by Jan Schrader on 23.02.2016.
 */
public class Utils {

    public static JSONArray shuffleJsonArray(JSONArray array) throws JSONException {
        Random rnd = new Random();
        rnd.setSeed(System.currentTimeMillis());
        for (int i = array.length() - 1; i >= 0; i--) {
            int j = rnd.nextInt(i + 1);
            Object object = array.get(j);
            array.put(j, array.get(i));
            array.put(i, object);
        }
        return array;
    }

    public static String getDateString(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}

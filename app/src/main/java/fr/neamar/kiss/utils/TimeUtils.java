package fr.neamar.kiss.utils;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fr.neamar.kiss.R;

public class TimeUtils {

    public static String formatTimestamp(Context context, long timestamp) {
        // Get current time
        Calendar now = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        // Calculate the difference in milliseconds
        long diffMillis = now.getTimeInMillis() - timestamp;
        long minutesDiff = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hoursDiff = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long daysDiff = TimeUnit.MILLISECONDS.toDays(diffMillis);

        // Get today, yesterday, and specific time formats
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("d. MMM 'at' HH:mm", Locale.getDefault());

        // Check for the same minute (just now)
        if (minutesDiff < 1) {
            return context.getString(R.string.just_now); // Assuming there's a "just now" string resource
        }

        // Check for differences within the same hour
        if (minutesDiff < 60) {
            return String.format(context.getString(R.string.minutes_ago), minutesDiff);
        }

        // Check for differences within the same day
        if (hoursDiff < 24 && isSameDay(now, calendar)) {
            return timeFormat.format(new Date(timestamp)); // Only return HH:MM without "Today"
        }

        // Check for yesterday
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(now, calendar)) {
            return String.format(context.getString(R.string.yesterday_at), timeFormat.format(new Date(timestamp)));
        }

        // For older days up to a week ago
        if (daysDiff <= 7) {
            return String.format(context.getString(R.string.days_ago_at), daysDiff, timeFormat.format(new Date(timestamp)));
        }

        // For more than a week ago, use date format
        return dateFormat.format(new Date(timestamp));
    }

    // Helper method to check if two calendar dates are the same day
    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}

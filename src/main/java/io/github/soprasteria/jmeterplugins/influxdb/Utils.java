package io.github.soprasteria.jmeterplugins.influxdb;

public class Utils {

    public static String timeFormatDuration(long millisec) {
        long seconds = (millisec / 1000);
        long sec = seconds % 60;
        long min = (seconds / 60) % 60;
        long hrs = (seconds / (60 * 60)) % 24;
        if (hrs > 0) {
            return String.format("%02d hour, %02d min, %02d sec", hrs, min, sec);
        } else {
            return String.format("%02d min, %02d sec", min, sec);
        }
    }
}

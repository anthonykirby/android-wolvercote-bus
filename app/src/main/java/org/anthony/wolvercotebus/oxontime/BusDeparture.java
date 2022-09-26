package org.anthony.wolvercotebus.oxontime;

import org.anthony.wolvercotebus.BogusDataError;

import androidx.annotation.NonNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * BusDeparture
 *
 * Represents a single departure:
 * - either an real-time prediction (minutes until due)
 * - or a timetable time (i.e. a clock time)
 */

public class BusDeparture {
    String routeCode;
    String destination;
    String displayTime;
    boolean isRealtime = false;

    BusDeparture routeCode(String routeCode) {
        this.routeCode = routeCode;
        return this;
    }

    BusDeparture destination(String destination) {
        this.destination = destination;
        return this;
    }

    BusDeparture displayTime(String displayTime) {
        this.displayTime = displayTime;
        this.isRealtime = isRealtime(displayTime);
        return this;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public String getDestination() {
        return destination;
    }

    public String getDisplayTime() {
        return displayTime;
    }

    public boolean isRealtime() {
        return isRealtime;
    }

    final static Pattern monitoredDisplayTimeRegex = Pattern.compile("([\\d]+) min");
    final static Pattern timetabledDisplayTimeRegex = Pattern.compile("([\\d]{2}:[\\d]{2})");
    public static boolean isRealtime(String displayTime) {
        if (displayTime.equals("Due")) {
            return true;
        }
        Matcher matcher = monitoredDisplayTimeRegex.matcher(displayTime);
        if (matcher.matches()) {
            //return Integer.parseInt(matcher.group(1));
            return true;
        }
        matcher = timetabledDisplayTimeRegex.matcher(displayTime);
        if (matcher.matches()) {
            return false;
        }
        // error
        throw new BogusDataError("failed to parse displayTime '"+displayTime+"'");
    }

    @NonNull
    @Override
    public String toString() {
        return "Service{" +
                "number='" + routeCode + '\'' +
                ", destination='" + destination + '\'' +
                ", displayTime='" + displayTime + '\'' +
                ", isRealtime='" + isRealtime + '\'' +
                '}';
    }
}

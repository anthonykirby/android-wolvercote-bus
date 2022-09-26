package org.anthony.wolvercotebus;

import org.anthony.wolvercotebus.oxontime.BusDeparture;

import java.util.List;

/*
 * StopDepartures
 *
 * The list of departures from a given stop
 */

public class StopDepartures {
    StopDefinition stop;
    List<BusDeparture> departures;

    StopDepartures(StopDefinition stop, List<BusDeparture> departures) {
        this.stop = stop;
        this.departures = departures;
    }

    public StopDefinition getStop() {
        return stop;
    }

    public List<BusDeparture> getDepartures() {
        return departures;
    }
}

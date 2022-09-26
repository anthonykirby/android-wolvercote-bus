package org.anthony.wolvercotebus.oxontime;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/*
 * OxontimeResponse
 *
 * Represents the departures for a specific stop, i.e. as returned from Oxontime
 */

public class OxontimeResponse {
    OxontimeResponse() {
        busDepartures = new ArrayList<>();
    }

    String stopName;
    String message;
    private final List<BusDeparture> busDepartures;

    String atcoCode;
    String naptanCode;

    OxontimeResponse atcoCode(String atcoCode) {
        this.atcoCode = atcoCode;
        return this;
    }

    OxontimeResponse naptanCode(String naptanCode) {
        this.naptanCode = naptanCode;
        return this;
    }

    @Deprecated
    String getStopName() {
        return stopName;
    }

    String getAtcoCode() {
        return atcoCode;
    }

    String getNaptanCode() {
        return naptanCode;
    }

    public List<BusDeparture> getServices() {
        return busDepartures;
    }

    void addService(BusDeparture busDeparture) {
        this.busDepartures.add(busDeparture);
        OxontimeDecoder.logger.info("adding service=" + busDeparture.toString());
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Stop=" + stopName + ":\n");
        for (BusDeparture busDeparture : busDepartures) {
            result.append("\tBus ")
                    .append(busDeparture.routeCode)
                    .append(" ")
                    .append(busDeparture.displayTime)
                    .append(" mins\n");
        }
        if (message != null)
            result.append("\tmessage=\"")
                    .append(message)
                    .append("\"\n");
        return result.toString();
    }
}

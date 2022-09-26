package org.anthony.wolvercotebus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.anthony.wolvercotebus.StopDefinition.Direction.*;

/*
 * StopDefinitions
 *
 * The actual set of stops that we retrieve data for:
 * - for the service we're interested in
 * - in both directions
 */

public class StopDefinitions {

    static final List<String> routes = Collections.singletonList("6");
    public static List<String> GetRoutes() {
        return routes;
    }

    public static List<StopDefinition> GetAllStops() {
        List<StopDefinition> all = new ArrayList<>();
        all.addAll(GetStops(INBOUND));
        all.addAll(GetStops(OUTBOUND));
        return all;
    }


    public static List<StopDefinition> GetStops(StopDefinition.Direction direction) {
        if (direction == INBOUND) {
            return new ArrayList<>(Arrays.asList(
                    new StopDefinition("https://oxontime.com/pwi/departureBoard/340021003PMS",
                            "Wolvercote, Papermill Square",
                            INBOUND),
                    new StopDefinition("https://oxontime.com/pwi/departureBoard/340001008EAS",
                            "Wolvercote, Home Close",
                            INBOUND),
                    new StopDefinition("https://oxontime.com/pwi/departureBoard/340001005FAR",
                            "Wolvercote Primary School",
                            INBOUND)
            ));
        } else {
            return new ArrayList<>(Arrays.asList(
                    new StopDefinition("https://oxontime.com/pwi/departureBoard/340000005C3",
                            "Oxford, Magdalen St Stop C3",
                            OUTBOUND),
                    new StopDefinition("https://oxontime.com/pwi/departureBoard/340001002OUT",
                            "Oxford, Radcliffe Observatory Quarter",
                            OUTBOUND),
                    new StopDefinition("https://oxontime.com/pwi/departureBoard/340000996OPP",
                            "Woodstock Road, South Parade",
                            OUTBOUND)
            ));
        }
    }
}

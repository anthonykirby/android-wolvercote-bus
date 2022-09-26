package org.anthony.wolvercotebus;

/*
 * StopDefinition
 *
 * To retrieve data from the Oxontime API, we need to call a different URL for each stop.
 */
public class StopDefinition {
    public enum Direction {INBOUND, OUTBOUND}

    public String url;
    public String description;
    public Direction direction;

    public StopDefinition(String url, String description, Direction direction) {
        this.url = url;
        this.description = description;
        this.direction = direction;
    }
}

package org.anthony.wolvercotebus;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.location.Location;

import org.anthony.wolvercotebus.LocationProcessor.UserLocation;

@RunWith(AndroidJUnit4.class)
public class LocationProcessorTest {

    @Test
    public void testNullLocations() {
        assertFalse(LocationProcessor.isNearOxford(null));
        assertFalse(LocationProcessor.isNearWolvercote(null));
        assertEquals(
                UserLocation.LOCATION_UNKNOWN,
                LocationProcessor.process(null));
    }

    @Test
    public void testFromWolvercoteRoundabout() {
        Location location = new Location("");
        location.setLatitude(51.7857851);
        location.setLongitude(-1.2863959);
        assertTrue(LocationProcessor.isNearWolvercote(location));
        assertFalse(LocationProcessor.isNearOxford(location));
        assertEquals(
                UserLocation.LOCATION_WOLVERCOTE,
                LocationProcessor.process(location));
    }

    @Test
    public void testFromOxfordStation() {
        Location location = new Location("");
        location.setLatitude(51.7556226);
        location.setLongitude(-1.2582862);
        assertFalse(LocationProcessor.isNearWolvercote(location));
        assertTrue(LocationProcessor.isNearOxford(location));
        assertEquals(
                UserLocation.LOCATION_OXFORD,
                LocationProcessor.process(location));
    }

    @Test
    public void testFromTimbuktu() {
        Location location = new Location("");
        location.setLatitude(16.7713828);
        location.setLongitude(-3.025489);
        assertFalse(LocationProcessor.isNearWolvercote(location));
        assertFalse(LocationProcessor.isNearOxford(location));
        assertEquals(
                UserLocation.LOCATION_ELSEWHERE,
                LocationProcessor.process(location));
    }
}
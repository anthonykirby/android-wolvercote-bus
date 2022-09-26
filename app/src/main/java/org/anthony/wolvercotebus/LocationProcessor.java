package org.anthony.wolvercotebus;

import android.location.Location;
import android.util.Log;

import static org.anthony.wolvercotebus.MainActivity.TAG;

import androidx.annotation.NonNull;

/*
 * LocationProcessor
 *
 * For a given user location, work out which end of the bus route this is
 * so that the relevant direction can be automatically selected
 */

class LocationProcessor {
	private LocationProcessor() {}

	public enum UserLocation {
		LOCATION_WOLVERCOTE,
		LOCATION_OXFORD,
		LOCATION_ELSEWHERE,
		LOCATION_UNKNOWN;

		@NonNull
		@Override
		public String toString() {
			switch (this) {
				case LOCATION_WOLVERCOTE:
					return "Wolvercote";
				case LOCATION_OXFORD:
					return "City Centre";
				case LOCATION_ELSEWHERE:
					return "(elsewhere)";
				default:
					return "(unknown)";
			}
		}
	}


	public static UserLocation process(Location location) {
		if (location==null) {
			return UserLocation.LOCATION_UNKNOWN;
		}
		Log.d(TAG, "location "+location.getLatitude()+","+location.getLongitude());
		if (isNearWolvercote(location)) {
			return UserLocation.LOCATION_WOLVERCOTE;
		} else if (isNearOxford(location)) {
			return UserLocation.LOCATION_OXFORD;
		} else {
			return UserLocation.LOCATION_ELSEWHERE;
		}
	}

	final static double LOCATION_WOLVERCOTE_LAT = 51.7859668;
	final static double LOCATION_WOLVERCOTE_LONG = -1.2924241;
	final static double LOCATION_OXFORD_LAT = 51.75522;
	final static double LOCATION_OXFORD_LONG = -1.25919;
	// tolerance is slightly less than half the separation
	final static double LOCATION_TOLERANCE_METRES = 2050.0;

	static boolean isNearWolvercote(Location location) {
		Location wolvercote = new Location("fixed");
		wolvercote.setLatitude(LOCATION_WOLVERCOTE_LAT);
		wolvercote.setLongitude(LOCATION_WOLVERCOTE_LONG);
		return isNear(wolvercote, location);
	}
	static boolean isNearOxford(Location location) {
		Location town = new Location("fixed");
		town.setLatitude(LOCATION_OXFORD_LAT);
		town.setLongitude(LOCATION_OXFORD_LONG);
		return isNear(town, location);
	}

	static boolean isNear(Location reference, Location location) {
		if (location == null) {
			return false;
		}
		float delta = reference.distanceTo(location);
		return delta < LOCATION_TOLERANCE_METRES;
	}
}

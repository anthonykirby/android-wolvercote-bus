package org.anthony.wolvercotebus.oxontime;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.anthony.wolvercotebus.BogusDataError;
import org.anthony.wolvercotebus.FatalError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/*
 * OxontimeDecoder
 *
 * Decode the JSON returned from the Oxontime API, extract the bits that
 * we find interesting, and put them in a more usable form.
 *
 */

public class OxontimeDecoder {

	public static String LOGGER_TAG = "OxontimeDecoder";
	public static class logger {
		public static void  info (String msg) {
			Log.i(LOGGER_TAG, msg);
		}
	}

	List<String> relevantRoutes;
	public OxontimeDecoder(List<String> routes) {
		this.relevantRoutes = routes;
	}
	boolean isRelevantService(BusDeparture service) {
		String serviceRoute = service.getRouteCode();
		for (String route: relevantRoutes) {
			if (route.equals(serviceRoute)) {
				return true;
			}
		}
		return false;
	}

	public OxontimeResponse parseUrl(String url) {
		BufferedReader in = null;
		HttpURLConnection conn=null;

		try
		{
		    // TODO try with resources
			conn = (HttpURLConnection) new URL(url).openConnection();
			if(conn.getResponseCode() != HttpURLConnection.HTTP_OK){
				throw new FatalError("HTTP error="
						+ conn.getResponseCode()
						+ " " + conn.getResponseMessage(), null);
			}
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			StringBuilder text = new StringBuilder();
			for ( String line; (line = in.readLine()) != null; ) {
				text.append(line).append(System.lineSeparator());
			}

			return parseJsonStringData(text.toString());
		} catch (IOException e) {
			throw new FatalError("IOException in parseUrl", e);
		} catch (BogusDataError e) {
			throw new FatalError("Failed to parse", e);
		} finally {
			try {
				if (null != in) {
					in.close();
				}
			} catch (IOException e) {
				// ignore
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
    }

	// development & testing: get input from previously captured test file
	OxontimeResponse parseFile(String filename) {
		try {
			String s = readFileToString(new File(filename));
			return parseJsonStringData(s);
		} catch (IOException e) {
			throw new FatalError("Failed to parse Oxontime Response:", e);
		}
	}
	public static String readFileToString( File file ) throws IOException {
		StringBuilder text = new StringBuilder();
		try(FileInputStream fileStream = new FileInputStream( file )) {
			BufferedReader br = new BufferedReader( new InputStreamReader( fileStream ) );
			for ( String line; (line = br.readLine()) != null; ) {
				text.append(line).append(System.lineSeparator());
			}
			return text.toString();
		}
	}


	public OxontimeResponse parseJsonStringData(String s) {
		// parse into json
		JsonElement jsonTree = JsonParser.parseString(s);
		if (jsonTree == null || !jsonTree.isJsonObject()) {
			throw new BogusDataError("expected top level object");
		}
		JsonObject jsonObject = jsonTree.getAsJsonObject();
		if (jsonObject.entrySet().size() != 1) {
			throw new BogusDataError("expected single top level object");
		}

		// top-level object should be key-value pair where key is the
		// atco code & value is data about the stop
		JsonElement stopJsonElement = jsonObject.entrySet().iterator().next().getValue();
		if (!stopJsonElement.isJsonObject()) {
			throw new BogusDataError("expected top level object");
		}
		JsonObject stopJson = stopJsonElement.getAsJsonObject();

		OxontimeResponse oxontimeResponse = new OxontimeResponse()
				.atcoCode(getStringValue(stopJson, "atco_code"))
				.naptanCode(getStringValue(stopJson, "naptan_code"));

		// also expect zero or more "calls" at that stop
		JsonElement callsElement = stopJson.get("calls");
		if (callsElement == null || !callsElement.isJsonArray()) {
			throw new BogusDataError("expected top level object");
		}
		JsonArray calls = callsElement.getAsJsonArray();
		logger.info("decoding "+calls.size()+" calls");
		for (JsonElement callElement : calls) {
			if (!callElement.isJsonObject()) {
				throw new BogusDataError("expected calls are object array", calls);
			}
			JsonObject call = callElement.getAsJsonObject();

			BusDeparture service = new BusDeparture()
					// NB could read 'route_code' or 'service_description'?
					.routeCode(getStringValue(call, "route_code"))
					.destination(getStringValue(call, "destination"))
					.displayTime(getStringValue(call, "display_time"));

			// TODO decode GPS location

			if (isRelevantService(service)) {
				oxontimeResponse.addService(service);
			} else {
				logger.info("ignoring service " + service);
			}

		}

		return oxontimeResponse;
	}

	// check & return a single JSON string value
	public static String getStringValue(JsonObject jo, String name) {
		JsonElement value = jo.get(name);
		if (!value.isJsonPrimitive()) {
			throw new BogusDataError("not a Json primitive '"+name+"'", jo);
		}
		if (!value.getAsJsonPrimitive().isString()) {
			throw new BogusDataError("does not contain string '"+name+"'", jo);
		}
		return value.getAsString();
	}
}

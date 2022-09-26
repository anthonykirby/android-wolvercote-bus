package org.anthony.wolvercotebus;

import static android.location.Criteria.ACCURACY_COARSE;
import static org.anthony.wolvercotebus.LocationProcessor.UserLocation.LOCATION_OXFORD;
import static org.anthony.wolvercotebus.LocationProcessor.UserLocation.LOCATION_UNKNOWN;
import static org.anthony.wolvercotebus.LocationProcessor.UserLocation.LOCATION_WOLVERCOTE;
import static org.anthony.wolvercotebus.StopDefinition.Direction.INBOUND;
import static org.anthony.wolvercotebus.StopDefinition.Direction.OUTBOUND;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.tabs.TabLayout;

import org.anthony.wolvercotebus.LocationProcessor.UserLocation;
import org.anthony.wolvercotebus.oxontime.BusDeparture;
import org.anthony.wolvercotebus.oxontime.OxontimeDecoder;
import org.anthony.wolvercotebus.oxontime.OxontimeResponse;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

	public final static String TAG = "WolvercoteBus";

	Button refreshButton;
	ProgressBar progressBar;
	TextView textLastUpdated;

	// tab layout
	TabLayout tabLayoutDirection;
	LinearLayout linearLayoutStops;
	StopDefinition.Direction travelDirection;

	UserLocation userLocation;

	private Date lastUpdated = null;
	List<StopDepartures> stopsDepartures = null;


	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		// we don't persist anything - we can just refresh
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "OnCreate");
		setContentView(R.layout.activity_main);

		refreshButton = findViewById(R.id.buttonRefresh);
		progressBar = findViewById(R.id.progressBar1);
		progressBar.setVisibility(ProgressBar.INVISIBLE);

		// tabs
		tabLayoutDirection = findViewById(R.id.tabLayout);
		linearLayoutStops = findViewById(R.id.linearLayoutStops);

		textLastUpdated = findViewById(R.id.textViewLastUpdated);
		populateLastUpdated();

		refreshButton.setOnClickListener(v -> new UpdateTask(this).execute());

		tabLayoutDirection.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				updateUI();
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {}
		});
		// poke to get initial value
		getSelectionTabDirection();
	}


	final static int RESUME_REFRESH_HOLDOFF_SECONDS = 2 * 60;

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "OnResume");
		populateLastUpdated();

		// if we're a while up to date (and not currently loading) then update
		if (lastUpdated == null || new Date().getTime() - lastUpdated.getTime() > 1000 * RESUME_REFRESH_HOLDOFF_SECONDS) {
			if (refreshButton.isEnabled()) {
				new UpdateTask(this).execute();
			}
		}

		// we start off not knowing where we are: request location
		userLocation = LOCATION_UNKNOWN;
		updateLocation();
	}


	void populateLastUpdated() {
		if (lastUpdated == null) {
			textLastUpdated.setText(
					getString(R.string.last_updated, getString(R.string.last_updated_no_data)));
		} else {
			textLastUpdated.setText(
					getString(R.string.last_updated, SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(lastUpdated)));
		}
	}


	static class UpdateTask extends AsyncTask<Void, Integer, List<StopDepartures>> {
		// weak ref, to avoid "This AsyncTask class should be static or leaks might occur"
		// (but the smell lingers...)
		private final WeakReference<MainActivity> activityRef;
		UpdateTask(MainActivity context) {
			activityRef = new WeakReference<>(context);
		}
		private MainActivity getActivity() { return activityRef.get(); }

		@Override
		protected void onPreExecute() {
			getActivity().updateTaskPreExecute();
		}

		@Override
		protected List<StopDepartures> doInBackground(Void... params) {
			return getActivity().updateTaskDoInBackground();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			Log.i(TAG, "UpdateTask::onProgressUpdate");
		}

		@Override
		protected void onPostExecute(List<StopDepartures> stopsDepartures) {
			getActivity().updateTaskPostExecute(stopsDepartures);
		}
	}

	void updateTaskPreExecute() {
		Log.i(TAG, "UpdateTask::onPreExecute");
		progressBar.setVisibility(ProgressBar.VISIBLE);
		refreshButton.setText(R.string.refresh_button_working);
		refreshButton.setEnabled(false);
	}


	protected List<StopDepartures> updateTaskDoInBackground() {
		Log.i(TAG, "UpdateTask::doInBackground");

		if (!isNetworkAvailable()) {
			runOnUiThread(() -> Toast.makeText(this,
					getString(R.string.toast_no_network),
					Toast.LENGTH_SHORT
			).show());
			return null;
		}

		List<StopDepartures> stopsDepartures = new ArrayList<>();
		try {
			// get times for multiple stops
			for (StopDefinition stop: StopDefinitions.GetAllStops()) {
				OxontimeDecoder p = new OxontimeDecoder(StopDefinitions.GetRoutes());
				Log.i(TAG, "getting stop "+stop.description+" ("+stop.url+")");
				OxontimeResponse ot = p.parseUrl(stop.url);
				stopsDepartures.add(new StopDepartures(stop, ot.getServices()));
			}
		} catch (final FatalError e) {
			// display error message
			runOnUiThread(() -> Toast.makeText(this,
					getString(R.string.toast_failed_data, e.getMessage()),
					Toast.LENGTH_SHORT
			).show());
		}

		return stopsDepartures;
	}

	void updateTaskPostExecute(List<StopDepartures> stopsDepartures) {
		progressBar.setVisibility(ProgressBar.INVISIBLE);
		refreshButton.setText(R.string.refresh_button);
		refreshButton.setEnabled(true);

		if (stopsDepartures == null || stopsDepartures.size() == 0) {
			lastUpdated = null;
		} else {
			lastUpdated = new Date();
		}

		dataUpdate(stopsDepartures);
		populateLastUpdated();
	}


	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager
				= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	void dataUpdate(List<StopDepartures> stopsDepartures) {
		this.stopsDepartures = stopsDepartures;
		updateUI();
		Toast.makeText(this,
				getString(R.string.toast_times_updated, userLocation),
				Toast.LENGTH_SHORT
		).show();
	}

	void locationUpdate(UserLocation newLocation) {
		Log.d(TAG, "locationUpdate = " + newLocation.toString());
		boolean locationChanged = newLocation != userLocation;
		userLocation = newLocation;
		if (locationChanged) {
			// Location has changed (or been initially set) so select the relevant
			// tab & refresh the contents
			Log.d(TAG, "travelDirection = " + travelDirection.toString());
			if (userLocation == LOCATION_OXFORD) {
				travelDirection = OUTBOUND;
			} else if (userLocation == LOCATION_WOLVERCOTE) {
				travelDirection = INBOUND;
			}
			selectTabDirection(travelDirection);
			updateUI();
			Toast.makeText(this,
					getString (R.string.toast_location_updated, newLocation.toString()),
					Toast.LENGTH_SHORT
			).show();
		}
	}

	void updateUI() {
		getSelectionTabDirection();
		fillTable(stopsDepartures);
	}

	void fillTable(List<StopDepartures> stopsDepartures) {

		// clear previous
		while (linearLayoutStops.getChildCount() > 0) {
			linearLayoutStops.removeViewAt(0);
		}

		if (stopsDepartures == null) {
			Log.i(TAG, "fillTable: no data");
			return;
		}

		// get stops/departures for the relevant direction
		List<StopDepartures> stops = stopsDepartures.stream()
    		.filter(p -> p.stop.direction == travelDirection).collect(Collectors.toList());

		// layout, expanding to fill parent
		FrameLayout.LayoutParams layoutFill = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);

		FrameLayout.LayoutParams layoutFillWithBorder = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		int separation = getResources().getDimensionPixelSize(R.dimen.stop_separation);
		layoutFillWithBorder.setMargins(0, 0,0, separation);

		for (StopDepartures stop: stops) {
			// title = 'from <stop name>'
			TextView stopTitle = new TextView(this);
			stopTitle.setText(MessageFormat.format("from {0}", stop.getStop().description));
			stopTitle.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
			linearLayoutStops.addView(stopTitle);

			// table of departures from this stop
			TableLayout stopTable = new TableLayout(this);
			stopTable.setLayoutParams(layoutFillWithBorder);
			stopTable.setStretchAllColumns(true);
			stopTable.setShrinkAllColumns(true);

			// first row is a heading
			linearLayoutStops.addView(stopTable);
			TableRow rowHeader = new TableRow(this);
			rowHeader.setLayoutParams(layoutFill);
			stopTable.addView(rowHeader);
			for (int headerElement: Arrays.asList(R.string.header_service, R.string.header_departure, R.string.header_realtime)) {
				TextView tv = new TextView(this);
				tv.setText(headerElement);
				rowHeader.addView(tv);
			}

			// subsequent rows for each departure from the stop
			for (BusDeparture departure: stop.getDepartures()) {
				TableRow row = new TableRow(this);
				stopTable.addView(row);

				TextView cellService = new TextView(this);
				cellService.setText(departure.getRouteCode());
				row.addView(cellService);

				TextView cellDestination = new TextView(this);
				cellDestination.setText(departure.getDisplayTime());
				row.addView(cellDestination);

				TextView cellDeparture = new TextView(this);
				if (departure.isRealtime()) {
					cellDeparture.setText("⚡");
				}
				row.addView(cellDeparture);
			}
		}
	}


	//============================================================================================
	// TabLayout to select travel direction
	//============================================================================================

	// two tabs, one for service inbound (to Oxford), the other for outbound
	private final int TAB_INDEX_INBOUND = 0;
	private final int TAB_INDEX_OUTBOUND = 1;

	// work out which tab is selected, and decode to travel direction
	void getSelectionTabDirection() {
		if (tabLayoutDirection.getSelectedTabPosition() == TAB_INDEX_INBOUND) {
			travelDirection = INBOUND;
		} else if(tabLayoutDirection.getSelectedTabPosition() == TAB_INDEX_OUTBOUND) {
			travelDirection = OUTBOUND;
		}
	}

	// select the relevant tab, from travel direction
	void selectTabDirection(StopDefinition.Direction travelDirection) {
		int tabIndex = travelDirection == OUTBOUND ? TAB_INDEX_OUTBOUND : TAB_INDEX_INBOUND;
		Objects.requireNonNull(tabLayoutDirection.getTabAt(tabIndex)).select();
	}


	//============================================================================================
	// Menus: display an "about" box
	//============================================================================================

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onAboutMenu(MenuItem item) {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.about_box);
		dialog.setTitle(R.string.aboutBox_title);

		ImageView image = dialog.findViewById(R.id.aboutBox_image);
		image.setImageResource(R.mipmap.ic_logo);

		TextView text = dialog.findViewById(R.id.aboutBox_text);
		text.setText(getString(R.string.aboutBox_text,
				"https://github.com/anthonykirby/android-wolvercote-bus",
				BuildConfig.VERSION_NAME,
				"https://www.oxontime.com"));
		// make link clickable
		text.setAutoLinkMask(RESULT_OK);
		text.setMovementMethod(LinkMovementMethod.getInstance());
		Linkify.addLinks(text, Linkify.WEB_URLS);

		Button dialogButtonOk = dialog.findViewById(R.id.aboutBox_buttonOk);
		dialogButtonOk.setOnClickListener(v -> dialog.dismiss());

		dialog.show();
	}


	//============================================================================================
	// Location interaction
	// - get the current location, i.e. "in Wolvercote" vs "in Oxford" vs elsewhere/unknown
	//============================================================================================

	// start the location update sequence
	void updateLocation() {
		Log.d(TAG, "updateLocation()");
		switch (getLocationPermission()) {
			case REFUSED:
				Log.i(TAG, "skipping location update (permission previously refused)");
				break;
			case GRANTED:
				Log.i(TAG, "we have location permission: start the check");
				Location location = getLocation();
				UserLocation newLocation = LocationProcessor.process(location);
				locationUpdate(newLocation);
				break;
			case PENDING:
				// request in progress (we'll retry if granted)
				break;
		}
	}

	// request the location (having previously checked the permission)
	Location getLocation() {
		LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(ACCURACY_COARSE);
		String provider = locationManager.getBestProvider (criteria,true);

		try {
			Log.i(TAG, "getLocation()");
			return locationManager.getLastKnownLocation(provider);
		} catch (SecurityException ex) {
			// we checked the permission, so this is primarily for the linter
			Log.w(TAG,"getLastKnownLocation failed", ex);
			return null;
		}
	}


	//=============================================================================================
	// Permission handling
	//=============================================================================================

	// three possible values for location permission
	public enum LocationPermission {
		GRANTED,	// we have permission
		REFUSED,	// we won't get permission
		PENDING		// we need to wait for user response
	}

	// user has refused permission (i.e. don't keep pestering)
	boolean locationPermissionRefused = false;

	LocationPermission getLocationPermission() {
		if (locationPermissionRefused) {
			return LocationPermission.REFUSED;
		} else if (ContextCompat.checkSelfPermission(
				this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
				PackageManager.PERMISSION_GRANTED) {
			return LocationPermission.GRANTED;
		} else {
			// we don't know;  need to request permission
			requestCoarseLocationPermission();
			return LocationPermission.PENDING;
		}
	}

	final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;

	void requestCoarseLocationPermission() {
		Log.i(TAG, "requesting COARSE_LOCATION permission");
		requestPermissions(
				new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
				PERMISSION_REQUEST_COARSE_LOCATION);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
			receiveCoarseLocationPermissionResult(grantResults);
		}
	}

	public void receiveCoarseLocationPermissionResult(int[] grantResults) {
		if (grantResults.length > 0 &&
				grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			Log.i(TAG, "COARSE_LOCATION permission was granted");
			// now we have the permission, (re)try the location request
			updateLocation();
		} else {
			Log.i(TAG, "COARSE_LOCATION permission was denied!");
			// don't keep pestering
			locationPermissionRefused = true;
		}
	}

}



package rh.tripper;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap map;
    private DrawerLayout mDrawerLayout;
    private FusedLocationProviderClient mFusedLocationClient;
    String email = null;
    Context context = null;
    JSONObject tripsJsonObject = null;
    JSONObject stopsJsonObject = null;
    NavigationView navigationView = null;
    String tripArray[] = {};
    String tripIDForStops = null;
    BottomSheetBehavior bottomSheetBehavior = null;
    Integer currentTrip = null;
    Boolean result = false;

    Calendar calendar = null;
    EditText stopArrivalDateBox = null;
    EditText stopDeptDateBox = null;
    int dateSwitch = 0;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    protected Location mLastLocation;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(false)
                        .setTitle("Add a stop")
                        .setMessage("How do you want to add your stop?")
                        .setPositiveButton("Current location", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if(mLastLocation != null){
                                    final View addStopDialogView = View.inflate(context, R.layout.add_stop_dialog, null);

                                    final EditText stopNameBox = addStopDialogView.findViewById(R.id.addStopName);
                                    stopArrivalDateBox = addStopDialogView.findViewById(R.id.addStopStartDate);
                                    stopDeptDateBox = addStopDialogView.findViewById(R.id.addStopEndDate);
                                    final EditText stopDescBox = addStopDialogView.findViewById(R.id.addStopDesc);

                                    calendar = Calendar.getInstance();

                                    final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
                                        @Override
                                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                            calendar.set(Calendar.YEAR, year);
                                            calendar.set(Calendar.MONTH, monthOfYear);
                                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                            updateLabel();
                                        }
                                    };

                                    stopArrivalDateBox.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            dateSwitch = 1;
                                            new DatePickerDialog(MainActivity.this, date, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
                                        }
                                    });

                                    stopDeptDateBox.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            dateSwitch = 2;
                                            new DatePickerDialog(MainActivity.this, date, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
                                        }
                                    });

                                    final AlertDialog.Builder addStopDetailsDialog = new AlertDialog.Builder(context);
                                    addStopDetailsDialog.setView(addStopDialogView)
                                            .setTitle("Add stop details")
                                            .setCancelable(false)
                                            .setNegativeButton("Add", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    boolean full = true;

                                                    String stopName = stopNameBox.getText().toString();
                                                    String stopArrivalDate = stopArrivalDateBox.getText().toString();
                                                    String stopDeptDate = stopDeptDateBox.getText().toString();
                                                    String stopDesc = stopDescBox.getText().toString();

                                                    if(TextUtils.isEmpty(stopName))
                                                    {
                                                        stopNameBox.setError("Required");
                                                        full = false;
                                                    }

                                                    if (stopArrivalDate.length() == 0)
                                                    {
                                                        stopArrivalDateBox.setError("Required");
                                                        full = false;
                                                    }

                                                    if (stopDeptDate.length() == 0)
                                                    {
                                                        stopDeptDateBox.setError("Required");
                                                        full = false;
                                                    }

                                                    if (stopDesc.length() == 0)
                                                    {
                                                        stopDescBox.setError("Required");
                                                        full = false;
                                                    }

                                                    if(full)
                                                    {
                                                        String [] stopDetails = new String[6];
                                                        stopDetails[0] = stopName;
                                                        stopDetails[1] = stopArrivalDate;
                                                        stopDetails[2] = stopDeptDate;
                                                        stopDetails[3] = stopDesc;
                                                        stopDetails[4] = String.valueOf(mLastLocation.getLatitude());
                                                        stopDetails[5] = String.valueOf(mLastLocation.getLongitude());

                                                        MainActivity.AddNewStopToTrip addNewStopToTrip = new MainActivity.AddNewStopToTrip();
                                                        addNewStopToTrip.execute(stopDetails);

                                                        Log.i(TAG, stopName + ", " + stopArrivalDate + ", " + stopDeptDate  + ", " + stopDesc + ", " + String.valueOf(mLastLocation.getLatitude()) + ", " + String.valueOf(mLastLocation.getLongitude()));
                                                    }
                                                }
                                            })
                                            .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            }).show();
                                }
                                else {
                                    showSnackbar("Your location was not found");
                                }
                            }
                        })
                        .setNegativeButton("Search", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Bundle extras = getIntent().getExtras();

        if(extras != null) {
            email = extras.getString("email");
        }

        // DEBUGGING
        email = "test@test.com";

        MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
        getAllTrips.execute(email);

        LinearLayout llBottomSheet = findViewById(R.id.bottom_sheet);

        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Integer ID = preferences.getInt("Current Trip", 0);

        if(ID > 0)
        {
            currentTrip = ID;
            tripIDForStops = String.valueOf(currentTrip);

            MainActivity.GetAllStops getAllStops = new MainActivity.GetAllStops();
            getAllStops.execute();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            getLastLocation();
        }
    }

    private void updateLabel() {
        String myFormat = "dd/mm/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.ENGLISH);

        if(dateSwitch == 1) {
            stopArrivalDateBox.setText(sdf.format(calendar.getTime()));
        } else if (dateSwitch == 2) {
            stopDeptDateBox.setText(sdf.format(calendar.getTime()));
        }
    }

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLastLocation = task.getResult();
                        } else {
                            Log.w(TAG, "getLastLocation:exception", task.getException());
                            showSnackbar(getString(R.string.no_location_detected));
                        }
                    }
                });
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(R.id.drawer_layout);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    private class GetAllTrips extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... emailIn) {

            HttpURLConnection urlConnection;
            InputStream in;

            try{
                URL url = new URL("http://10.0.2.2:3000/getalltrips?email="+ email );
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                tripsJsonObject = new JSONObject(responseStrBuilder.toString());
                urlConnection.disconnect();
                in.close();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally
            {
                return tripsJsonObject;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            mDrawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);

            Menu m = navigationView.getMenu();

            m.clear();

            SubMenu topMenu = m.addSubMenu("");

            topMenu.add(R.id.drawer_group, R.id.profile, Menu.FIRST, "Profile").setCheckable(false).setIcon(R.drawable.ic_account_circle_black_24dp);
            topMenu.add(R.id.drawer_group, R.id.settings, Menu.FIRST, "Settings").setCheckable(false).setIcon(R.drawable.ic_settings_black_24dp);
            topMenu.add(R.id.drawer_group, R.id.logout, Menu.FIRST, "Log Out").setCheckable(false).setIcon(R.drawable.ic_exit_to_app_black_24dp);

            SubMenu sub = m.addSubMenu("Trips");

            try
            {
                JSONArray tripJSON = tripsJsonObject.getJSONArray("trips");

                sub.add(R.id.trips_group, R.id.addTripID, Menu.FIRST, "Add a new trip").setCheckable(false).setIcon(R.drawable.ic_add_black_24dp);

                for(int i = 0; i <tripJSON.length(); i++)
                {
                    JSONObject tripObj = tripJSON.getJSONObject(i);
                    sub.add(R.id.trips_group, tripObj.getInt("tripID"), Menu.FIRST + i, tripObj.getString("tripName")).setCheckable(true).setIcon(R.drawable.ic_place_black_24dp);
                }
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class GetTripName extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {

            HttpURLConnection urlConnection;
            InputStream in;
            String result = null;

            try{
                URL url = new URL("http://10.0.2.2:3000/gettripname?tripid="+ currentTrip );
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                result = responseStrBuilder.toString();

                urlConnection.disconnect();
                in.close();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally
            {
                return result;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            final View editTripView = View.inflate(context, R.layout.edit_trip_dialog, null);
            final EditText newTripName = editTripView.findViewById(R.id.editTripName);

            newTripName.setText(result);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(editTripView)
                    .setCancelable(false)
                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String temp = newTripName.getText().toString();

                            MainActivity.SetTripName setTripName = new MainActivity.SetTripName();
                            setTripName.execute(temp);

                            dialog.cancel();
                        }
                    })

                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
        }
    }

    private class SetTripName extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... newName) {

            HttpURLConnection urlConnection;
            InputStream in;

            try{
                URL url = new URL("http://10.0.2.2:3000/settripname?tripid="+ currentTrip + "&tripname=" + newName[0] );
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                result = Boolean.valueOf(reader.readLine());

                urlConnection.disconnect();
                in.close();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally
            {
                return result;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result == true) {
                Snackbar.make(findViewById(R.id.drawer_layout), "Trip updated", Snackbar.LENGTH_LONG).show();
                MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
                getAllTrips.execute(email);
            } else
            {
                Snackbar.make(findViewById(R.id.drawer_layout), "Error updating trip", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_edit:
                MainActivity.GetTripName getTripName = new MainActivity.GetTripName();
                getTripName.execute();
                break;
            case R.id.action_test:
                Toast.makeText(context, "Testing", Toast.LENGTH_LONG).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        if (item.getItemId() == R.id.profile) {
            // Handle the camera action
            Toast.makeText(context, String.valueOf(item.getItemId()), Toast.LENGTH_LONG).show();
        } else if (item.getItemId() == R.id.settings) {
            Toast.makeText(context, String.valueOf(item.getItemId()), Toast.LENGTH_LONG).show();
        } else if (item.getItemId() == R.id.logout) {
            Toast.makeText(context, String.valueOf(item.getItemId()), Toast.LENGTH_LONG).show();
        } else if (item.getItemId() == R.id.addTripID) {

            final View addTripView = View.inflate(context, R.layout.add_trip_dialog, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(addTripView)
                    .setCancelable(false)
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            EditText tripname = addTripView.findViewById(R.id.addTripName);

                            String tripNameStr = tripname.getText().toString();

                            if(tripNameStr.length() > 0) {

                                MainActivity.CreateNewTrip createNewTrip = new MainActivity.CreateNewTrip();
                                createNewTrip.execute(tripNameStr);

                                dialog.cancel();
                            } else
                            {
                                tripname.setError("Required");
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
        } else {
                currentTrip = item.getItemId();

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("Current Trip", currentTrip);
                editor.apply();

                bottomSheetBehavior.setPeekHeight(0);

                tripIDForStops = String.valueOf(currentTrip);

                MainActivity.GetAllStops getAllStops = new MainActivity.GetAllStops();
                getAllStops.execute();
        }


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
    }

    private class GetAllStops extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {

            HttpURLConnection urlConnection;
            InputStream in;

            try{
                URL url = new URL("http://10.0.2.2:3000/getallstops?tripid="+ tripIDForStops);
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                stopsJsonObject = new JSONObject(responseStrBuilder.toString());
                urlConnection.disconnect();
                in.close();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally
            {
                return stopsJsonObject;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {

            map.clear();

            List<LatLng> latlngs = new ArrayList<>();

            try {
                final JSONArray stopsJSON = stopsJsonObject.getJSONArray("stops");
                //tripArray = new String[stopsJSON.length()];

                for (int i = 0; i < stopsJSON.length(); i++) {
                    JSONObject stopsObj = stopsJSON.getJSONObject(i);

                    Double lat = Double.parseDouble(stopsObj.getString("lat"));
                    Double lng = Double.parseDouble(stopsObj.getString("long"));

                    String stopName = stopsObj.getString("stopName");
                    String stopID = stopsObj.getString("stopID");

                    Marker markerObj;

                    LatLng marker = new LatLng(lat, lng);
                    markerObj =  map.addMarker(new MarkerOptions().position(marker).title(stopName));

                    latlngs.add(new LatLng(lat, lng));

                    markerObj.setTag(stopID);


                    if(i == 0){
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 4));
                    }
                }

                map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        String ID = (String) (marker.getTag());
                        int IDint = Integer.parseInt(ID);

                        //Toast.makeText(context, ID, Toast.LENGTH_SHORT).show();

                        try {
                            final JSONArray stopJSON = stopsJsonObject.getJSONArray("stops");

                            for (int i = 0; i < stopsJSON.length(); i++) {

                                JSONObject stopObject = stopsJSON.getJSONObject(i);

                                if(stopObject.getString("stopID") == ID) {

                                    TextView stopName = findViewById(R.id.bottom_stop_name);
                                    TextView stopDesc = findViewById(R.id.bottom_stop_desc);
                                    TextView arrivalDate = findViewById(R.id.bottom_arrival_date);
                                    TextView deptDate = findViewById(R.id.bottom_dept_date);

                                    //Toast.makeText(context, stopObject.getString("stopID") + " " + ID, Toast.LENGTH_SHORT).show();
                                    stopName.setText(stopObject.getString("stopName"));
                                    stopDesc.setText(stopObject.getString("stopDesc"));
                                    arrivalDate.setText(getString(R.string.arrivalLabelStart) + stopObject.getString("arrivalDate"));
                                    deptDate.setText(getString(R.string.deptLabelStart) + stopObject.getString("deptDate"));

                                    map.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));

                                    bottomSheetBehavior.setPeekHeight(100);
                                }
                            }
                        } catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                        return false;
                    }
                });

                PolylineOptions polylineOptions = new PolylineOptions().addAll(latlngs);

                map.addPolyline(polylineOptions);

            } catch(JSONException e){
                e.printStackTrace();
            }
        }
    }

    private class CreateNewTrip extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... tripName) {
            String tripNameStr = tripName[0];

            HttpURLConnection urlConnection = null;
            InputStream in = null;

            try{
                URL url = new URL("http://10.0.2.2:3000/addtrip?email="+email+"&tripname="+tripNameStr+"&startdate=2018-04-26 00:00:00");
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                result = Boolean.valueOf(reader.readLine());
                urlConnection.disconnect();
                in.close();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally
            {
                return result;
            }
        }
        protected void onPostExecute(Boolean result) {
            if(result == true){
                MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
                getAllTrips.execute(email);
            }
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(true)
                        .setMessage("There was a problem. Please try again.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        })
                        .show();
            }
        }
    }

    private class AddNewStopToTrip extends AsyncTask<String, Void, Boolean>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... stopDetails) {
            HttpURLConnection urlConnection = null;
            InputStream in = null;

            try{
                URL url = new URL("http://10.0.2.2:3000/addstop?tripid="+ currentTrip + "&email=" + email + "&arrivaldate=" + stopDetails[1] +"&deptdate=" + stopDetails[2] + "&stopname=" + stopDetails[0] + "&stopdesc=" + stopDetails[3] + "&lat=" + stopDetails[4] + "&long=" + stopDetails[5]);
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                result = Boolean.valueOf(reader.readLine());
                urlConnection.disconnect();
                in.close();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally
            {
                return result;
            }
        }

        protected void onPostExecute(Boolean result) {


        }
    }
}

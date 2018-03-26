package rh.tripper;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap map;
    private DrawerLayout mDrawerLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Integer ID = preferences.getInt("Current Trip", 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                                View addStopView = View.inflate(context, R.layout.add_stop_dialog, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(addStopView)
                        .setCancelable(false)
                        .setPositiveButton("Current location", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

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
        email = "test@test.com";

        MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
        getAllTrips.execute(email);

        LinearLayout llBottomSheet = findViewById(R.id.bottom_sheet);

        // init the bottom sheet behavior
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);

        // change the state of the bottom sheet

        // set the peek height
        bottomSheetBehavior.setPeekHeight(0);

        // set hideable or not
        bottomSheetBehavior.setHideable(false);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // set callback for changes
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        if(ID > 0)
        {
            currentTrip = ID;
            tripIDForStops = String.valueOf(currentTrip);

            MainActivity.GetAllStops getAllStops = new MainActivity.GetAllStops();
            getAllStops.execute();
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

                    Log.i("Setting ID: ", stopID);

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

                                    Log.i("Clicking ID:", ID);
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

    /*private class addNewStopToTrip extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection urlConnection = null;
            InputStream in = null;

            try{
                URL url = new URL("http://10.0.2.2:3000/addstop?tripid="+"&email="+"&arrivaldate="+"&deptdate="+"&stopname="+"&stopdesc="+"&lat="+"&long="+"");
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
    }*/
}

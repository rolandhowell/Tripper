package rh.tripper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap map;
    private DrawerLayout mDrawerLayout;
    RelativeLayout progress = null;
    String email = null;
    Context context = null;
    JSONObject tripsJsonObject = null;
    JSONObject stopsJsonObject = null;
    NavigationView navigationView = null;
    String tripArray[] = {};
    String tripIDForStops = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Add a stop", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Bundle extras = getIntent().getExtras();

        if(extras != null) {
            email = extras.getString("email");
        }
        email = "test@test.com";

        MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
        getAllTrips.execute(email);
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
            SubMenu sub = m.addSubMenu("Trips");
            try
            {
                JSONArray tripJSON = tripsJsonObject.getJSONArray("trips");
                tripArray = new String[tripJSON.length()];

                for(int i = 0; i <tripJSON.length(); i++)
                {
                    JSONObject tripObj = tripJSON.getJSONObject(i);
                    //sub.add(tripObj.getString("tripName")).setCheckable(false).setIcon(R.drawable.ic_place_black_24dp);
                    //sub.add(R.id.drawer_group, , i, "Test " + i).setIcon(R.drawable.ic_place_black_24dp);
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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
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
        Integer id = item.getItemId();

        if (id == R.id.profile) {
            // Handle the camera action
            Toast.makeText(context, String.valueOf(id), Toast.LENGTH_LONG).show();
        } else if (id == R.id.settings) {
            Toast.makeText(context, String.valueOf(id), Toast.LENGTH_LONG).show();
        } else if (id == R.id.logout) {
            Toast.makeText(context, String.valueOf(id), Toast.LENGTH_LONG).show();
        } else {
            tripIDForStops = String.valueOf(id);

            MainActivity.GetAllStops getAllStops = new MainActivity.GetAllStops();
            getAllStops.execute();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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

            try {
                JSONArray stopsJSON = stopsJsonObject.getJSONArray("stops");
                tripArray = new String[stopsJSON.length()];

                for (int i = 0; i < stopsJSON.length(); i++) {
                    JSONObject stopsObj = stopsJSON.getJSONObject(i);

                    Double lat = Double.parseDouble(stopsObj.getString("lat"));
                    Double lng = Double.parseDouble(stopsObj.getString("long"));

                    String stopName = stopsObj.getString("stopName");

                    LatLng marker = new LatLng(lat, lng);
                    map.addMarker(new MarkerOptions().position(marker).title(stopName));

                    if(i == 0){
                        map.moveCamera(CameraUpdateFactory.newLatLng(marker));
                    }
                }

            } catch(JSONException e){
                e.printStackTrace();
            }
        }
    }
}

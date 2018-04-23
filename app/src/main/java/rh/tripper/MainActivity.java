package rh.tripper;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
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
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.END;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    String urlString = "http://ec2-18-221-155-124.us-east-2.compute.amazonaws.com:3000";
    //String urlString = "http://10.0.2.2:3000/";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    protected Location mLastLocation;
    String email = null;
    Context context = null;
    JSONObject tripsJsonObject = null;
    JSONObject stopsJsonObject = null;
    NavigationView navigationView = null;
    String tripIDForStops = null;
    BottomSheetBehavior bottomSheetBehavior = null;
    Integer currentTrip = null;
    Boolean result = false;
    int currentStop;
    FloatingActionButton fab;

    Calendar calendar = null;
    EditText stopArrivalDateBox = null;
    EditText stopDeptDateBox = null;
    int dateSwitch = 0;
    private GoogleMap map;
    private DrawerLayout mDrawerLayout;
    private FusedLocationProviderClient mFusedLocationClient;

    Boolean offlineMode = false;
    DatabaseHelper mDBHelper = null;

    double addStopLat;
    double addStopLong;
    String addStopName;
    ConstraintLayout autoLayout;
    LinearLayout llBottomSheet;
    Boolean stopEdit = false;

    int height = 0;
    Boolean updatingStop = false;
    Boolean byCurrLoc = false;

    public static final String AUTHORITY = "rh.tripper.provider";
    public static final String ACCOUNT_TYPE = "rh.tripper";
    public static final String ACCOUNT = "mainaccount";
    public static final long SECOND_PER_MIN = 60L;
    public static final long SYNC_INTERVAL_IN_MINS = 5L;
    public static final long SYNC_INTERVAL = SECOND_PER_MIN * SYNC_INTERVAL_IN_MINS;
    Account mAccount;
    ContentResolver resolver;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        mAccount = CreateSyncAccount(this);
        resolver = getContentResolver();

        ContentResolver.addPeriodicSync(mAccount, AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autoLayout = findViewById(R.id.autocomplete_layout);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                addStopName = (String) place.getName();
                addStopLat = place.getLatLng().latitude;
                addStopLong = place.getLatLng().longitude;

                autoLayout.setVisibility(View.GONE);

                if (updatingStop) {
                    MainActivity.UpdateStopLocation updateStopLocation = new MainActivity.UpdateStopLocation();
                    updateStopLocation.execute();
                } else {
                    final View addStopDialogView = View.inflate(context, R.layout.add_stop_dialog, null);

                    final EditText stopNameBox = addStopDialogView.findViewById(R.id.addStopName);
                    stopNameBox.setText(addStopName);
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

                                    if (TextUtils.isEmpty(stopName)) {
                                        stopNameBox.setError("Required");
                                        full = false;
                                    }

                                    if (stopArrivalDate.length() == 0) {
                                        stopArrivalDateBox.setError("Required");
                                        full = false;
                                    }

                                    if (stopDeptDate.length() == 0) {
                                        stopDeptDateBox.setError("Required");
                                        full = false;
                                    }

                                    if (stopDesc.length() == 0) {
                                        stopDescBox.setError("Required");
                                        full = false;
                                    }

                                    if (full) {
                                        String[] stopDetails = new String[6];
                                        stopDetails[0] = stopName;
                                        stopDetails[1] = stopArrivalDate;
                                        stopDetails[2] = stopDeptDate;
                                        stopDetails[3] = stopDesc;
                                        stopDetails[4] = String.valueOf(addStopLat);
                                        stopDetails[5] = String.valueOf(addStopLong);

                                        AddNewStopToTrip addNewStopToTrip = new AddNewStopToTrip();
                                        addNewStopToTrip.execute(stopDetails);

                                        Log.i(TAG, stopName + ", " + stopArrivalDate + ", " + stopDeptDate + ", " + stopDesc + ", " + String.valueOf(mLastLocation.getLatitude()) + ", " + String.valueOf(mLastLocation.getLongitude()));
                                    }
                                }
                            })
                            .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }).show();
                }
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        MainActivity.isConnectedToServer isConnectedToServer = new MainActivity.isConnectedToServer();
        isConnectedToServer.execute();

        mDBHelper = new DatabaseHelper(this);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(false)
                        .setTitle("Add a stop")
                        .setMessage("How do you want to add your stop?")
                        .setPositiveButton("Current location", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (mLastLocation != null) {
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

                                                    if (TextUtils.isEmpty(stopName)) {
                                                        stopNameBox.setError("Required");
                                                        full = false;
                                                    }

                                                    if (stopArrivalDate.length() == 0) {
                                                        stopArrivalDateBox.setError("Required");
                                                        full = false;
                                                    }

                                                    if (stopDeptDate.length() == 0) {
                                                        stopDeptDateBox.setError("Required");
                                                        full = false;
                                                    }

                                                    if (stopDesc.length() == 0) {
                                                        stopDescBox.setError("Required");
                                                        full = false;
                                                    }

                                                    if (full) {
                                                        String[] stopDetails = new String[6];
                                                        stopDetails[0] = stopName;
                                                        stopDetails[1] = stopArrivalDate;
                                                        stopDetails[2] = stopDeptDate;
                                                        stopDetails[3] = stopDesc;
                                                        stopDetails[4] = String.valueOf(mLastLocation.getLatitude());
                                                        stopDetails[5] = String.valueOf(mLastLocation.getLongitude());

                                                        AddNewStopToTrip addNewStopToTrip = new AddNewStopToTrip();
                                                        addNewStopToTrip.execute(stopDetails);
                                                    }
                                                }
                                            })
                                            .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            }).show();
                                } else {
                                    showSnackbar("Your location was not found");
                                }
                            }
                        })
                        .setNegativeButton("Search", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                autoLayout.setVisibility(View.VISIBLE);
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

        if (extras != null) {
            email = extras.getString("email");
        }

        llBottomSheet = findViewById(R.id.bottom_sheet);

        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == 3) {
                    Button stopEditButton = findViewById(R.id.stopEditButton);
                    Button stopDeleteButton = findViewById(R.id.stopDelButton);

                    stopEditButton.setVisibility(View.VISIBLE);
                    stopDeleteButton.setVisibility(View.VISIBLE);
                }

                if (newState == 4 || newState == 1) {
                    Button stopEditButton = findViewById(R.id.stopEditButton);
                    Button stopDeleteButton = findViewById(R.id.stopDelButton);

                    stopEditButton.setVisibility(View.GONE);
                    stopDeleteButton.setVisibility(View.GONE);

                    Resources r = context.getResources();
                    int px = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            60,
                            r.getDisplayMetrics()
                    );

                    CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                            CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                            CoordinatorLayout.LayoutParams.WRAP_CONTENT
                    );

                    params.gravity = BOTTOM | END;
                    params.bottomMargin = px;

                    px = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            10,
                            r.getDisplayMetrics()
                    );
                    params.rightMargin = px;
                    fab.setLayoutParams(params);
                }

                if (newState == 5) {
                    Resources r = context.getResources();
                    int px = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            10,
                            r.getDisplayMetrics()
                    );

                    CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                            CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                            CoordinatorLayout.LayoutParams.WRAP_CONTENT
                    );

                    params.gravity = BOTTOM | END;
                    params.bottomMargin = px;
                    params.rightMargin = px;
                    fab.setLayoutParams(params);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    private Account CreateSyncAccount(MainActivity mainActivity) {
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);

        accountManager.addAccountExplicitly(newAccount, null, null);
        return newAccount;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            getLastLocation();
        }

        MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
        getAllTrips.execute();
    }

    private void updateLabel() {
        String myFormat = "dd/mm/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.ENGLISH);

        if (dateSwitch == 1) {
            stopArrivalDateBox.setText(sdf.format(calendar.getTime()));
        } else if (dateSwitch == 2) {
            stopDeptDateBox.setText(sdf.format(calendar.getTime()));
        }
    }

    @SuppressLint("MissingPermission")
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

    /*private void showToast(final String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }*/

    private void showSnackbar(final String text) {
        View container = findViewById(R.id.drawer_layout);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            startLocationPermissionRequest();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
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
            case R.id.del_trip:
                final AlertDialog.Builder deleteTripDialog = new AlertDialog.Builder(context);
                deleteTripDialog.setTitle("Delete Trip")
                        .setMessage("Are you sure you want to delete this trip and all it's stops?")
                        .setCancelable(true)
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                MainActivity.DeleteTrip deleteTrip = new MainActivity.DeleteTrip();
                                deleteTrip.execute();
                                dialog.cancel();
                            }
                        }).show();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.profile) {
            Intent i = new Intent(context, UpdateProfileActivity.class);
            startActivity(i);
        } else if (item.getItemId() == R.id.settings) {
            Toast.makeText(context, String.valueOf(item.getItemId()), Toast.LENGTH_LONG).show();
        } else if (item.getItemId() == R.id.logout) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Are you sure?")
                    .setCancelable(true)
                    .setPositiveButton("Log out", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            logout();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
        } else if (item.getItemId() == R.id.addTripID) {

            final View addTripView = View.inflate(context, R.layout.add_trip_dialog, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(addTripView)
                    .setCancelable(false)
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            EditText tripname = addTripView.findViewById(R.id.addTripName);

                            String tripNameStr = tripname.getText().toString();

                            if (tripNameStr.length() > 0) {

                                MainActivity.CreateNewTrip createNewTrip = new MainActivity.CreateNewTrip();
                                createNewTrip.execute(tripNameStr);

                                dialog.cancel();
                            } else {
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
        map.getUiSettings().setMapToolbarEnabled(false);
    }

    @SuppressLint("StaticFieldLeak")
    private class GetAllTrips extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... emailIn) {

            /*if(!offlineMode) {
                //offline mode enabled

                DatabaseHelper mDbHelper = new DatabaseHelper(context);

                SQLiteDatabase db = mDbHelper.getReadableDatabase();

                // Define a projection that specifies which columns from the database
                // you will actually use after this query.
                String[] projection = {
                        DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID,
                        DatabaseHelper.TripEntry.COLUMN_NAME_TRIPNAME,
                        DatabaseHelper.TripEntry.COLUMN_NAME_STARTDATE
                };

                // How you want the results sorted in the resulting Cursor
                String sortOrder =
                        DatabaseHelper.TripEntry.COLUMN_NAME_STARTDATE + " DESC";

                Cursor cursor = db.query(
                        DatabaseHelper.TripEntry.TABLE_NAME,   // The table to query
                        projection,             // The array of columns to return (pass null to get all)
                        null,              // The columns for the WHERE clause
                        null,          // The values for the WHERE clause
                        null,                   // don't group the rows
                        null,                   // don't filter by row groups
                        sortOrder               // The sort order
                );

                tripsJsonObject = new JSONObject();

                String tripJSON = "{\"trips\":[";

                while(cursor.moveToNext()) {
                    tripJSON = tripJSON + "{\"tripID\":" + cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID)) + ", \"tripName\":\"" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPNAME)) + "\",\"startDate\":\"" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_STARTDATE)) + "\"},";
                }
                cursor.close();

                tripJSON = tripJSON.substring(0, tripJSON.length() - 1);

                tripJSON = tripJSON + "]}";

                StringBuilder responseStrBuilder = new StringBuilder();
                responseStrBuilder.append(tripJSON);

                try {
                    tripsJsonObject = new JSONObject(responseStrBuilder.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return tripsJsonObject;
            } else {*/
            //offline mode disabled
            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL(urlString + "/getalltrips?email=" + email);
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
                return tripsJsonObject;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
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

            try {
                JSONArray tripJSON = tripsJsonObject.getJSONArray("trips");

                sub.add(R.id.trips_group, R.id.addTripID, Menu.FIRST, "Add a new trip").setCheckable(false).setIcon(R.drawable.ic_add_black_24dp);

                SQLiteDatabase db = mDBHelper.getWritableDatabase();

                ContentValues tripDetails = new ContentValues();

                for (int i = 0; i < tripJSON.length(); i++) {
                    JSONObject tripObj = tripJSON.getJSONObject(i);
                    sub.add(R.id.trips_group, tripObj.getInt("tripID"), Menu.FIRST + i, tripObj.getString("tripName")).setCheckable(true).setIcon(R.drawable.ic_place_black_24dp);

                    if (!offlineMode) {
                        String[] projection = {DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID};

                        String selection = DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID + " = ?";
                        String[] selectionArgs = {String.valueOf(tripObj.getInt("tripID"))};

                        Cursor cursor = db.query(
                                DatabaseHelper.TripEntry.TABLE_NAME,
                                projection,
                                selection,
                                selectionArgs,
                                null,
                                null,
                                null
                        );

                        long itemID = 0;

                        while (cursor.moveToNext()) {
                            itemID = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID));
                        }
                        cursor.close();

                        if (itemID > 0) {
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID, String.valueOf(tripObj.getInt("tripID")));
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_EMAIL, email);
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPNAME, tripObj.getString("tripName"));
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_STARTDATE, tripObj.getString("startDate"));

                            String tripUpdate = DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID + " LIKE ?";
                            String[] tripUpdateArgs = {String.valueOf(tripObj.getInt("tripID"))};

                            db.update(
                                    DatabaseHelper.TripEntry.TABLE_NAME,
                                    tripDetails,
                                    tripUpdate,
                                    tripUpdateArgs);
                        } else {
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID, String.valueOf(tripObj.getInt("tripID")));
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_EMAIL, email);
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPNAME, tripObj.getString("tripName"));
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_STARTDATE, tripObj.getString("startDate"));

                            db.insert(DatabaseHelper.TripEntry.TABLE_NAME, null, tripDetails);
                        }

                        tripDetails.clear();
                    }
                }

                mDBHelper.close();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
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

            try {
                URL url = new URL(urlString + "/gettripname?tripid=" + currentTrip);
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
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
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

    public void SwitchBoxes() {

        if (stopEdit) {
            TextView nameView = findViewById(R.id.bottom_stop_name);
            TextView arrivalView = findViewById(R.id.bottom_arrival_date);
            TextView deptView = findViewById(R.id.bottom_dept_date);
            TextView descView = findViewById(R.id.bottom_stop_desc);

            EditText nameEdit = findViewById(R.id.bottom_stop_name_edit);
            EditText arrivalEdit = findViewById(R.id.bottom_arrival_date_edit);
            EditText deptEdit = findViewById(R.id.bottom_dept_date_edit);
            EditText descEdit = findViewById(R.id.bottom_stop_desc_edit);

            Button editButton = findViewById(R.id.stopEditButton);
            Button delButton = findViewById(R.id.stopDelButton);
            Button saveButton = findViewById(R.id.stopSaveButton);

            editButton.setVisibility(View.VISIBLE);
            delButton.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.GONE);

            String name = nameEdit.getText().toString();
            String arrival = "Arrival: " + arrivalEdit.getText().toString();
            String dept = "Left: " + deptEdit.getText().toString();
            String desc = descEdit.getText().toString();

            String[] details = new String[4];
            details[0] = arrivalEdit.getText().toString();
            details[1] = deptEdit.getText().toString();
            details[2] = nameEdit.getText().toString();
            details[3] = descEdit.getText().toString();

            MainActivity.UpdateStopDetails updateStopDetails = new MainActivity.UpdateStopDetails();
            updateStopDetails.execute(details);

            nameEdit.setVisibility(View.GONE);
            arrivalEdit.setVisibility(View.GONE);
            deptEdit.setVisibility(View.GONE);
            descEdit.setVisibility(View.GONE);

            nameView.setText(name);
            arrivalView.setText(arrival);
            deptView.setText(dept);
            descView.setText(desc);

            nameView.setVisibility(View.VISIBLE);
            arrivalView.setVisibility(View.VISIBLE);
            deptView.setVisibility(View.VISIBLE);
            descView.setVisibility(View.VISIBLE);

            Button changeLocationButton = findViewById(R.id.changeLocationButton);
            changeLocationButton.setVisibility(View.GONE);

            stopEdit = false;
        } else {
            TextView nameView = findViewById(R.id.bottom_stop_name);
            TextView arrivalView = findViewById(R.id.bottom_arrival_date);
            TextView deptView = findViewById(R.id.bottom_dept_date);
            TextView descView = findViewById(R.id.bottom_stop_desc);

            Button editButton = findViewById(R.id.stopEditButton);
            Button delButton = findViewById(R.id.stopDelButton);
            Button saveButton = findViewById(R.id.stopSaveButton);

            editButton.setVisibility(View.GONE);
            delButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.VISIBLE);

            String name = nameView.getText().toString();
            String arrival = arrivalView.getText().toString().substring(8);
            String dept = deptView.getText().toString().substring(5);
            String desc = descView.getText().toString();

            nameView.setVisibility(View.GONE);
            arrivalView.setVisibility(View.GONE);
            deptView.setVisibility(View.GONE);
            descView.setVisibility(View.GONE);

            EditText nameEdit = findViewById(R.id.bottom_stop_name_edit);
            EditText arrivalEdit = findViewById(R.id.bottom_arrival_date_edit);
            EditText deptEdit = findViewById(R.id.bottom_dept_date_edit);
            EditText descEdit = findViewById(R.id.bottom_stop_desc_edit);

            nameEdit.setText(name);
            arrivalEdit.setText(arrival);
            deptEdit.setText(dept);
            descEdit.setText(desc);

            nameEdit.setVisibility(View.VISIBLE);
            arrivalEdit.setVisibility(View.VISIBLE);
            deptEdit.setVisibility(View.VISIBLE);
            descEdit.setVisibility(View.VISIBLE);

            Button changeLocationButton = findViewById(R.id.changeLocationButton);
            changeLocationButton.setVisibility(View.VISIBLE);
            changeLocationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updatingStop = true;

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setCancelable(false)
                            .setTitle("Add a stop")
                            .setMessage("How do you want to add your stop?")
                            .setPositiveButton("Current location", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    byCurrLoc = true;
                                    if (mLastLocation != null) {
                                        MainActivity.UpdateStopLocation updateStopLocation = new MainActivity.UpdateStopLocation();
                                        updateStopLocation.execute();
                                    } else {
                                        showSnackbar("Your location was not found");
                                    }
                                }
                            })
                            .setNegativeButton("Search", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    byCurrLoc = false;
                                    autoLayout.setVisibility(View.VISIBLE);
                                }
                            }).show();
                }
            });

            stopEdit = true;
        }
    }

    public void logout() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove("Current Trip").apply();
        preferences.edit().remove("User").apply();

        Intent i = new Intent(context, LandingActivity.class);
        startActivity(i);
    }

    @SuppressLint("StaticFieldLeak")
    private class SetTripName extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... newName) {

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL(urlString + "/settripname?tripid=" + currentTrip + "&tripname=" + newName[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                result = Boolean.valueOf(reader.readLine());

                urlConnection.disconnect();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return result;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Snackbar.make(findViewById(R.id.drawer_layout), "Trip updated", Snackbar.LENGTH_LONG).show();
                MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
                getAllTrips.execute(email);
            } else {
                Snackbar.make(findViewById(R.id.drawer_layout), "Error updating trip", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class CreateNewTrip extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... tripName) {
            String tripNameStr = tripName[0];

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL(urlString + "/addtrip?email=" + email + "&tripname=" + tripNameStr + "&startdate=2018-04-26 00:00:00");
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                result = Boolean.valueOf(reader.readLine());
                urlConnection.disconnect();
                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return result;
            }
        }

        protected void onPostExecute(Boolean result) {
            if (result == true) {
                MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
                getAllTrips.execute(email);
            } else {
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

    @SuppressLint("StaticFieldLeak")
    private class AddNewStopToTrip extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... stopDetails) {
            HttpURLConnection urlConnection = null;
            InputStream in = null;

            try {
                URL url = new URL(urlString + "/addstop?tripid=" + currentTrip + "&email=" + email + "&arrivaldate=" + stopDetails[1] + "&deptdate=" + stopDetails[2] + "&stopname=" + stopDetails[0] + "&stopdesc=" + stopDetails[3] + "&lat=" + stopDetails[4] + "&long=" + stopDetails[5]);
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                result = Boolean.valueOf(reader.readLine());
                urlConnection.disconnect();
                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return result;
            }
        }

        protected void onPostExecute(Boolean result) {


        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DeleteStop extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... emailIn) {

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL(urlString + "/delstop?stopid=" + currentStop);
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                urlConnection.disconnect();
                in.close();
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {

            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                MainActivity.GetAllStops getAllStops = new MainActivity.GetAllStops();
                getAllStops.execute();
                showSnackbar("Your stop was deleted.");
            } else {
                showSnackbar("There was a problem, please try again.");
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetAllStops extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {

            /*if(!offlineMode) {
                //offline mode enabled
                DatabaseHelper mDbHelper = new DatabaseHelper(context);

                SQLiteDatabase db = mDbHelper.getReadableDatabase();

                // Define a projection that specifies which columns from the database
                // you will actually use after this query.
                String[] projection = {
                        DatabaseHelper.StopEntry.COLUMN_NAME_STOPID,
                        DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME,
                        DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL,
                        DatabaseHelper.StopEntry.COLUMN_NAME_DEPT,
                        DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC,
                        DatabaseHelper.StopEntry.COLUMN_NAME_LAT,
                        DatabaseHelper.StopEntry.COLUMN_NAME_LONG
                };

                // Filter results WHERE "title" = 'My Title'
                String selection = DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID + " = ?";
                String[] selectionArgs = {String.valueOf(currentTrip)};


                // How you want the results sorted in the resulting Cursor
                String sortOrder =
                        DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL + " DESC";

                Cursor cursor = db.query(
                        DatabaseHelper.StopEntry.TABLE_NAME,   // The table to query
                        projection,             // The array of columns to return (pass null to get all)
                        selection,              // The columns for the WHERE clause
                        selectionArgs,          // The values for the WHERE clause
                        null,                   // don't group the rows
                        null,                   // don't filter by row groups
                        sortOrder               // The sort order
                );

                stopsJsonObject = new JSONObject();

                String stopJSON = "{\"stops\":[";

                while(cursor.moveToNext()) {
                    stopJSON = stopJSON + "{\"stopID\":" + cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID)) + ", \"stopName\":\"" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME)) + "\",\"arrivalDate\":\"" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL)) + "\",\"deptDate\":\"" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_DEPT)) + "\",\"stopDesc\":\"" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC)) + "\",\"lat\":\"" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_LAT)) + "\",\"long\":\"" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_LONG)) + "\"},";
                }
                cursor.close();

                stopJSON = stopJSON.substring(0, stopJSON.length() - 1);

                stopJSON = stopJSON + "]}";

                Log.i(TAG, stopJSON);

                StringBuilder responseStrBuilder = new StringBuilder();
                responseStrBuilder.append(stopJSON);

                try {
                    stopsJsonObject = new JSONObject(responseStrBuilder.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return stopsJsonObject;
            }
            else
            {*/
            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL(urlString + "/getallstops?tripid=" + tripIDForStops);
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
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return stopsJsonObject;
            }
            //}
        }

        @Override
        protected void onPostExecute(JSONObject result) {

            map.clear();

            List<LatLng> latlngs = new ArrayList<>();

            try {
                final JSONArray stopsJSON = stopsJsonObject.getJSONArray("stops");

                SQLiteDatabase db = mDBHelper.getWritableDatabase();

                ContentValues stopDetails = new ContentValues();

                for (int i = 0; i < stopsJSON.length(); i++) {
                    JSONObject stopsObj = stopsJSON.getJSONObject(i);

                    Double lat = Double.parseDouble(stopsObj.getString("lat"));
                    Double lng = Double.parseDouble(stopsObj.getString("long"));

                    String stopName = stopsObj.getString("stopName");
                    String stopID = stopsObj.getString("stopID");

                    Marker markerObj;

                    LatLng marker = new LatLng(lat, lng);
                    markerObj = map.addMarker(new MarkerOptions().position(marker).title(stopName));

                    latlngs.add(new LatLng(lat, lng));

                    markerObj.setTag(stopID);

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 4));


                    /*if(offlineMode) {
                        String[] projection = {DatabaseHelper.StopEntry.COLUMN_NAME_STOPID};

                        String selection = DatabaseHelper.StopEntry.COLUMN_NAME_STOPID + " = ?";
                        String[] selectionArgs = {String.valueOf(stopsObj.getInt("stopID"))};

                        Cursor cursor = db.query(
                                DatabaseHelper.StopEntry.TABLE_NAME,
                                projection,
                                selection,
                                selectionArgs,
                                null,
                                null,
                                null
                        );

                        long itemID = 0;

                        while (cursor.moveToNext()) {
                            itemID = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID));
                        }
                        cursor.close();

                        if (itemID > 0) {
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID, String.valueOf(stopsObj.getInt("stopID")));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID, stopsObj.getString("tripID"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_EMAIL, email);
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME, stopsObj.getString("stopName"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL, stopsObj.getString("arrivalDate"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_DEPT, stopsObj.getString("deptDate"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC, stopsObj.getString("stopDesc"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_LAT, stopsObj.getString("lat"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_LONG, stopsObj.getString("long"));

                            String stopUpdate = DatabaseHelper.StopEntry.COLUMN_NAME_STOPID + " LIKE ?";
                            String[] stopUpdateArgs = {String.valueOf(stopsObj.getInt("stopID"))};

                            db.update(
                                    DatabaseHelper.StopEntry.TABLE_NAME,
                                    stopDetails,
                                    stopUpdate,
                                    stopUpdateArgs);
                        } else {
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID, String.valueOf(stopsObj.getInt("stopID")));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID, stopsObj.getString("tripID"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_EMAIL, email);
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME, stopsObj.getString("stopName"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL, stopsObj.getString("arrivalDate"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_DEPT, stopsObj.getString("deptDate"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC, stopsObj.getString("stopDesc"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_LAT, stopsObj.getString("lat"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_LONG, stopsObj.getString("long"));

                            db.insert(DatabaseHelper.StopEntry.TABLE_NAME, null, stopDetails);
                        }
                    }*/
                }

                mDBHelper.close();

                map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        String ID = (String) (marker.getTag());
                        int IDint = Integer.parseInt(ID);

                        try {
                            final JSONArray stopJSON = stopsJsonObject.getJSONArray("stops");

                            for (int i = 0; i < stopsJSON.length(); i++) {

                                JSONObject stopObject = stopsJSON.getJSONObject(i);

                                if (stopObject.getString("stopID") == ID) {

                                    TextView stopName = findViewById(R.id.bottom_stop_name);
                                    TextView stopDesc = findViewById(R.id.bottom_stop_desc);
                                    TextView arrivalDate = findViewById(R.id.bottom_arrival_date);
                                    TextView deptDate = findViewById(R.id.bottom_dept_date);

                                    stopName.setText(stopObject.getString("stopName"));
                                    stopDesc.setText(stopObject.getString("stopDesc"));
                                    arrivalDate.setText(getString(R.string.arrivalLabelStart) + stopObject.getString("arrivalDate"));
                                    deptDate.setText(getString(R.string.deptLabelStart) + stopObject.getString("deptDate"));

                                    map.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));

                                    bottomSheetBehavior.setPeekHeight(120);

                                    currentStop = IDint;

                                    Button stopDelButton = findViewById(R.id.stopDelButton);
                                    stopDelButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            MainActivity.DeleteStop deleteStop = new MainActivity.DeleteStop();
                                            deleteStop.execute();
                                        }
                                    });

                                    Button stopEditButton = findViewById(R.id.stopEditButton);
                                    stopEditButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            height = 0;
                                            SwitchBoxes();
                                        }
                                    });

                                    Button stopSaveButton = findViewById(R.id.stopSaveButton);
                                    stopSaveButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            SwitchBoxes();
                                        }
                                    });
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                });

                map.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
                    @Override
                    public void onInfoWindowClose(Marker marker) {
                        bottomSheetBehavior.setPeekHeight(0);
                        Resources r = context.getResources();
                        int px = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                10,
                                r.getDisplayMetrics()
                        );

                        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                                CoordinatorLayout.LayoutParams.WRAP_CONTENT
                        );

                        params.gravity = BOTTOM | END;
                        params.bottomMargin = px;
                        params.rightMargin = px;
                        fab.setLayoutParams(params);
                    }
                });

                PolylineOptions polylineOptions = new PolylineOptions().addAll(latlngs);

                map.addPolyline(polylineOptions);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class UpdateStopDetails extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... details) {

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL(urlString + "/updatestopdetails?stopid=" + currentStop + "&arrdate=" + details[0] + "&deptdate=" + details[1] + "&name=" + details[2] + "&desc=" + details[3]);
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                urlConnection.disconnect();
                in.close();
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                MainActivity.GetAllStops getAllStops = new MainActivity.GetAllStops();
                getAllStops.execute();
                showSnackbar("Stop updated.");
            } else {
                showSnackbar("There was a problem, please try again.");
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DeleteTrip extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL(urlString + "/deltrip?tripid=" + currentTrip);
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                urlConnection.disconnect();
                in.close();
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {

            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
                getAllTrips.execute(email);
                showSnackbar("Your trip was deleted.");
            } else {
                showSnackbar("There was a problem, please try again.");
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class isConnectedToServer extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                URL myUrl = new URL(urlString + "");
                URLConnection connection = myUrl.openConnection();
                connection.setConnectTimeout(5000);
                connection.connect();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            offlineMode = result;

            MainActivity.GetAllTrips getAllTrips = new MainActivity.GetAllTrips();
            getAllTrips.execute(email);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            Integer ID = preferences.getInt("Current Trip", 0);

            if (ID > 0) {
                currentTrip = ID;
                tripIDForStops = String.valueOf(currentTrip);

                MainActivity.GetAllStops getAllStops = new MainActivity.GetAllStops();
                getAllStops.execute();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class UpdateStopLocation extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... details) {

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url;

                if (byCurrLoc) {
                    url = new URL(urlString + "/updatestoplocation?stopid=" + currentStop + "&lat=" + String.valueOf(mLastLocation.getLatitude()) + "&long=" + String.valueOf(mLastLocation.getLongitude()));
                } else {
                    url = new URL("http://10.0.2.2:3000/updatestoplocation?stopid=" + currentStop + "&lat=" + String.valueOf(addStopLat) + "&long=" + String.valueOf(addStopLong));
                }
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                urlConnection.disconnect();
                in.close();
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                MainActivity.GetAllStops getAllStops = new MainActivity.GetAllStops();
                getAllStops.execute();
                showSnackbar("Location updated.");
                updatingStop = false;
            } else {
                showSnackbar("There was a problem, please try again.");
                updatingStop = false;
            }
        }
    }
}

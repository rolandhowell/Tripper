package rh.tripper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

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

public class LandingActivity extends AppCompatActivity {

    String urlString = "http://ec2-18-221-155-124.us-east-2.compute.amazonaws.com:3000/";

    Button loginButton = null;
    TextView regPrompt = null;
    EditText emailBox, passwordBox;
    String email = null;
    String password = null;
    Boolean result = false;
    Context context = null;
    ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LandingActivity.this, MainActivity.class));
            finish();
        } else {
            setContentView(R.layout.activity_landing);

            loginButton = findViewById(R.id.loginButton);
            regPrompt = findViewById(R.id.regPrompt);
            progressBar = findViewById(R.id.loginProgress);

            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    emailBox = findViewById(R.id.emailBox);
                    passwordBox = findViewById(R.id.passwordBox);

                    email = emailBox.getText().toString();
                    password = passwordBox.getText().toString();

                    if (TextUtils.isEmpty(email)) {
                        emailBox.setError("Email is required.");
                        return;
                    }

                    if (TextUtils.isEmpty(password)) {
                        passwordBox.setError("Password is required.");
                        return;
                    }

                    progressBar.setVisibility(View.VISIBLE);

                    //authenticate user
                    auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LandingActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    // If sign in fails, display a message to the user. If sign in succeeds
                                    // the auth state listener will be notified and logic to handle the
                                    // signed in user can be handled in the listener.
                                    progressBar.setVisibility(View.GONE);
                                    if (!task.isSuccessful()) {
                                        // there was an error
                                        if (password.length() < 6) {
                                            passwordBox.setError("Password needs to be at least 6 characters.");
                                        } else {
                                            Toast.makeText(LandingActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        Intent intent = new Intent(LandingActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                            });
                }
            });

            regPrompt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(context, RegisterActivity.class);
                    startActivityForResult(i, 1);
                }
            });
        }

    }

    @SuppressLint("StaticFieldLeak")
    private class LogUserIn extends AsyncTask<String, Void, Boolean> {
        ProgressDialog progressDialog = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loginButton.setEnabled(false);
            regPrompt.setEnabled(false);

            progressDialog = new ProgressDialog(LandingActivity.this, R.style.Theme_AppCompat_DayNight_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Logging in...");
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... passwordIn) {

            HttpURLConnection urlConnection;
            InputStream in;

            try{
                URL url = new URL(urlString + "login?email=" + email + "&password=" + password);
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

        @Override
        protected void onPostExecute(Boolean resultB) {

            progressDialog.dismiss();

            if (result) {
                LandingActivity.FirstSync firstSync = new LandingActivity.FirstSync();
                firstSync.execute();
            } else {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                alertBuilder.setMessage("The email or password you entered was incorrect.");
                alertBuilder.setCancelable(true);

                alertBuilder.setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog logInAlert = alertBuilder.create();
                logInAlert.show();

                loginButton.setEnabled(true);
                regPrompt.setEnabled(true);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FirstSync extends AsyncTask<Void, Void, Boolean> {

        ProgressDialog progressDialog = null;
        private String urlString = "http://ec2-18-221-155-124.us-east-2.compute.amazonaws.com:3000";
        private JSONObject tripsJsonObject;
        private JSONObject stopsJsonObject;
        private DatabaseHelper mDBHelper;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loginButton.setEnabled(false);
            regPrompt.setEnabled(false);

            progressDialog = new ProgressDialog(LandingActivity.this, R.style.Theme_AppCompat_DayNight_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Syncing...");
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... strings) {
            HttpURLConnection urlConnection;
            InputStream in;

            mDBHelper = new DatabaseHelper(context);
            SQLiteDatabase db = mDBHelper.getWritableDatabase();

            String[] tripProjection = {
                    DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID,
                    DatabaseHelper.TripEntry.COLUMN_NAME_TRIPNAME,
                    DatabaseHelper.TripEntry.COLUMN_NAME_STARTDATE
            };

            String tripSelection = DatabaseHelper.TripEntry.COLUMN_NAME_LOCAL + " = ?";
            String[] tripSelectionArgs = {"true"};

            Cursor tripCursor = db.query(
                    DatabaseHelper.TripEntry.TABLE_NAME,   // The table to query
                    tripProjection,             // The array of columns to return (pass null to get all)
                    tripSelection,              // The columns for the WHERE clause
                    tripSelectionArgs,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    null               // The sort order
            );

            while (tripCursor.moveToNext()) {
                try {
                    URL url = new URL(urlString + "/sendtrip?tripid=" + tripCursor.getLong(tripCursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID)) + "&tripname=" + tripCursor.getString(tripCursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPNAME)) + "&startdate=" + tripCursor.getString(tripCursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_STARTDATE)) + "&email=" + email);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    Boolean result = Boolean.valueOf(reader.readLine());
                    urlConnection.disconnect();
                    in.close();

                    if (result) {
                        ContentValues updateLocal = new ContentValues();
                        updateLocal.put(DatabaseHelper.TripEntry.COLUMN_NAME_LOCAL, "false");

                        String tripUpdate = DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID + " = ?";
                        String[] tripUpdateArgs = {String.valueOf(tripCursor.getLong(tripCursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID)))};

                        db.update(
                                DatabaseHelper.TripEntry.TABLE_NAME,
                                updateLocal,
                                tripUpdate,
                                tripUpdateArgs);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            tripCursor.close();

            String[] stopProjection = {
                    DatabaseHelper.StopEntry.COLUMN_NAME_STOPID,
                    DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID,
                    DatabaseHelper.StopEntry.COLUMN_NAME_EMAIL,
                    DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL,
                    DatabaseHelper.StopEntry.COLUMN_NAME_DEPT,
                    DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME,
                    DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC,
                    DatabaseHelper.StopEntry.COLUMN_NAME_LAT,
                    DatabaseHelper.StopEntry.COLUMN_NAME_LONG
            };

            String stopSelection = DatabaseHelper.StopEntry.COLUMN_NAME_LOCAL + " = ?";
            String[] stopSelectionArgs = {"true"};

            Cursor stopCursor = db.query(
                    DatabaseHelper.StopEntry.TABLE_NAME,   // The table to query
                    stopProjection,             // The array of columns to return (pass null to get all)
                    stopSelection,              // The columns for the WHERE clause
                    stopSelectionArgs,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    null               // The sort order
            );

            while (stopCursor.moveToNext()) {
                try {
                    URL url = new URL(urlString + "/sendstop?stopid=" + stopCursor.getLong(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID)) + "&tripid=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID)) + "&email=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_EMAIL)) + "&arrivaldate=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL)) + "&deptdate=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_DEPT)) + "&stopname=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME)) + "&stopdesc=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC)) + "&lat=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_LAT)) + "&long=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_LONG)));
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    Boolean result = Boolean.valueOf(reader.readLine());
                    urlConnection.disconnect();
                    in.close();

                    if (result) {
                        ContentValues updateLocal = new ContentValues();
                        updateLocal.put(DatabaseHelper.StopEntry.COLUMN_NAME_LOCAL, "false");

                        String stopUpdate = DatabaseHelper.StopEntry.COLUMN_NAME_STOPID + " = ?";
                        String[] stopUpdateArgs = {String.valueOf(stopCursor.getLong(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID)))};

                        db.update(
                                DatabaseHelper.StopEntry.TABLE_NAME,
                                updateLocal,
                                stopUpdate,
                                stopUpdateArgs);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            stopCursor.close();

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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                URL url = new URL(urlString + "/getallstops2?email=" + email);
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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            db = mDBHelper.getWritableDatabase();
            ContentValues tripDetails = new ContentValues();

            try {
                JSONArray tripJSON = tripsJsonObject.getJSONArray("trips");
                for (int i = 0; i < tripJSON.length(); i++) {
                    JSONObject tripObj = tripJSON.getJSONObject(i);

                    String[] projection = {DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID, DatabaseHelper.TripEntry.COLUMN_NAME_LOCAL};

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

                    String local = "false";
                    long itemID = 0;

                    while (cursor.moveToNext()) {
                        local = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_LOCAL));
                        itemID = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID));
                    }
                    cursor.close();

                    if (local.equals("false")) {
                        if (itemID > 0) {
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID, String.valueOf(tripObj.getInt("tripID")));
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_EMAIL, email);
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPNAME, tripObj.getString("tripName"));
                            tripDetails.put(DatabaseHelper.TripEntry.COLUMN_NAME_STARTDATE, tripObj.getString("startDate"));

                            String tripUpdate = DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID + " = ?";
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
                    }

                    tripDetails.clear();
                }
                mDBHelper.close();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ContentValues stopDetails = new ContentValues();
            db = mDBHelper.getWritableDatabase();

            try {
                JSONArray stopJSON = stopsJsonObject.getJSONArray("stops");
                for (int i = 0; i < stopJSON.length(); i++) {
                    JSONObject stopObj = stopJSON.getJSONObject(i);

                    String[] projection = {DatabaseHelper.StopEntry.COLUMN_NAME_STOPID, DatabaseHelper.StopEntry.COLUMN_NAME_LOCAL};

                    String selection = DatabaseHelper.StopEntry.COLUMN_NAME_STOPID + " = ?";
                    String[] selectionArgs = {String.valueOf(stopObj.getInt("stopID"))};

                    Cursor cursor = db.query(
                            DatabaseHelper.StopEntry.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            null
                    );

                    String local = "false";
                    long itemID = 0;

                    while (cursor.moveToNext()) {
                        local = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_LOCAL));
                        itemID = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID));
                    }
                    cursor.close();

                    if (local.equals("false")) {
                        if (itemID > 0) {
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID, String.valueOf(stopObj.getInt("stopID")));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_EMAIL, email);
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID, stopObj.getInt("tripID"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL, stopObj.getString("arrivalDate"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_DEPT, stopObj.getString("deptDate"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME, stopObj.getString("stopName"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC, stopObj.getString("stopDesc"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_LAT, stopObj.getString("lat"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_LONG, stopObj.getString("long"));

                            String stopUpdate = DatabaseHelper.StopEntry.COLUMN_NAME_STOPID + " = ?";
                            String[] stopUpdateArgs = {String.valueOf(stopObj.getInt("stopID"))};

                            db.update(
                                    DatabaseHelper.StopEntry.TABLE_NAME,
                                    stopDetails,
                                    stopUpdate,
                                    stopUpdateArgs);
                        } else {
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID, String.valueOf(stopObj.getInt("stopID")));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_EMAIL, email);
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID, stopObj.getString("tripID"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL, stopObj.getString("arrivalDate"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_DEPT, stopObj.getString("deptDate"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME, stopObj.getString("stopName"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC, stopObj.getString("stopDesc"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_LAT, stopObj.getString("lat"));
                            stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_LONG, stopObj.getString("long"));

                            db.insert(DatabaseHelper.StopEntry.TABLE_NAME, null, stopDetails);
                        }
                    }

                    stopDetails.clear();
                }

                mDBHelper.close();

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressDialog.dismiss();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("User", email);
            editor.apply();

            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
        }
    }
}
package rh.tripper;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    String email;
    String urlString = "http://ec2-18-221-155-124.us-east-2.compute.amazonaws.com:3000";
    ContentResolver contentResolver;
    JSONObject tripsJsonObject;
    JSONObject stopsJsonObject;
    DatabaseHelper mDBHelper;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        email = preferences.getString("Email", "");

        mDBHelper = new DatabaseHelper(context);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            URL url = new URL(urlString + "/getallstops?email=" + email);
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

        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues tripDetails = new ContentValues();

        try {
            JSONArray tripJSON = tripsJsonObject.getJSONArray("trips");
            for (int i = 0; i < tripJSON.length(); i++) {
                JSONObject tripObj = tripJSON.getJSONObject(i);

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

                String local = null;
                long itemID = 0;

                while (cursor.moveToNext()) {
                    local = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_LOCAL));
                    itemID = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.TripEntry.COLUMN_NAME_TRIPID));
                }
                cursor.close();

                if (local == "false") {
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

                mDBHelper.close();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ContentValues stopDetails = new ContentValues();

        try {
            JSONArray stopJSON = stopsJsonObject.getJSONArray("stops");
            for (int i = 0; i < stopJSON.length(); i++) {
                JSONObject stopObj = stopJSON.getJSONObject(i);

                String[] projection = {DatabaseHelper.StopEntry.COLUMN_NAME_STOPID};

                String selection = DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID + " = ?";
                String[] selectionArgs = {String.valueOf(stopObj.getInt("tripID"))};

                Cursor cursor = db.query(
                        DatabaseHelper.StopEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null
                );

                String local = null;
                long itemID = 0;

                while (cursor.moveToNext()) {
                    local = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_LOCAL));
                    itemID = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID));
                }
                cursor.close();

                if (local == "false") {
                    if (itemID > 0) {
                        stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID, String.valueOf(stopObj.getInt("stopID")));
                        stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_EMAIL, email);
                        stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID, String.valueOf(stopObj.getInt("tripID")));
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
                        stopDetails.put(DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID, String.valueOf(stopObj.getInt("tripID")));
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

                mDBHelper.close();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
                urlConnection.disconnect();
                in.close();
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
                URL url = new URL(urlString + "/sendstop?stopid=" + stopCursor.getLong(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPID)) +
                        "&tripid=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_TRIPID)) +
                        "&email=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_EMAIL)) +
                        "&arrivaldate=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_ARRIVAL)) +
                        "&deptdate=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_DEPT)) +
                        "&stopname=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPNAME)) +
                        "&stopdesc=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_STOPDESC)) +
                        "&lat=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_LAT)) +
                        "&long=" + stopCursor.getString(stopCursor.getColumnIndexOrThrow(DatabaseHelper.StopEntry.COLUMN_NAME_LONG)));
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                urlConnection.disconnect();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        stopCursor.close();
    }
}

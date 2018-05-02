package rh.tripper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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

public class UpdateProfileActivity extends AppCompatActivity {

    Context context;
    int errorCode = 0;
    String name;
    String city;
    String country;
    String dob;
    String email;
    String currPassword;
    String newPassword;
    EditText nameBox;
    EditText cityBox;
    EditText countryBox;
    EditText dobBox;
    JSONObject user;
    private Boolean result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);
        context = this;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        email = preferences.getString("User", null);

        nameBox = findViewById(R.id.updateNameBox);
        cityBox = findViewById(R.id.updateCityBox);
        countryBox = findViewById(R.id.updateCountryBox);
        dobBox = findViewById(R.id.updateDobBox);

        UpdateProfileActivity.GetProfileDetails getProfileDetails = new UpdateProfileActivity.GetProfileDetails();
        getProfileDetails.execute();

        Button updateButton = findViewById(R.id.updateProfileButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = nameBox.getText().toString();
                city = cityBox.getText().toString();
                country = countryBox.getText().toString();
                dob = dobBox.getText().toString();

                UpdateProfileActivity.UpdateProfile updateProfile = new UpdateProfileActivity.UpdateProfile();
                updateProfile.execute();
            }
        });

        Button cancelButton = findViewById(R.id.updateProfileCancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, MainActivity.class);
                startActivity(i);
            }
        });

        Button deleteButton = findViewById(R.id.updateProfileDeleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Are you sure?")
                        .setCancelable(true)
                        .setPositiveButton("Yes, delete my profile", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                UpdateProfileActivity.DeleteProfile deleteProfile = new UpdateProfileActivity.DeleteProfile();
                                deleteProfile.execute();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });

        Button changePasswordButton = findViewById(R.id.updatePasswordButton);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View changePasswordDialog = View.inflate(context, R.layout.change_password_dialog, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Change Password")
                        .setView(changePasswordDialog)
                        .setCancelable(false)
                        .setPositiveButton("Change", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                EditText currPasswordBox = changePasswordDialog.findViewById(R.id.currentPasswordBox);
                                EditText newPasswordBox = changePasswordDialog.findViewById(R.id.newPasswordBox);

                                currPassword = currPasswordBox.getText().toString();
                                newPassword = newPasswordBox.getText().toString();

                                UpdateProfileActivity.ChangePassword changePassword = new UpdateProfileActivity.ChangePassword();
                                changePassword.execute();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });
    }

    private void showSnackbar(final String text) {
        View container = findViewById(R.id.main_layout);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showSnackbar(final String mainTextString, final String actionString, View.OnClickListener listener) {
        Snackbar.make(findViewById(R.id.main_layout), mainTextString, Snackbar.LENGTH_INDEFINITE).setAction(actionString, listener).show();
    }

    @SuppressLint("StaticFieldLeak")
    private class GetProfileDetails extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL("http://10.0.2.2:3000/getprofiledetails?email=" + email);
                urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                user = new JSONObject(responseStrBuilder.toString());

                return user;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject user) {
            if (user != null) {
                try {
                    JSONArray userJSON = user.getJSONArray("user");

                    JSONObject userObj = userJSON.getJSONObject(0);

                    name = userObj.getString("name");
                    city = userObj.getString("city");
                    country = userObj.getString("country");
                    dob = userObj.getString("dob");

                    nameBox.setText(name);
                    cityBox.setText(city);
                    countryBox.setText(country);
                    dobBox.setText(dob);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                showSnackbar("There was a problem. Please try again.");
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class UpdateProfile extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL("http://10.0.2.2:3000/upprofile?email=" + email + "&name=" + name + "&city=" + city + "&country=" + country + "&dob=" + dob);
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
        protected void onPostExecute(Boolean result) {

            if (result) {
                showSnackbar("Profile updated");
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            } else {
                showSnackbar("There was a problem", "Retry", new RetryListener());
            }
        }
    }

    private class ChangePassword extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            HttpURLConnection urlConnection = null;
            InputStream in = null;

            if (currPassword.length() == 0 || newPassword.length() == 0) {
                errorCode = 1;
                return false;
            } else {
                try {
                    URL url = new URL("http://10.0.2.2:3000/chgpw?email=" + email + "&currpassword=" + currPassword + "&newpassword=" + newPassword);
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
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result) {
                showSnackbar("Your password was changed");
            } else {
                if (errorCode == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("You didn't fill in all of the fields.")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else {
                    showSnackbar("There was a problem, please try again");
                }

            }
        }
    }

    private class DeleteProfile extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            HttpURLConnection urlConnection;
            InputStream in;

            try {
                URL url = new URL("http://10.0.2.2:3000/delprofile?email=" + email);
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
        protected void onPostExecute(Boolean result) {

            if (result) {
                showSnackbar("Profile updated");
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            } else {
                showSnackbar("There was a problem", "Retry", new RetryListener());
            }
        }
    }

    public class RetryListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            UpdateProfileActivity.UpdateProfile updateProfile = new UpdateProfileActivity.UpdateProfile();
            updateProfile.execute();
        }
    }
}

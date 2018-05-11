package rh.tripper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    String urlString = "http://ec2-18-221-155-124.us-east-2.compute.amazonaws.com:3000/";

    Calendar dobCalendar = null;
    EditText dobBox, nameBox, emailBox, passwordBox, cityBox, countryBox;
    Button regButton = null;
    Context context = null;
    View viewVar = null;
    ProgressBar progressBar;
    CheckBox termsandcons;

    String dob, name, email, password, city, country;
    Boolean tandcs;
    boolean result = false;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        context = this;
        auth = FirebaseAuth.getInstance();

        dobCalendar = Calendar.getInstance();
        dobBox = findViewById(R.id.dobBox);
        regButton = findViewById(R.id.registerButtonNew);
        progressBar = findViewById(R.id.registerProgress);

        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                dobCalendar.set(Calendar.YEAR, year);
                dobCalendar.set(Calendar.MONTH, monthOfYear);
                dobCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel();
            }
        };

        dobBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(RegisterActivity.this, date, dobCalendar.get(Calendar.YEAR), dobCalendar.get(Calendar.MONTH), dobCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        regButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nameBox = findViewById(R.id.nameBoxNew);
                emailBox = findViewById(R.id.emailBoxNew);
                passwordBox = findViewById(R.id.passwordBoxNew);
                cityBox = findViewById(R.id.cityBoxNew);
                countryBox = findViewById(R.id.countryBoxNew);
                dobBox = findViewById(R.id.dobBox);
                termsandcons = findViewById(R.id.termsandcons);

                name = nameBox.getText().toString();
                email = emailBox.getText().toString();
                password = passwordBox.getText().toString();
                city = cityBox.getText().toString();
                country = countryBox.getText().toString();
                dob = dobBox.getText().toString();

                if (TextUtils.isEmpty(name))
                {
                    nameBox.setError("Name is required");
                    return;
                }

                if (TextUtils.isEmpty(email))
                {
                    emailBox.setError("Email is required");
                    return;
                }

                if (TextUtils.isEmpty(password))
                {
                    passwordBox.setError("Name is required");
                    return;
                }

                if (!termsandcons.isChecked()) {
                    termsandcons.setError("Required");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                //create user
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this, "Authentication failed." + task.getException(),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    RegisterActivity.CreateUser createUser = new RegisterActivity.CreateUser();
                                    createUser.execute();
                                }
                            }
                        });
            }
        }));
    }

    private void updateLabel() {
        String myFormat = "dd/mm/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.ENGLISH);

        dobBox.setText(sdf.format(dobCalendar.getTime()));
    }

    @SuppressLint("StaticFieldLeak")
    private class CreateUser extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... emailIn) {

            HttpURLConnection urlConnection;
            InputStream in;

            try{
                URL url = new URL(urlString + "reguser?email=" + email + "&name=" + name + "&city=" + city + "&country=" + country + "&dob=" + dob);
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

            if (result) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("User", email);
                editor.apply();

                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            } else {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                alertBuilder.setMessage("An account already exists for that email. Would you like to log in instead?");
                alertBuilder.setCancelable(true);

                alertBuilder.setPositiveButton(
                        "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                Intent i = new Intent(viewVar.getContext(), LandingActivity.class);
                                startActivityForResult(i, 1);
                            }
                        });

                alertBuilder.setNegativeButton(
                        "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                emailBox.setText("");
                                passwordBox.setText("");
                            }
                        });

                AlertDialog registerAlert = alertBuilder.create();
                registerAlert.show();
            }
        }
    }
}

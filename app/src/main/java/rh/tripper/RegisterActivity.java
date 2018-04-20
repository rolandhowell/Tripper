package rh.tripper;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

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
    EditText dobBox = null;
    EditText nameBox = null;
    EditText emailBox = null;
    EditText passwordBox = null;
    EditText cityBox = null;
    EditText countryBox = null;
    Button regButton = null;
    Context context = null;
    View viewVar = null;

    int count = 0;

    String dob = null;
    String name = null;
    String email = null;
    String password = null;
    String city = null;
    String country = null;
    boolean result = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        context = this;

        dobCalendar = Calendar.getInstance();
        dobBox = findViewById(R.id.dobBox);
        regButton = findViewById(R.id.registerButtonNew);

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

                name = nameBox.getText().toString();
                email = emailBox.getText().toString();
                password = passwordBox.getText().toString();
                city = cityBox.getText().toString();
                country = countryBox.getText().toString();
                dob = dobBox.getText().toString();

                if(name.length() == 0)
                {
                    nameBox.setError("Name is required");
                    count++;
                }

                if(email.length() == 0)
                {
                    emailBox.setError("Email is required");
                    count++;
                }

                if(password.length() == 0)
                {
                    passwordBox.setError("Name is required");
                    count++;
                }

                if(count == 0)
                {
                    viewVar = view;
                    CreateUser logIn = new CreateUser();
                    logIn.execute(email);
                }
            }
        }));
    }

    private void updateLabel() {
        String myFormat = "dd/mm/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.ENGLISH);

        dobBox.setText(sdf.format(dobCalendar.getTime()));
    }

    private class CreateUser extends AsyncTask<String, Void, Boolean> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... emailIn) {

            HttpURLConnection urlConnection = null;
            InputStream in = null;

            try{
                URL url = new URL(urlString + "reguser?email=" + email + "&password=" + password + "&name=" + name + "&city=" + city + "&country=" + country + "&dob=" + dob);
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
        protected void onPostExecute(Boolean resultB) {

            if (result == true)
            {
                Intent i = new Intent(viewVar.getContext(), MainActivity.class);
                i.putExtra("email", email);
                startActivityForResult(i, 1);
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

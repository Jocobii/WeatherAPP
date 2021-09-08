package com.example.myweatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    FusedLocationProviderClient fusedLocationProviderClient;
    String cityKey = "";
    int REQUEST_LOCATION = 88;
    String apikey = "MG2NAQyrB87rbcrBFCjY3T4If29jNYFc";
    String coordinates = "";
    TextView txtUser, txtTitle, txtBody,txtWeather,tv_latitude, tv_longitude;
    Button  btnlocation;

    private static final String TAG = MainActivity.class.getSimpleName();
    private ArrayList<Weather> weatherArrayList = new ArrayList<>();
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.idListView);
        //Verificamos si los servicios estan activos
        checkPermissions();

        //Asignacion de variables
        txtUser = findViewById(R.id.txtUser);
        txtTitle = findViewById(R.id.txtTitle);
        txtBody = findViewById(R.id.txtBody);
        tv_latitude = findViewById(R.id.tv_latitude);
        tv_longitude = findViewById(R.id.tv_longitude);
        btnlocation = findViewById(R.id.btnlocation);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        btnlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocationManager locationManager = (LocationManager) getSystemService(
                        Context.LOCATION_SERVICE
                );
                if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                    getCurrentLocation();

                }else{
                    checkPermissions();
                }

            }
        });


    }

    private class FetchWeatherDetails extends AsyncTask<URL, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(URL... urls) {
            URL weatherUrl = urls[0];
            String weatherSearchResults = null;

            try {
                weatherSearchResults = NetworkUtils.getResponseFromHttpUrl(weatherUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "doInBackground: weatherSearchResults: " + weatherSearchResults);
            return weatherSearchResults;
        }

        @Override
        protected void onPostExecute(String weatherSearchResults) {
            if(weatherSearchResults != null && !weatherSearchResults.equals("")) {
                weatherArrayList = parseJSON(weatherSearchResults);
                //Just for testing
                Iterator itr = weatherArrayList.iterator();
                while(itr.hasNext()) {
                    Weather weatherInIterator = (Weather) itr.next();
                    Log.i(TAG, "onPostExecute: Date: " + weatherInIterator.getDate()+
                            " Min: " + weatherInIterator.getMinTemp() +
                            " Max: " + weatherInIterator.getMaxTemp() +
                            " Link: " + weatherInIterator.getLink());
                }
            }
            super.onPostExecute(weatherSearchResults);
        }
    }

    private ArrayList<Weather> parseJSON(String weatherSearchResults) {
        if(weatherArrayList != null) {
            weatherArrayList.clear();
        }

        if(weatherSearchResults != null) {
            try {
                JSONObject rootObject = new JSONObject(weatherSearchResults);
                JSONArray results = rootObject.getJSONArray("DailyForecasts");

                for (int i = 0; i < results.length(); i++) {
                    Weather weather = new Weather();

                    JSONObject resultsObj = results.getJSONObject(i);

                    Integer unixSeconds = resultsObj.getInt("EpochDate");
                    String datesString = unixSeconds.toString();
                    try {
                        // convert seconds to milliseconds
                        Date date = new java.util.Date(unixSeconds*1000L);
                        // the format of your date
                        SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                "EEEE d 'de' MMMM 'de' yyyy", new Locale("ES", "MX"));
                        // give a timezone reference for formatting (see comment at the bottom)
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-4"));
                        String formattedDate = sdf.format(date);
                        weather.setDate(formattedDate);
                        Log.e("fecha:", formattedDate);
                        Log.e("fecha number:", datesString);
                        Toast.makeText(MainActivity.this, "Hora: " + formattedDate, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    JSONObject temperatureObj = resultsObj.getJSONObject("Temperature");
                    String minTemperature = temperatureObj.getJSONObject("Minimum").getString("Value")+"° C";
                    weather.setMinTemp(minTemperature);

                    String maxTemperature = temperatureObj.getJSONObject("Maximum").getString("Value")+"° C";
                    weather.setMaxTemp(maxTemperature);

                    String link = resultsObj.getString("Link");
                    weather.setLink(link);

                   /* Log.i(TAG, "parseJSON: date: " + date + " " +
                            "Min: " + minTemperature + " " +
                            "Max: " + maxTemperature + " " +
                            "Link: " + link);*/

                    weatherArrayList.add(weather);
                }

                if(weatherArrayList != null) {
                    WeatherAdapter weatherAdapter = new WeatherAdapter(this, weatherArrayList);
                    listView.setAdapter(weatherAdapter);
                }

                return weatherArrayList;

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //Obtengo la localizacion actual
    @SuppressLint("MissingPermission")
    private void getCurrentLocation(){
        //inicializamos el manager de localizacion
        LocationManager locationManager = (LocationManager) getSystemService(
                Context.LOCATION_SERVICE
        );
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            //Si el servicio esta activo
            //Obtenemos la ultima locacion
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    //inicializamos la locacion
                    Location location = task.getResult();
                    if(location != null){
                        //Si no es nula la obtenemos
                        coordinates = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
                        getLocationWithCoordinates(coordinates);
                        tv_latitude.setText(String.valueOf(location.getLatitude()));
                        tv_longitude.setText(String.valueOf(location.getLongitude()));

                    }else{
                        //Si es nula inicilizamos una peticion
                        LocationRequest locationRequest = LocationRequest.create();
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        locationRequest.setInterval(10000);
                        locationRequest.setFastestInterval(1000);
                        locationRequest.setNumUpdates(1);

                        LocationCallback locationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                Location location1 = locationResult.getLastLocation();
                                //set latitude
                                tv_latitude.setText(String.valueOf(location1.getLatitude()));
                                //set longitude
                                tv_longitude.setText(String.valueOf(location1.getLongitude()));
                            }
                        };
                        //Request location update
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest
                                ,locationCallback
                                , Looper.myLooper());
                    }
                }
            });
        }else{
            //Cuando el servicio no esta activado
            checkPermissions();
        }
    }

    private void getLocationWithCoordinates(String coordenadas){
        String url = "http://dataservice.accuweather.com/locations/v1/cities/geoposition/search?apikey=" + apikey + "&q=" + coordenadas;
        StringRequest postRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    txtUser.setText(jsonObject.getString("Key"));
                    txtBody.setText(jsonObject.getString("Type"));
                    txtTitle.setText(jsonObject.getString("LocalizedName"));
                    cityKey = jsonObject.getString("Key");
                    Toast.makeText(MainActivity.this, "Coordenadas: " + coordinates, Toast.LENGTH_LONG).show();
                    String URLCITY = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/"+cityKey;
                    Toast.makeText(MainActivity.this, "URL: " + URLCITY, Toast.LENGTH_LONG).show();
                    NetworkUtils.setWeatherdbBaseUrl(URLCITY);

                    URL weatherUrl = NetworkUtils.buildUrlForWeather();
                    new FetchWeatherDetails().execute(weatherUrl);
                    Log.i(TAG, "onCreate: weatherUrl: " + weatherUrl);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Ha ocurrido un error: " + response, Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Ha ocurrido un error: " + error, Toast.LENGTH_LONG).show();
            }
        });
        Volley.newRequestQueue(this).add(postRequest);
    }

    //Da permisos a la APP y activa GPS
    private void checkPermissions() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(MainActivity.this, "Permisos Consedidos", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permisos Denegados\n" + deniedPermissions.toString(), Toast.LENGTH_LONG).show();
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("Si no aceptas los permisos,no podras utilizar la APP\n\nPor favor cambia los permisos en  [Setting] > [Permission]")
                .setPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .check();

        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(5000);
        request.setFastestInterval(200);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());
        result.addOnCompleteListener(task -> {
            try{
                LocationSettingsResponse response = task.getResult(ApiException.class);
                //

            } catch (ApiException e) {
                switch (e.getStatusCode()){
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try{
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException sendIntentException){

                        }
                        break;
                }
                e.printStackTrace();
            }
        });

    }
}
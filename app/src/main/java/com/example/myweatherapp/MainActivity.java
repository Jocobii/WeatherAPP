package com.example.myweatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    FusedLocationProviderClient fusedLocationProviderClient;
    int REQUEST_LOCATION = 88;
    String apikey = "FYVQjGG9RAX24cSMaf5v5tupY7tyjvXD";
    String coordinates = "";
    EditText txtUser, txtTitle, txtBody;
    TextView tv_latitude, tv_longitude;
    Button btnEnviar, btnlocation,button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Verificamos si los servicios estan activos
        checkPermissions();

        //Asignacion de variables
        txtUser = findViewById(R.id.txtUser);
        txtTitle = findViewById(R.id.txtTitle);
        txtBody = findViewById(R.id.txtBody);
        btnEnviar = findViewById(R.id.btnEnviar);
        tv_latitude = findViewById(R.id.tv_latitude);
        tv_longitude = findViewById(R.id.tv_longitude);
        btnlocation = findViewById(R.id.btnlocation);
        button = findViewById(R.id.location);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        btnlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCurrentLocation();
            }
        });


        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocationWithCoordinates(coordinates);
            }
        });

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
                        tv_latitude.setText(String.valueOf(location.getLatitude()));
                        tv_longitude.setText(String.valueOf(location.getLongitude()));
                        coordinates = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
                        getLocationWithCoordinates(coordinates);
                        Toast.makeText(MainActivity.this, "Coordenadas: " + coordinates, Toast.LENGTH_LONG).show();

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
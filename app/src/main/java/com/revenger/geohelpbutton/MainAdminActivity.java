package com.revenger.geohelpbutton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;

public class MainAdminActivity extends FragmentActivity implements OnMapReadyCallback {

    final String DELIMITER_GEO = "/", DELIMITER_LAT_LONG = " ";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private GoogleMap mMap;
    private DatabaseReference GeoInfoRef;
    private FirebaseAuth mAuth;
    private String curGeoStr;
    LatLng curLatLng;

    private TextView timeTextView;
    private Button logoutButton, clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        timeTextView = findViewById(R.id.timeTextView);
        logoutButton = findViewById(R.id.logoutBtn);
        clearButton = findViewById(R.id.clearBtn);

        mAuth = FirebaseAuth.getInstance();

        // ???????????????????? ???????? ?? ????
        GeoInfoRef = FirebaseDatabase.getInstance().getReference().child("GeoInfo");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // ???????????? ???????????????? ????????????????
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ?????????? ????????
                AlertDialog.Builder builder = new AlertDialog.Builder(MainAdminActivity.this);
                builder.setTitle("??????????????")
                        .setMessage("???? ?????????????????????????? ???????????? ???????????? ?????? ?????????????? ?? ???????????")
                        .setCancelable(false);
                builder.setNegativeButton("??????",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.setPositiveButton("????",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                GeoInfoRef.child("geoString").setValue("");
                                mMap.clear();
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        // ?????????? ???? ????????????????
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ?????????? ????????
                AlertDialog.Builder builder = new AlertDialog.Builder(MainAdminActivity.this);
                builder.setTitle("??????????")
                        .setMessage("???? ?????????????????????????? ???????????? ???????????")
                        .setCancelable(false);
                builder.setNegativeButton("??????",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.setPositiveButton("????",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mAuth.signOut();
                                SendUserToLoginActivity();
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        } else {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        // ?????????????????? ???????????????????? ???? ????????????????????????????
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED  ) {
            requestPermissions(new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // ???????????????? ??????????????
        GeoInfoRef.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // ?????????????????? ??????????
                    if (dataSnapshot.child("time").exists()) {
                        // ???????????? ?????? ???????????????????????? ??????????????????
                        String timeStr;
                        if (dataSnapshot.child("time").getValue().toString().charAt(0) == '??' || dataSnapshot.child("time").getValue().toString().charAt(0) == '??') {
                            timeStr = dataSnapshot.child("time").getValue().toString();
                        } else {
                            timeStr = "?????????? ???????????????? " + dataSnapshot.child("time").getValue().toString();
                        }
                        timeTextView.setText(timeStr);
                    }

                    // ?????????????????? ???????????? ?? ????????????????
                    if (dataSnapshot.child("geoString").exists()) {
                        curGeoStr = dataSnapshot.child("geoString").getValue().toString();

                        if (curGeoStr.length() != 0) {
                            mMap.clear();

                            if (curGeoStr.charAt(0) == '0') {
                                // ???????????? ??????????????
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(47.234033, 39.705533)));
                            } else {
                                // ?????????????????????? ???????????? ?????? ?? ???????????? [lat lng, ...]
                                ArrayList<String> geoList = new ArrayList<>(Arrays.asList(curGeoStr.split(DELIMITER_GEO)));
                                int quantityMarkers = geoList.size(), k = 0;

                                for (String tempGeo : geoList) {
                                    String[] latLong = tempGeo.split(DELIMITER_LAT_LONG);
                                    curLatLng = new LatLng(Double.parseDouble(latLong[0]), Double.parseDouble(latLong[1]));

                                    if (k == 0) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(curLatLng)
                                                .title("???????????? ????????????")
                                                .snippet(latLong[0] + ", " + latLong[1])
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                    } else if (k == quantityMarkers - 1) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(curLatLng)
                                                .title("?????????????????? ????????????")
                                                .snippet(latLong[0] + ", " + latLong[1])
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                    } else {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(curLatLng)
                                                .title("?????????????????????????? ????????????")
                                                .snippet(latLong[0] + ", " + latLong[1])
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                    }

                                    k++;
                                }

                                mMap.moveCamera(CameraUpdateFactory.newLatLng(curLatLng));
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // ???????????????? ???????????????????????? ?? ???????????????????? ??????????????????????
    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainAdminActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    // ???????????????????? ???????????? ????????????????????
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //getLocation();
                } else {
                    // Permission Denied
                    finish();
                    System.exit(0);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}

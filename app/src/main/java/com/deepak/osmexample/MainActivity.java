package com.deepak.osmexample;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.MapQuestRoadManager;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener {

    //private static final int EXTERNAL_PERMISSION_CODE = 24;

    //Minimum distance to update
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    //Minimum time between updates
    private static final long MIN_TIME_BW_UPDATES = 500;

    private static final String CURRENT_LOCATION = "Current Location";

    LocationManager locationManager;

    Marker markerForCurrentLocation;
    Button buttonToMoveMapToCurrentLoc;

    Context context;
    //private static final int REQUEST_FINE_LOCATION = 6;

    IMapController mapController;
    GeoPoint myPoint;
    MapView mapView;

    double startLat = 12.9532, startLng = 77.5835;

    AlertDialog dialog;

    GeoPoint destinationPoint;
    ArrayList<GeoPoint> arrayListForRoadPoints;
    Polyline polylineRoadOverlay;

    ProgressDialog progressDialog;

    Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button1 = (Button) findViewById(R.id.button1);
        buttonToMoveMapToCurrentLoc = (Button) findViewById(R.id.buttonToMoveMapTocurrentLoc);

        arrayListForRoadPoints = new ArrayList<>();

    }

    @Override
    protected void onResume() {
        super.onResume();
        showMap();
        //showCustomers();
        findNearestCustomer();

        buttonToMoveMapToCurrentLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.animateTo(myPoint);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showMap() {
        //important! set your user agent to prevent getting banned from the osm servers
        //org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);

        if (mapView == null) {
            mapView = (MapView) findViewById(R.id.map);

            mapView.setBuiltInZoomControls(false);
            mapView.setMultiTouchControls(true);

            myPoint = new GeoPoint(startLat, startLng);

            mapController = mapView.getController();
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapController.setZoom(15);
            mapController.setCenter(myPoint);

            markerForCurrentLocation = new Marker(mapView);
            markerForCurrentLocation.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
            markerForCurrentLocation.setIcon(getResources().getDrawable(R.drawable.blue, null));
            markerForCurrentLocation.setSnippet(CURRENT_LOCATION);

            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            Location currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (currentLocation != null) {
                myPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
            }
            markerForCurrentLocation.setPosition(myPoint);
            mapView.getOverlays().add(markerForCurrentLocation);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        myPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        markerForCurrentLocation.setPosition(myPoint);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("GPS Settings");
        alertDialog.setMessage("Please enable GPS in the settings");
        alertDialog.setPositiveButton("Go to GPS settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        dialog = alertDialog.create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
    }

    public void showMarkers(double lati, double lngi, String markerName) {
        //--- Create Overlay for marker

        GeoPoint startPoint = new GeoPoint(lati, lngi);
        //rename variable to something appropriate
        org.osmdroid.views.overlay.Marker startMarker = new org.osmdroid.views.overlay.Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        startMarker.setSnippet(markerName);
        startMarker.setIcon(ContextCompat.getDrawable(context, R.drawable.green));

        mapView.getOverlays().add(startMarker);
        arrayListForRoadPoints.add(startPoint);
    }

    public void findNearestCustomer() {
        if (mapView != null) {
            new ShowNavigationTask().execute();
        }
    }

    public class ShowNavigationTask extends AsyncTask<Void, Void, Void> {


        @Override
        protected void onPreExecute() {
            //super.onPreExecute();
            /*progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("YOUR NAVIGATION");
            progressDialog.setMessage("is downloading");
            progressDialog.show();
            if (mapView.getOverlays().contains(polylineRoadOverlay)) {
                mapView.getOverlays().remove(polylineRoadOverlay);
            }*/
        }

        //Road road;
        @Override
        protected Void doInBackground(Void... params) {
            int nearestPoint = 0; //distance between two points
            GeoPoint startPoint = myPoint;
            GeoPoint endPoint;
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            //RoadManager roadManager = new OSRMRoadManager(context);
            RoadManager roadManager = new MapQuestRoadManager("cuAt9LzDGexGlRhkOA7fbozqQGDcvncl");

            if (arrayListForRoadPoints.size() > 0) {

                for (int i = 0; i < (arrayListForRoadPoints.size()); i++) {

                    endPoint = arrayListForRoadPoints.get(i);
                    int distance = startPoint.distanceTo(endPoint);
                    if (nearestPoint == 0) {
                        nearestPoint = distance;
                        destinationPoint = endPoint;

                    } else if (distance < nearestPoint) {
                        nearestPoint = distance;
                        destinationPoint = endPoint;
                    }
                }
                waypoints.add(myPoint);
                waypoints.add(destinationPoint);

                roadManager.addRequestOption("routeType=fastest");

                //road = roadManager.getRoad(waypoints);
                polylineRoadOverlay = RoadManager.buildRoadOverlay(roadManager.getRoad(waypoints));
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {

            //super.onProgressUpdate(values);
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("YOUR NAVIGATION");
            progressDialog.setMessage("is downloading");
            progressDialog.isIndeterminate();
            progressDialog.show();

        }


        @Override
        protected void onPostExecute(Void aVoid) {
            //progressDialog.dismiss();
            if (arrayListForRoadPoints.size() > 0) {
                mapView.getOverlays().add(polylineRoadOverlay);
            }
            mapController.animateTo(myPoint);
        }
    }

}

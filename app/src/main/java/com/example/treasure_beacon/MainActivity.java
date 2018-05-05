package com.example.treasure_beacon;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

/*
BeaconConsumer is an interface for an Android Activity or Service that wants to interact with beacons.
The interface is used in conjunction with BeaconManager and provides a callback when the BeaconService is ready to use.
Until this callback is made, ranging and monitoring of beacons is not possible.
*/
public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    protected static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private BeaconManager beaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Checking needed permissions
        verifyBluetooth();
        // https://altbeacon.github.io/android-beacon-library/requesting_permission.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }

        /*
        BeaconManager is a class used to set up interaction with beacons from an Activity or Service.
        This class is used in conjunction with BeaconConsumer interface, which provides a callback when the BeaconService is ready to use.
        Until this callback is made, ranging and monitoring of beacons is not possible.
        */
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        //
        //beaconManager.getBeaconParsers().clear();
        //beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        // Binds the Android Activity to the BeaconService.
        beaconManager.bind(this);
    }

    // Checking needed permissions
    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }
            });
            builder.show();
        }
    }

    // Checking needed permissions
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbinds the Android Activity to the BeaconService
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this))
            /*
            setBackgroundMode method notifies the beacon service that the application is either moving to background mode or foreground mode.
            When in background mode, BluetoothLE scans to look for beacons are executed less frequently in order to save battery life.
             */
            beaconManager.setBackgroundMode(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this))
            /*
            setBackgroundMode method notifies the beacon service that the application is either moving to background mode or foreground mode.
            When in background mode, BluetoothLE scans to look for beacons are executed less frequently in order to save battery life.
             */
            beaconManager.setBackgroundMode(false);
    }

    @Override
    // Called when the beacon service is running and ready to accept your commands through the BeaconManager
    public void onBeaconServiceConnect() {
        // addRangeNotifier specifies a class that should be called each time the BeaconService gets ranging data, which is nominally once per second when beacons are detected.
        // RangeNotifier interface is implemented by classes that receive beacon ranging notifications
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            // didRangeBeaconsInRegion is called once per second to give an estimate of the mDistance to visible beacons
            // Beacon class represents a single hardware Beacon detected by an Android device.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Beacon myBeacon = beacons.iterator().next();
                    // getDistance() provides a calculated estimate of the distance to the beacon based on a running average of the RSSI and the transmitted power calibration value included in the beacon advertisement.
                    update_circle_progress(myBeacon.getDistance());
                    // Prints informations about the beacon detected, made visible at button click
                    logToDisplay(myBeacon.toString());
                    button_appears();
                }
            }

        });

        try {
            // Tells the BeaconService to start looking for beacons that match the passed Region object, and providing updates on the estimated mDistance every seconds while beacons in the Region are visible.
            /*
            Region class represents a criteria of fields used to match beacons.
            The uniqueId field is used to distinguish this Region in the system.
            When you set up monitoring or ranging based on a Region and later want to stop monitoring or ranging, you must do so by passing a Region object that has the same uniqueId field value.
            If it doesn't match, you can't cancel the operation. There is no other purpose to this field.
            */
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }

    private void update_circle_progress(double distance) {
        // Rounding with one decimal precision
        // round returns the closest long to the argument
        double rDistance = Math.round(distance*10)/10.0;
        // Assuming maximum distance is 10 m
        if(rDistance > 10.0)
            rDistance = 10.0;
        // Assuming minimum distance is 0.2 m
        else if(rDistance <= 0.2)
            rDistance = 0.0;
        // Percentage ditance with respect to the assumption 10m<-->100%
        int pDistance = (int) (10*rDistance);

        // Debugging
        //logToDisplay("distance = "+distance+"\n"+"rDistance = "+rDistance+"\n"+"pDistance = "+pDistance+"\n");

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.circle_progress_bar);
        // Reverse proportion
        progressBar.setProgress(100-pDistance);
    }

    private void button_appears() {
        // https://developer.android.com/training/multiple-threads/communicate-ui
        runOnUiThread(new Runnable() {
            public void run() {
                Button  button = (Button) findViewById(R.id.button);
                button.setVisibility(View.VISIBLE);
            }
        });
    }

    public void show_details(android.view.View view) {
        TextView monitor = (TextView) findViewById(R.id.tv_monitor);
        if(monitor.getVisibility() == View.VISIBLE)
            monitor.setVisibility(View.INVISIBLE);
        else
            monitor.setVisibility(View.VISIBLE);
    }

    private void logToDisplay(final String line) {
        // https://developer.android.com/training/multiple-threads/communicate-ui
        runOnUiThread(new Runnable() {
            public void run() {
                TextView monitor = (TextView) findViewById(R.id.tv_monitor);
                monitor.setText(line+"\n");
            }
        });
    }
}

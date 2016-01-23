package com.capellasolutions.recoverynetwork2;
/*
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import org.apache.log4j.Level;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;


public  class Configurator {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static void configure() {
        final LogConfigurator logConfigurator = new LogConfigurator();

        File dir = new File( Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS ) + "/logs" );
        if (!dir.exists())
            dir.mkdirs();

        logConfigurator.setFileName(dir.toString()+ "/rbox-rasheed.log");
        logConfigurator.setRootLevel(Level.DEBUG);
        // Set log level of a specific logger
        logConfigurator.setLevel("com.capellasolutions", Level.ALL);
        logConfigurator.configure();
    }
}
*/
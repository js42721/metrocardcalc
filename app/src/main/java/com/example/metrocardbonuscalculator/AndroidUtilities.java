package com.example.metrocardbonuscalculator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

public final class AndroidUtilities {
    private AndroidUtilities() {
    }

    /** Returns the value of the versionCode attribute. */
    public static int getVersionCode(Context context) {
        int versionCode = 0;
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        try {
            versionCode = pm.getPackageInfo(packageName, 0).versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e); // Should never happen.
        }
        return versionCode;
    }

    /** Returns true if an internet connection is available. */
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /** Returns true if external storage is readable and writable. */
    public static boolean isExtStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /** Returns true if external storage is readable. */
    public static boolean isExtStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
               Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}

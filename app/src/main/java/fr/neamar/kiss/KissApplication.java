package fr.neamar.kiss;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import fr.neamar.kiss.utils.IconPackCache;

public class KissApplication extends Application {
    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private final IconPackCache mIconPackCache = new IconPackCache();
    private volatile DataHandler dataHandler;
    private volatile RootHandler rootHandler;
    private volatile IconsHandler iconsPackHandler;

    public static KissApplication getApplication(Context context) {
        return (KissApplication) context.getApplicationContext();
    }

    public static IconPackCache iconPackCache(Context ctx) {
        return getApplication(ctx).mIconPackCache;
    }

    public DataHandler getDataHandler() {
        if (dataHandler == null) {
            synchronized (this) {
                if (dataHandler == null) {
                    dataHandler = new DataHandler(this);
                }
            }
        }
        return dataHandler;
    }

    public RootHandler getRootHandler() {
        if (rootHandler == null) {
            synchronized (this) {
                if (rootHandler == null) {
                    rootHandler = new RootHandler(this);
                }
            }
        }
        return rootHandler;
    }

    public void resetRootHandler(Context ctx) {
        rootHandler.resetRootHandler(ctx);
    }

    public void initDataHandler() {
        DataHandler dataHandler = getDataHandler();
        if (dataHandler != null && dataHandler.allProvidersHaveLoaded) {
            // Already loaded! We still need to fire the FULL_LOAD event
            Intent i = new Intent(MainActivity.FULL_LOAD_OVER);
            sendBroadcast(i);
        }
    }

    public IconsHandler getIconsHandler() {
        if (iconsPackHandler == null) {
            synchronized (this) {
                if (iconsPackHandler == null) {
                    iconsPackHandler = new IconsHandler(this);
                }
            }
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
    }

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     *
     * @param level the memory-related event that was raised.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // this is called every time the screen is off
            SQLiteDatabase.releaseMemory();
            mIconPackCache.clearCache(this);
        }
    }

    public String getBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return String.valueOf(batLevel);
    }

    public String getNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        StringBuilder result = new StringBuilder();

        boolean isWifiConnected = false;
        boolean isCellularConnected = false;
        String wifiName = null;
        String cellularOperatorName = null;

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android 6.0 (API level 23) and above
                android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);

                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        isWifiConnected = true;
                        // Get the Wi-Fi network name (SSID)
                        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null) {
                            wifiName = wifiInfo.getSSID();
                            // Remove quotes if SSID is enclosed
                            if (wifiName.startsWith("\"") && wifiName.endsWith("\"")) {
                                wifiName = wifiName.substring(1, wifiName.length() - 1);
                            }
                        }
                    }
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        isCellularConnected = true;
                        // Get cellular network operator name
                        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                        cellularOperatorName = telephonyManager.getNetworkOperatorName();
                    }
                }
            } else {
                // For devices below Android 6.0
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        isWifiConnected = true;
                        // Get Wi-Fi network name
                        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null) {
                            wifiName = wifiInfo.getSSID();
                            // Remove quotes if SSID is enclosed
                            if (wifiName.startsWith("\"") && wifiName.endsWith("\"")) {
                                wifiName = wifiName.substring(1, wifiName.length() - 1);
                            }
                        }
                    } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        isCellularConnected = true;
                        // Get cellular network operator name
                        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                        cellularOperatorName = telephonyManager.getNetworkOperatorName();
                    }
                }
            }

            // Build the result string
            if (isWifiConnected && wifiName != null) {
                result.append("Connected to Wi-Fi (").append(wifiName).append(")");
            }
            if (isCellularConnected && cellularOperatorName != null) {
                if (result.length() > 0) {
                    result.append(" and ");
                }
                result.append("Connected to Cellular Network (").append(cellularOperatorName).append(")");
            }
            if (!isWifiConnected && !isCellularConnected) {
                return "Not connected to any network";
            }

            return result.toString();
        }

        return "Unable to determine network status";

    }
}

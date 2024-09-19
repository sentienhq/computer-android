package fr.neamar.kiss.sentien;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.security.keystore.KeyProperties;
import android.util.Log;


import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.prefs.Preferences;

import javax.crypto.KeyGenerator;

import okhttp3.OkHttpClient;

public class AccountService {
    private static final String TAG = "\uD83C\uDFC4 AccountService";
    private static final String PREFS_NAME = "ComputerModule:Prefs";
    private static final String ACCOUNT_CREATED_PREF = "account_created";
    private static final String DEVICE_ALIAS_PREF = "device_alias";
    private static final String USER_ALIAS_PREF = "user_alias";
    private final Context context;
    //    private MinioClient minioClient;
    private ConnectivityManager connectivityManager;
    private NetworkCapabilities activeNetwork;
    private CryptoService cryptoService;
    private DataService dataService;
    private boolean connected = false;
    private boolean accountCreated;
    private String deviceAlias;
    private String userAlias;
    private String userId;
    private String deviceId;
    private String jwtToken;
    private String accountStatus = "not_created"; // not_created, awaiting_to_join, active

    AccountService(Context context, SharedPreferences prefs, CryptoService cryptoService, DataService dataService) {
        this.context = context;
        this.dataService = dataService;
        this.cryptoService = cryptoService;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        SharedPreferences prefsCM = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        accountCreated = prefsCM.getBoolean(ACCOUNT_CREATED_PREF, false);
        deviceAlias = prefsCM.getString(DEVICE_ALIAS_PREF, null);
        if (deviceAlias == null) {
            deviceAlias = getDeviceName();
            prefsCM.edit().putString(DEVICE_ALIAS_PREF, deviceAlias).apply();
        }
        userAlias = prefsCM.getString(USER_ALIAS_PREF, "user_unknown");
        updateNetworkStatus();
        if (connected) {
            boolean isInitialized = cryptoService.isInitialized();
            if (!isInitialized) {
                Log.i(TAG, "CryptoService is not initialized, carry on");
                return;
            }
            if (accountCreated) {
                Log.i(TAG, "Account already created, carry on");
                loginAccount(prefsCM);   // login!
                return;
            } else {
                Log.i(TAG, "Account not created, creating new one? Ask user to create account or join existing one");
                accountStatus = "not_created";
                // registerAccount(prefsCM);
            }
        }
    }

    public void registerAccount(SharedPreferences prefsCM) {
        Log.i(TAG, "Account not created, creating new one");
        // return [user_id, device_id, public_key, signature] in hex
        String[] registrationData = cryptoService.generateRegistrationData();
        AccountAPI.ApiResponse response = AccountAPI.registerAccount(registrationData[0], userAlias, registrationData[1], deviceAlias, registrationData[2], registrationData[3]);
        if (response != null && response.success && response.statusCode == 200) {
            accountCreated = true;
            prefsCM.edit().putBoolean(ACCOUNT_CREATED_PREF, true).apply();
            Log.i(TAG, "Account created" + response.body.toString());
        } else {
            if (response != null) {
                if (response.error.equals("CONNECTION_ERROR")) {
                    Log.i(TAG, "Account creation failed, connection error, server is down!");
                }
                if (response.statusCode == 409) {
                    Log.i(TAG, "Account creation failed, account already exists");
                    // try to login!
                    loginAccount(prefsCM);
                }
            } else {
                accountCreated = false;
                prefsCM.edit().putBoolean(ACCOUNT_CREATED_PREF, false).apply();
                Log.i(TAG, "Account creation failed and no response");
            }
        }
    }

    public void loginAccount(SharedPreferences prefsCM) {
        try {
            String rootHash = dataService.getRootHash();
            // return [user_id, device_id, signature] in hex
            String[] loginData = cryptoService.generateLoginData(rootHash);
            AccountAPI.ApiResponse response = AccountAPI.login(loginData[0], loginData[1], rootHash, loginData[2]);
            if (response != null && response.success && response.statusCode == 200) {
                Log.i(TAG, "Login successful");
                JSONObject jsonResponse = response.body;
                jwtToken = jsonResponse.getString("token");
                boolean isRootMatching = jsonResponse.getBoolean("isRootMatching");
                String rootDataBlob = jsonResponse.getString("rootData");
                prefsCM.edit().putBoolean(ACCOUNT_CREATED_PREF, true).apply();
                userId = loginData[0];
                deviceId = loginData[1];
                accountStatus = "active";
                Log.i(TAG, "Login response: " + response.body.toString());
                Log.i(TAG, "Login response code: " + response.statusCode);
                if (isRootMatching) {
                    Log.i(TAG, "Root hash matches, set timer for update");
                } else {
                    if (rootDataBlob.isEmpty()) {
                        Log.i(TAG, "Root hash does not match, but root data blob is empty, lets do update");
                        Log.i(TAG, "Empty root data blob: " + rootDataBlob);
                        // TODO: make update!
                        sendUpdate();
                    } else {
                        Log.i(TAG, "Root hash does not match, but root data blob is not empty, lets decrypt it");
                        // TODO: decrypt root data blob
                    }

                }
            } else {
                Log.i(TAG, "Login failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Login failed");
        }

    }

    public boolean checkForUpdates() {
        try {
            if (activeNetwork != null && activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                connected = isConnected();
                if (!connected) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendUpdate() {
        if (jwtToken == null || !accountCreated || !connected || userId == null || deviceId == null) {
            return;
        }
        try {
            String rootDataBlob = dataService.getRootBlob();
            String fileName = "file_" + dataService.getRootHash();
            AccountAPI.ApiResponse response = AccountAPI.uploadFile(userId, deviceId, jwtToken, rootDataBlob, fileName);
            Log.i(TAG, "Update response code: " + response.statusCode);
            if (response.success && response.statusCode == 200) {
                Log.i(TAG, "Update response: " + response.body.toString());
            }
            if (!response.success && response.statusCode == 409) {
                Log.i(TAG, "Update failed, update hash already exists");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateNetworkStatus() {
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            activeNetwork = connectivityManager.getNetworkCapabilities(network);
            connected = isConnected();
        } else {
            connected = false;
        }
    }

    public boolean isConnected() {
        return activeNetwork != null && (
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        );
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public String getUserId() {
        return userId;
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer + ":" + model;
        }
    }

}

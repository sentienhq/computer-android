package fr.neamar.kiss.sentien;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;

public class AccountService {
    private static final String TAG = "AccountService";
    private final Context context;
    //    private MinioClient minioClient;
    private ConnectivityManager connectivityManager;
    private NetworkCapabilities activeNetwork;
    private CryptoService cryptoService;
    private boolean connected = false;

    AccountService(Context context, SharedPreferences prefs, CryptoService cryptoService) {
        this.context = context;
        this.cryptoService = cryptoService;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        updateNetworkStatus();
        if (connected) {
            boolean isInitialized = cryptoService.isInitialized();
            if (!isInitialized) {
                Log.i(TAG, "CryptoService is not initialized, carry on");
                return;
            }
            cryptoService.generateRegistrationObject();
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

    public void sendUpdate(String updateType) {

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


}

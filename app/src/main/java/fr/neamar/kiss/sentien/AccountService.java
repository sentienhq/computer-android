package fr.neamar.kiss.sentien;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;

public class AccountService {
    private static final String usernameAlias = "sentien_username";
    private static final String passwordAlias = "sentien_password";
    private final Context context;
    private String username = "";
    private String password = "";
    private KeyStore keystore;
    //    private MinioClient minioClient;
    private ConnectivityManager mgr;
    private NetworkCapabilities activeNetwork;
    private boolean connected = false;

    AccountService(Context context, SharedPreferences prefs) {
        this.context = context;
        mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = mgr.getActiveNetwork();
        if (nw != null) {
            activeNetwork = mgr.getNetworkCapabilities(nw);
            connected = isConnected();

        }
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            boolean hasUsername = keystore.containsAlias(usernameAlias);
            boolean hasPassword = keystore.containsAlias(passwordAlias);
            if (!hasUsername || !hasPassword) {
                return;
            } else {
                try {
                    username = Arrays.toString(keystore.getKey(usernameAlias, null).getEncoded());
                    password = Arrays.toString(keystore.getKey(passwordAlias, null).getEncoded());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // keystore.load(null, null); // TODO: load keystore
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public boolean isConnected() {
        if (activeNetwork == null) {
            return false;
        }
        return (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }


}

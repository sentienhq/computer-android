package fr.neamar.kiss.sentien;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Arrays;

import fr.neamar.kiss.DataHandler;

//import io.minio.BucketExistsArgs;
//import io.minio.MakeBucketArgs;
//import io.minio.MinioClient;

public class AccountService {
    // TODO: S3 connector
//    private final String endpoint = "https://api.sentien.io/v1/accounts"; private final String accessKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMe"; private final String secretKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMe"; private final String bucketName = "kiss"; private final String region = "auto";
//    private final String accessKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMe";
//    private final String secretKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMe";
//    private final String bucketName = "kiss";
//    private final String region = "auto";
    private static final String usernameAlias = "sentien_username";
    private static final String passwordAlias = "sentien_password";
    private final Context context;
    private String username = "";
    private String password = "";
    private KeyStore keystore;
    private DataHandler dataHandler;
    //    private MinioClient minioClient;
    private ConnectivityManager mgr;
    private NetworkCapabilities activeNetwork;
    private boolean connected = false;

    AccountService(Context context, DataHandler dataHandler) {
        this.context = context;
        this.dataHandler = dataHandler;
        mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = mgr.getActiveNetwork();
        if (nw != null) {
            activeNetwork = mgr.getNetworkCapabilities(nw);
            connected = isConnected();

        }
        try {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
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
        } catch (KeyStoreException e) {
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

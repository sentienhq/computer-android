package fr.neamar.kiss.sentien;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import fr.neamar.kiss.DataHandler;

public class ComputerModule {
    public static final String MODULE_NAME = "Computer";
    public static final String MODULE_VERSION = "0.0.1";
    private static final String TAG = "ComputerModule";
    private boolean enabled = false;

    private ClipboardManager clipboardManager;

    private CryptoService cryptoService;
    private AccountService accountService;
    private DataService dataService;
    private ClipboardService clipboardService;


    public ComputerModule(Context context, DataHandler dataHandler, SharedPreferences prefs) {
        try {

            cryptoService = new CryptoService(context);
            dataService = new DataService(dataHandler);
            accountService = new AccountService(context, prefs, cryptoService, dataService);
            clipboardService = new ClipboardService(context, accountService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean setEnabled(boolean newlyEnabled) {
        enabled = newlyEnabled;
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void checkForUpdates() {
        Log.i(MODULE_NAME, "Checking for updates. Is enabled: " + isEnabled());
    }
}

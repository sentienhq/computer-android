package fr.neamar.kiss.sentien;

import android.util.Log;
import android.webkit.JavascriptInterface;

import fr.neamar.kiss.MainActivity;


public class WebAppInterface {
    private static final String TAG = "\uD83D\uDD12 WebAppInterface";
    private MainActivity mainActivityRef;
    private ComputerModule computerModuleRef;

    public WebAppInterface(MainActivity mainActivity, ComputerModule computerModule) {
        mainActivityRef = mainActivity;
        computerModuleRef = computerModule;


    }

    @JavascriptInterface
    public void closeApp() {
        Log.i("PRESSED!", "CLOSE PRESSED");
        mainActivityRef.runOnUiThread(() -> {
            mainActivityRef.closeCMWindow(); // Ensure this runs on the main thread
        });
    }
}

package fr.neamar.kiss.sentien;

import fr.neamar.kiss.MainActivity;


public class WebAppInterface {
    private static final String TAG = "\uD83D\uDD12 WebAppInterface";
    private MainActivity mainActivityRef;
    private ComputerModule computerModuleRef;

    public WebAppInterface(MainActivity mainActivity, ComputerModule computerModule) {
        mainActivityRef = mainActivity;
        computerModuleRef = computerModule;
    }
}

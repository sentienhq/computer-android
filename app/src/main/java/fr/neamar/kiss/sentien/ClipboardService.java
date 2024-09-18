package fr.neamar.kiss.sentien;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

public class ClipboardService {
    private static final String TAG = "ClipboardService";
    private static ClipboardManager clipboardManager;
    public static ClipboardManager.OnPrimaryClipChangedListener listener = () -> {

        String clipboardText = getClipboardText();
        Log.d("ClipboardService", "Clipboard changed" + clipboardText);
    };

    ClipboardService(Context context, AccountService accountService) {
        clipboardManager = ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE));
        clipboardManager.addPrimaryClipChangedListener(ClipboardService.listener);
    }

    public static String getClipboardText() {
        if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
            ClipData clip = clipboardManager.getPrimaryClip();
            if (clip != null) {
                ClipData.Item item = clip.getItemAt(0);
                if (item != null) {
                    Log.d("ClipboardService", "Clipboard content: " + item.getText());
                    return item.getText().toString();
                }
            }
        }
        return "";
    }

    public static void setClipboard(Context context, String text) {
        clipboardManager = ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE));
        ClipData clip = ClipData.newPlainText("ComputerModule", text);
        clipboardManager.setPrimaryClip(clip);
    }
}

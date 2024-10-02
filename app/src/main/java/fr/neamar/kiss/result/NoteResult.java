package fr.neamar.kiss.result;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.Layout;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.NotePojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.ClipboardUtils;
import fr.neamar.kiss.utils.FuzzyScore;
import fr.neamar.kiss.utils.TimeUtils;

public class NoteResult extends Result<NotePojo> {

    public NoteResult(NotePojo pojo) {
        super(pojo);
    }

    // Method to check if the TextView is ellipsized
    private static boolean isEllipsized(TextView textView) {
        Layout layout = textView.getLayout();
        if (layout != null) {
            int lineCount = layout.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                if (layout.getEllipsisCount(i) > 0) {
                    return true; // Ellipsized text found
                }
            }
        }
        return false; // No ellipsized text
    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_note, parent);
        TextView noteText = view.findViewById(R.id.item_note_text);
        TextView noteTimestamp = view.findViewById(R.id.item_note_time);
        String niceTime = TimeUtils.formatTimestamp(context, pojo.timestamp);
        noteTimestamp.setText(niceTime);
        noteText.setText(pojo.getContent());
        return view;
    }

    @Override
    public void doLaunch(Context context, View v) {
        TextView noteText = v.findViewById(R.id.item_note_text);
        if (noteText == null || context == null) {
            // Safety check to avoid crashing or closing the app
            return;
        }
        if (isEllipsized(noteText)) {
            noteText.setMaxLines(Integer.MAX_VALUE);
            noteText.setEllipsize(null);
        }
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        // todo implement this later
        // adapter.add(new ListPopup.Item(context, R.string.menu_note_edit));

        adapter.add(new ListPopup.Item(context, R.string.menu_note_copy));
        adapter.add(new ListPopup.Item(context, R.string.share));
        adapter.add(new ListPopup.Item(context, R.string.menu_note_remove));
        return inflatePopupMenu(adapter, context);
    }

    protected boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        // todo implement this later
        switch (stringId) {
            case R.string.menu_note_copy:
                ClipboardUtils.setClipboard(context, pojo.getContent());
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
                break;
            case R.string.share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, pojo.getContent());
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)));
                break;
            case R.string.menu_note_remove:
                DataHandler dh = KissApplication.getApplication(context).getDataHandler();
                dh.removeNote(pojo.id);
                break;
        }
        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }
}

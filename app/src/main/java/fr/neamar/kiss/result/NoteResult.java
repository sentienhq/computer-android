package fr.neamar.kiss.result;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.NotePojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.FuzzyScore;

public class NoteResult extends Result<NotePojo> {

    public NoteResult(NotePojo pojo) {
        super(pojo);
    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_note, parent);
        TextView noteText = view.findViewById(R.id.item_note_text);
        noteText.setText(pojo.getContent());
        return view;
    }

    @Override
    protected void doLaunch(Context context, View v) {

    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.menu_note_edit));
        adapter.add(new ListPopup.Item(context, R.string.menu_note_copy));
        adapter.add(new ListPopup.Item(context, R.string.share));
        adapter.add(new ListPopup.Item(context, R.string.menu_note_remove));
        return inflatePopupMenu(adapter, context);
    }
}

package fr.neamar.kiss.loader;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.db.DBHelper;

import java.util.List;

import fr.neamar.kiss.pojo.NotePojo;

public class LoadNotesPojos extends LoadPojos<NotePojo> {
    private static final String TAG = LoadNotesPojos.class.getSimpleName();

    public LoadNotesPojos(Context context) {
        super(context, NotePojo.SCHEME);
    }

    @Override
    protected List<NotePojo> doInBackground(Void... voids) {
        Context context = this.context.get();
        if (context == null) {
            return new ArrayList<>();
        }
        List<NotePojo> notes = DBHelper.getNotes(context);
        return notes;
    }
}

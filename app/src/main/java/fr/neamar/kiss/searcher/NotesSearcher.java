package fr.neamar.kiss.searcher;

import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.NotePojo;

public class NotesSearcher extends Searcher {
    public NotesSearcher(MainActivity activity, boolean isRefresh) {
        super(activity, "<note>", isRefresh);
    }

    @Override
    protected int getMaxResultCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null) {
            return null;
        }
        List<NotePojo> notes = KissApplication.getApplication(activity).getDataHandler().getAllNotes();
        if (notes != null) {
            this.addResults(notes);
        }
        return null;
    }
}

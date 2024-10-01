package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.loader.LoadNotesPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.NotePojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.FuzzyScore;

public class NotesProvider extends Provider<NotePojo> {
    private static final String TAG = NotesProvider.class.getSimpleName();

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadNotesPojos(this));
    }

//    @Override
//    public boolean mayFindById(String id) {
//        return id.startsWith(NotePojo.SCHEME);
//    }

    @Override
    public NotePojo findById(String id) {
        return null;
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        for (NotePojo pojo : getPojos()) {
            FuzzyScore.MatchInfo matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            boolean match = pojo.updateMatchingRelevance(matchInfo, false);
            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }

    public String generateUniqueId(String note) {
        return NotePojo.SCHEME + note.hashCode() + System.currentTimeMillis();
    }
}

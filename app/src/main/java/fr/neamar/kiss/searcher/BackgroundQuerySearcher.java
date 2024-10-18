package fr.neamar.kiss.searcher;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.pojo.Pojo;

public class BackgroundQuerySearcher extends Searcher {
    private final Context context;
    private final SearchResultsCallback callback;
    private final HashMap<String, Integer> knownIds;

    public BackgroundQuerySearcher(Context context, String query, SearchResultsCallback callback) {
        super(null, query, false); // Pass 'null' as MainActivity
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.knownIds = new HashMap<>();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // Collect previous results to boost relevance
        List<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(context, query);
        for (ValuedHistoryRecord id : lastIdsForQuery) {
            knownIds.put(id.record, id.value);
        }

        // Request results via "addResult"
        KissApplication.getApplication(context).getDataHandler().requestResults(query, this);

        return null;
    }

    @Override
    public boolean addResult(Pojo pojo) {
        if (isCancelled())
            return false;

        if (pojo.isDisabled()) {
            // Give penalty for disabled items
            pojo.relevance -= 200;
        } else {
            // Boost if item was previously selected for this query
            Integer value = knownIds.get(pojo.id);
            if (value != null) {
                pojo.relevance += 25 * value;
            }
        }

        return this.processedPojos.add(pojo);
    }

    @Override
    public boolean addResults(List<? extends Pojo> pojos) {
        if (isCancelled())
            return false;

        boolean added = false;
        for (Pojo pojo : pojos) {
            added |= addResult(pojo);
        }
        return added;
    }

    @Override
    protected void onPostExecute(Void param) {
        if (isCancelled()) {
            return;
        }

        // Collect the results
        List<Pojo> results = new ArrayList<>();
        while (!processedPojos.isEmpty()) {
            results.add(processedPojos.poll());
        }

        // Pass the results to the callback
        if (callback != null) {
            callback.onSearchResults(results);
        }
    }

    @Override
    protected void displayActivityLoader() {
        // No UI to update, so override and leave empty
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // No UI to update, so you can leave this empty or handle as needed
    }
}

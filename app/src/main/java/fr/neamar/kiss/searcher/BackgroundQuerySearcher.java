package fr.neamar.kiss.searcher;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.pojo.NotePojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.utils.FuzzyScore;

public class BackgroundQuerySearcher {
    private static final String TAG = "\uD83D\uDCAC BackgroundQuerySearcher";
    private final Context context;
    private final String capabilityType;
    private final String queryString;
    private final String[] queries;
    private final ArrayList<FuzzyScore> queriesNormalizedScores;
    private final HashMap<String, Integer> knownIds;

    public BackgroundQuerySearcher(Context context, String capabilityType, String queryString) {
        this.context = context;
        this.capabilityType = capabilityType;
        this.queryString = queryString;
        this.queries = queryString.contains(",") ? queryString.split(",") : new String[]{queryString};
        this.queriesNormalizedScores = new ArrayList<>();
        for (String query : queries) {
            StringNormalizer.Result sn = StringNormalizer.normalizeWithResult(query.trim(), false);
            queriesNormalizedScores.add(new FuzzyScore(sn.codePoints, true));
        }
        this.knownIds = new HashMap<>();
    }

    private <T extends Pojo> List<T> filterAndProcessResults(List<T> items) {
        return items.stream()
                .filter(pojo -> {
                    StringNormalizer.Result pojoNormalized = pojo.normalizedName != null ? pojo.normalizedName : StringNormalizer.normalizeWithResult(pojo.getName(), false);
                    return queriesNormalizedScores.stream().anyMatch(fuzzyScore -> {
                        FuzzyScore.MatchInfo matchInfo = fuzzyScore.match(pojoNormalized.codePoints);
                        if (matchInfo.match && matchInfo.score > 4) {
                            pojo.relevance += matchInfo.score;
                            adjustRelevance(pojo);
                            return true;
                        }
                        return false;
                    });
                })
                .sorted((p1, p2) -> Integer.compare(p2.relevance, p1.relevance))
                .limit(30)
                .collect(Collectors.toList());
    }

    public List<Pojo> search() {
        loadPreviousSelections();
        List<Pojo> results = new ArrayList<>();
        // Perform search based on capability type
        switch (capabilityType.toUpperCase()) {
            case "APPS":
                results.addAll(searchApps());
                break;
            case "CONTACTS":
                results.addAll(searchContacts());
                break;
            case "SHORTCUTS":
                results.addAll(searchShortcuts());
                break;
            case "NOTES":
                results.addAll(searchNotes());
                break;
            default:
                break;
        }
        return results;
    }

    public String searchGetResultString() {
        try {
            List<Pojo> results = this.search();
            JSONObject mainJsonObject = new JSONObject();
            mainJsonObject.put("capabilityType", this.capabilityType);

            // Create a JSONArray to hold each result JSON object
            JSONArray capabilityResults = new JSONArray();
            for (Pojo pojo : results) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", pojo.id);
                switch (this.capabilityType) {
                    case "APPS":
                        jsonObject.put("appName", ((AppPojo) pojo).getName());
                        break;
                    case "CONTACTS":
                        jsonObject.put("contactName", ((ContactsPojo) pojo).getName());
                        jsonObject.put("contactNickname", ((ContactsPojo) pojo).normalizedNickname);
                        jsonObject.put("contactPhone", ((ContactsPojo) pojo).phone);
                        jsonObject.put("contactIsPrimary", ((ContactsPojo) pojo).isHomeNumber());
                        List<Pojo> adjustedShortcuts = new BackgroundQuerySearcher(this.context, "SHORTCUTS", ": " + pojo.getName()).search();
                        if (adjustedShortcuts.size() > 0) {
                            JSONArray shortcutArray = new JSONArray();
                            for (Pojo shortcut : adjustedShortcuts) {
                                JSONObject shortcutJson = new JSONObject();
                                shortcutJson.put("shortcutPackageName", ((ShortcutPojo) shortcut).packageName);
                                shortcutJson.put("shortcutName", ((ShortcutPojo) shortcut).getName());
                                shortcutJson.put("shortcutIntentUri", ((ShortcutPojo) shortcut).intentUri);
                                shortcutArray.put(shortcutJson);
                            }
                            jsonObject.put("contactAdjustedShortcuts", shortcutArray);
                        }
                        break;
                    case "SHORTCUTS":
                        jsonObject.put("shortcutPackageName", ((ShortcutPojo) pojo).packageName);
                        jsonObject.put("shortcutName", ((ShortcutPojo) pojo).getName());
                        jsonObject.put("shortcutIntentUri", ((ShortcutPojo) pojo).intentUri);
                        break;
                    case "NOTES":
                        jsonObject.put("noteType", ((NotePojo) pojo).type.toString());
                        jsonObject.put("noteContent", ((NotePojo) pojo).content);
                        break;
                }
                jsonObject.put("relevance", pojo.relevance);
                capabilityResults.put(jsonObject); // Add each JSON object to the array
            }

            // Put the array into the main JSON object
            mainJsonObject.put("capabilityResults", capabilityResults);

            return mainJsonObject.toString(); // Return the JSON string
        } catch (JSONException e) {
            return "no results";
        }
    }

    private void loadPreviousSelections() {
        for (String query : queries) {
            DBHelper.getPreviousResultsForQuery(this.context, query).forEach(id -> knownIds.put(id.record, id.value));
        }
    }

    private List<AppPojo> searchApps() {
        return filterAndProcessResults(KissApplication.getApplication(this.context).getDataHandler().getApplications());
    }

    private List<ContactsPojo> searchContacts() {
        return filterAndProcessResults(KissApplication.getApplication(this.context).getDataHandler().getContacts());
    }

    private List<ShortcutPojo> searchShortcuts() {
        return filterAndProcessResults(KissApplication.getApplication(this.context).getDataHandler().getShortcuts());
    }

    private List<NotePojo> searchNotes() {
        return filterAndProcessResults(KissApplication.getApplication(this.context).getDataHandler().getAllNotes());
    }

    private void adjustRelevance(Pojo pojo) {
        if (pojo.isDisabled()) {
            pojo.relevance -= 200;
        } else {
            Integer value = knownIds.get(pojo.id);
            if (value != null) {
                pojo.relevance += 25 * value;
            }
        }
    }
}

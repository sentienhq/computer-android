package fr.neamar.kiss.sentien.llm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.pojo.NotePojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.utils.Permission;
import fr.neamar.kiss.utils.TimeUtils;
import okhttp3.*;

public class LLMService {
    private static final String TAG = "\uD83E\uDDE0 LLMService";
    private static final String LLM_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static String LLM_API_KEY = "";
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final ExecutorService executorService;
    private final LLMActions llmActions = new LLMActions();


    public LLMService(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("LLMServicePrefs", Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public static JSONObject makeApiCall(JSONArray messages) {
        try {
            // Build request body
            if (messages == null || messages.length() == 0) {
                return null;
            }
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-4o-mini"); // Specify the model you want to use
            requestBody.put("messages", messages);
            JSONObject responseFormat = new JSONObject();
            responseFormat.put("type", "json_object"); // Specify the format of the response
            requestBody.put("response_format", responseFormat);
            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
            Request request = new Request.Builder()
                    .url(LLM_API_URL)
                    .addHeader("Authorization", "Bearer " + LLM_API_KEY)
                    .post(body)
                    .build();
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                assert response.body() != null;
                String responseBody = response.body().string();
                return new JSONObject(responseBody);
            } else {
                Log.e(TAG, "API call failed with code: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during API call", e);
        }
        return null;
    }

    /**
     * Initializes the LLMService with a new API key.
     *
     * @param llmKey The API key for the LLM service.
     */
    public void init(String llmKey) {
        LLM_API_KEY = llmKey;
        // Optionally, you can store the API key securely if needed.
    }

    /**
     * Performs a task by sending a prompt to the LLM and returns the result via a callback.
     * This operation is executed in a separate thread to avoid blocking the main thread.
     *
     * @param prompt   The user's prompt to send to the LLM.
     * @param callback The callback to receive the LLM's response.
     */
    public void performTask(String prompt, LLMCallback callback) {
        executorService.submit(() -> {
            // Make API call
            String userPrompt = "{\"user_request\": \"" + prompt + "\"}";
            new LLMTask(userPrompt, callback, llmActions);
            // makeApiCall(", null); if (response != null) {
            ////                // Update conversation history
            ////                // updateConversationHistory(prompt, response);
            ////                // Return result to UI thread
            ////                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(response));
            ////            } else {
            ////                // Return error to UI thread
            ////                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error performing LLM task"));
            ////            }
//
        });
    }
//
//    private String getNotes() {
//        StringBuilder notes = new StringBuilder();
//        List<NotePojo> noteList = KissApplication.getApplication(context).getDataHandler().getAllNotes();
//        assert noteList != null;
//        for (NotePojo note : noteList) {
//            notes.append(note.getContent()).append("\n");
//        }
//        return notes.toString();
//    }
//
//    private String getContacts() {
//        StringBuilder contacts = new StringBuilder();
//        List<ContactsPojo> contactList = KissApplication.getApplication(context).getDataHandler().getContacts();
//        assert contactList != null;
//        for (ContactsPojo contact : contactList) {
//            contacts.append(contact.getName() + " - " + contact.phone + "\n");
//        }
//        return contacts.toString();
//    }
//
//    private String getAvailableShortcuts() {
//        StringBuilder availableShortcuts = new StringBuilder();
//        List<ShortcutPojo> shortcutList = KissApplication.getApplication(context).getDataHandler().getShortcuts();
//        assert shortcutList != null;
//        for (ShortcutPojo shortcut : shortcutList) {
//            availableShortcuts.append(shortcut.getName()).append("\n");
//        }
//        return availableShortcuts.toString();
//    }
//
//    private String getInstalledApps() {
//        StringBuilder installedApps = new StringBuilder();
//        List<AppPojo> appList = KissApplication.getApplication(context).getDataHandler().getApplications();
//        assert appList != null;
//        for (AppPojo app : appList) {
//            installedApps.append(app.getName()).append("\n");
//        }
//        return installedApps.toString();
//    }
//
//    private List<String> parseCapabilities(String capabilitiesJson) {
//        try {
//            JSONObject capabilities = new JSONObject(capabilitiesJson);
//            JSONArray capabilitiesArray = capabilities.getJSONArray("capabilities");
//            String subject = capabilities.getString("subject");
//            Log.d(TAG, "Subject: " + subject);
//            List<String> capabilitiesList = new ArrayList<>();
//            for (int i = 0; i < capabilitiesArray.length(); i++) {
//                capabilitiesList.add(capabilitiesArray.getString(i));
//            }
//            return capabilitiesList;
//        } catch (JSONException e) {
//            Log.e(TAG, "Error parsing capabilities and subject", e);
//            return null;
//        }
//    }


//    private String processOutput(JSONObject jsonResponseContent, JSONArray messages) {
//        try {
//            String mainGoal = jsonResponseContent.getString("main_goal");
//            // JSONArray constraints = jsonResponseContent.getJSONArray("constraints");
//            JSONArray previousActions = jsonResponseContent.getJSONArray("previous_actions");
//            JSONArray nextActions = jsonResponseContent.getJSONArray("next_actions");
//
//            boolean executionEnded = false;
//
//            while (!executionEnded) {
//                JSONArray newPreviousActions = new JSONArray();
//
//                for (int i = 0; i < nextActions.length(); i++) {
//                    JSONObject action = nextActions.getJSONObject(i);
//                    String actionName = action.getString("action_name");
//                    JSONObject params = action.getJSONObject("params");
//
//                    // Process the action and get the result
//                    String result = llmActions.processAction(actionName, params);
//
//                    // Add the action to previous_actions with the result
//                    JSONObject actionWithResult = new JSONObject();
//                    actionWithResult.put("action_name", actionName);
//                    actionWithResult.put("params", params);
//                    actionWithResult.put("result", result);
//                    newPreviousActions.put(actionWithResult);
//
//                    if (actionName.equals("EXECUTION_REEVALUATE")) {
//                        JSONObject newUserMessage = new JSONObject();
//                        previousActions = mergePreviousActions(previousActions, newPreviousActions);
//                        String newUserPrompt = "{\"main_goal\":\"" + mainGoal + "\",  \"previous_actions\":" + previousActions.toString() + "}";
//                        // Make a new API call with updated previous_actions
//                        String response = makeApiCall(newUserPrompt, messages);
//
//                        // Parse the new response
//                        if (response != null) {
//                            jsonResponseContent = new JSONObject(response);
//                            nextActions = jsonResponseContent.getJSONArray("next_actions");
//                            // Reset newPreviousActions for the next iteration
//                            newPreviousActions = new JSONArray();
//                            // Break the for-loop to start processing the new nextActions
//                            break;
//                        } else {
//                            throw new RuntimeException("Error during API call in EXECUTION_REEVALUATE");
//                        }
//                    } else if (actionName.equals("EXECUTION_END")) {
//                        executionEnded = true;
//                        // Optionally, handle any finalization here
//                        return result; // or return any final output you need
//                    } else {
//                        // Continue processing other actions
//                    }
//                }
//
//                // After processing all actions without EXECUTION_REEVALUATE or EXECUTION_END
//                if (!executionEnded) {
//                    // Merge newPreviousActions into previousActions
//                    previousActions = mergePreviousActions(previousActions, newPreviousActions);
//                    jsonResponseContent.put("previous_actions", previousActions);
//                    // Since there are no more actions, we can end the execution
//                    executionEnded = true;
//                }
//            }
//
//            // Return final result or any required output
//            return "Execution completed";
//
//        } catch (JSONException e) {
//            throw new RuntimeException(e);
//        }
//    }
//

//    private JSONArray getLongTermMemory() {
//        String historyJson = sharedPreferences.getString("long_term_memory", "[]");
//        try {
//            return new JSONArray(historyJson);
//        } catch (JSONException e) {
//            Log.e(TAG, "Failed to parse conversation history", e);
//        }
//        return new JSONArray();
//    }
//
//    private void updateLongTermMemory(String key, String value, String[] tags) {
//        JSONArray conversationHistory = getLongTermMemory();
//
//        try {
//
//
//            // Save updated conversation history
//            sharedPreferences.edit()
//                    .putString("long_term_memory", conversationHistory.toString())
//                    .apply();
//        } catch (JSONException e) {
//            Log.e(TAG, "Failed to update conversation history", e);
//        }
//    }

    // Helper method to merge previous actions
    private JSONArray mergePreviousActions(JSONArray previousActions, JSONArray newPreviousActions) throws JSONException {
        JSONArray mergedActions = new JSONArray();
        for (int i = 0; i < previousActions.length(); i++) {
            mergedActions.put(previousActions.getJSONObject(i));
        }
        for (int i = 0; i < newPreviousActions.length(); i++) {
            mergedActions.put(newPreviousActions.getJSONObject(i));
        }
        return mergedActions;
    }


    /**
     * Callback interface to receive results from the LLMService.
     */
    public interface LLMCallback {
        void onSuccess(String result);

        void onError(String error);

        void onUpdate(String result);
    }
}

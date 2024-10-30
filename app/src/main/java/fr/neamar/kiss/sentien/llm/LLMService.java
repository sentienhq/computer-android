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

                    String userPrompt = "{\"user_request\": \"" + prompt + "\"}";
                    new LLMTask(this.context, userPrompt, callback, llmActions);

// this is a test
//            try {
//                JSONObject userPrompt = new JSONObject();
//                userPrompt.put("capabilityQuery", prompt);
//                userPrompt.put("capabilityType", "CONTACTS");
//                this.llmActions.processAction(this.context, "GET_CAPABILITY", userPrompt);
//            } catch (JSONException e) {
//                throw new RuntimeException(e);
                }


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
        );
    }

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

    public interface ActionResult {
        boolean isSuccess();

        String getUserMessage();

        String getActionResultValues();
    }

    public static class ActionResultImpl implements ActionResult {
        private final boolean success;
        private final String userMessage;
        private final String actionResultValues;

        public ActionResultImpl(boolean success, String userMessage, String actionResultValues) {
            this.success = success;
            this.userMessage = userMessage;
            this.actionResultValues = actionResultValues;
        }

        @Override
        public boolean isSuccess() {
            return success;
        }

        @Override
        public String getUserMessage() {
            return userMessage;
        }

        @Override
        public String getActionResultValues() {
            return actionResultValues;
        }
    }

}

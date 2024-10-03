package fr.neamar.kiss.sentien;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.*;

public class LLMService {
    private static final String TAG = "LLMService";
    private static final String LLM_API_URL = "https://api.openai.com/v1/chat/completions";
    private static String LLM_API_KEY = "";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final ExecutorService executorService;
    private final OkHttpClient httpClient;

    public LLMService(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("LLMServicePrefs", Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();
        this.httpClient = new OkHttpClient();
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
            String response = makeApiCall(prompt);
            if (response != null) {
                // Update conversation history
                updateConversationHistory(prompt, response);
                // Return result to UI thread
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(response));
            } else {
                // Return error to UI thread
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error performing LLM task"));
            }
        });
    }

    private String makeApiCall(String prompt) {
        try {
            // Get conversation history
            JSONArray conversationHistory = getConversationHistory();

            // Build messages array
            JSONArray messages = new JSONArray();
            // Add previous messages
            for (int i = 0; i < conversationHistory.length(); i++) {
                messages.put(conversationHistory.getJSONObject(i));
            }
            // Add current prompt as user message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);

            // Build request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-4o-mini"); // Specify the model you want to use
            requestBody.put("messages", messages);

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
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    String assistantReply = message.getString("content");
                    return assistantReply;
                }
            } else {
                Log.e(TAG, "API call failed with code: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during API call", e);
        }
        return null;
    }

    private JSONArray getConversationHistory() {
        String historyJson = sharedPreferences.getString("conversation_history", "[]");
        try {
            return new JSONArray(historyJson);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse conversation history", e);
        }
        return new JSONArray();
    }

    private void updateConversationHistory(String prompt, String response) {
        JSONArray conversationHistory = getConversationHistory();

        try {
            // Add user message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            conversationHistory.put(userMessage);

            // Add assistant's reply
            JSONObject assistantMessage = new JSONObject();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", response);
            conversationHistory.put(assistantMessage);

            // Keep only the last 10 messages (20 entries considering user and assistant messages)
            if (conversationHistory.length() > 20) {
                JSONArray trimmedHistory = new JSONArray();
                for (int i = conversationHistory.length() - 20; i < conversationHistory.length(); i++) {
                    trimmedHistory.put(conversationHistory.getJSONObject(i));
                }
                conversationHistory = trimmedHistory;
            }

            // Save updated conversation history
            sharedPreferences.edit()
                    .putString("conversation_history", conversationHistory.toString())
                    .apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update conversation history", e);
        }
    }

    /**
     * Callback interface to receive results from the LLMService.
     */
    public interface LLMCallback {
        void onSuccess(String result);

        void onError(String error);
    }
}

package fr.neamar.kiss.sentien.llm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.pojo.NotePojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.utils.Permission;
import okhttp3.*;

public class LLMService {
    private static final String TAG = "\uD83E\uDDE0 LLMService";
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
            // Step 1: Analyze Intent
            String capabilitiesJson = analyzeIntent(prompt);
            if (capabilitiesJson == null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error analyzing intent"));
                return;
            }
            // Step 2: Parse required capabilities
            List<String> requiredCapabilities = parseCapabilities(capabilitiesJson);
            Log.d(TAG, "Required capabilities: " + requiredCapabilities);
            if (requiredCapabilities == null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error parsing capabilities"));
                return;
            }
            // Step 3: Fetch relevant data
            String systemPrompt = buildSystemPrompt(requiredCapabilities);
            Log.d(TAG, "System prompt: " + systemPrompt);
            // Make API call
            String response = makeApiCall(prompt, systemPrompt);
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

    private String buildSystemPrompt(List<String> requiredCapabilities) {
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an AI assistant with access to the following capabilities and data:\n\n");
        if (requiredCapabilities.contains("apps")) {
            systemPrompt.append("Installed Apps and Shortcuts:\n").append(getInstalledApps()).append("\n\n");
        }
        if (requiredCapabilities.contains("contacts")) {
            systemPrompt.append("Contacts:\n").append(getContacts()).append("\n\n");
        }
        if (requiredCapabilities.contains("notes")) {
            systemPrompt.append("User personal notes:\n").append(getNotes()).append("\n\n");
        }
        if (requiredCapabilities.contains("memory")) {
            systemPrompt.append("AI Long-term memory:\n").append(getMemory()).append("\n\n");
        }
        systemPrompt.append("Device current info:\n\n");
        // add phone time, date, timezone, battery, network, etc.
        systemPrompt.append("Phone time: ").append(System.currentTimeMillis()).append("\n");
        systemPrompt.append("Battery level: ").append(KissApplication.getApplication(context).getBatteryLevel()).append("%\n");
        systemPrompt.append("Network type: ").append(KissApplication.getApplication(context).getNetworkType()).append("\n");

        systemPrompt.append("Instructions:\n");
        systemPrompt.append("When the user asks for an action, analyze the request and respond with a JSON object in the following format:\n");
        systemPrompt.append("{\"trigger_intent\": \"intent_action\", \"intent_package\": \"Value for intent\", \"extra_input_value\": \"Optional extra intent value\", \"return_user_message\": \"message to display to the user\"}\n");
        systemPrompt.append("Available intents: \"OPEN_APP\", \"CALL_CONTACT\", \"RUN_SHORTCUT\", \"NAVIGATE\", \"NONE\".\n");
        systemPrompt.append("Provide the necessary parameters for the intent in the intent_package and extra_input_value fields.\n");
        systemPrompt.append("If the user's request is not related to an action, respond with \"NONE\" and return just text information for user about the request.\n");
        systemPrompt.append("If the user's request is not related to an action or the action is not supported, respond with \"NONE\" and return just text information for user about the request.\n");
        systemPrompt.append("If the user's request is related to an action, and the action is supported, respond with the return_message field.\n");
        systemPrompt.append("Do not include any other fields in the JSON object.\n");
        systemPrompt.append("Example:\n");
        systemPrompt.append("{\"trigger_intent\": \"OPEN_APP\", \"intent_package\": \"com.android.calculator2\", \"extra_input_value\": \"\", \"return_user_message\": \"Calculator app opened ...\"}\n");
        systemPrompt.append("Example:\n");
        systemPrompt.append("{\"trigger_intent\": \"CALL_CONTACT\", \"intent_package\": \"Contact Name\", \"extra_input_value\": \"tel:1234567890\", \"return_user_message\": \"Contact 1234567890 called\"}\n");
        systemPrompt.append("Example:\n");
        systemPrompt.append("{\"trigger_intent\": \"RUN_SHORTCUT\", \"intent_package\": \"com.android.calculator2.SHORTCUT_CALCULATOR\", \"extra_input_value\": \"2+2\", \"return_user_message\": \"Shortcut for Calculator app opened\"}\n");
        systemPrompt.append("Example:\n");
        systemPrompt.append("{\"trigger_intent\": \"NAVIGATE\", \"intent_package\": \"com.google.android.apps.maps\", \"extra_input_value\": \"Central Park\", \"return_user_message\": \"Navigating to Central Park\"}\n");
        systemPrompt.append("Example:\n");
        systemPrompt.append("{\"trigger_intent\": \"NONE\", \"intent_package\": \"\", \"extra_input_value\": \"\", \"return_user_message\": \"Message to display to the user\"}\n");
        return systemPrompt.toString();

    }

    private String getNotes() {
        StringBuilder notes = new StringBuilder();
        List<NotePojo> noteList = KissApplication.getApplication(context).getDataHandler().getAllNotes();
        assert noteList != null;
        for (NotePojo note : noteList) {
            notes.append(note.getContent()).append("\n");
        }
        return notes.toString();
    }

    private String getContacts() {
        StringBuilder contacts = new StringBuilder();
        List<ContactsPojo> contactList = KissApplication.getApplication(context).getDataHandler().getContacts();
        assert contactList != null;
        for (ContactsPojo contact : contactList) {
            contacts.append(contact.getName() + " - " + contact.phone + "\n");
        }
        return contacts.toString();
    }

    private String getAvailableShortcuts() {
        StringBuilder availableShortcuts = new StringBuilder();
        List<ShortcutPojo> shortcutList = KissApplication.getApplication(context).getDataHandler().getShortcuts();
        assert shortcutList != null;
        for (ShortcutPojo shortcut : shortcutList) {
            availableShortcuts.append(shortcut.getName()).append("\n");
        }
        return availableShortcuts.toString();
    }

    private String getInstalledApps() {
        StringBuilder installedApps = new StringBuilder();
        List<AppPojo> appList = KissApplication.getApplication(context).getDataHandler().getApplications();
        assert appList != null;
        for (AppPojo app : appList) {
            installedApps.append(app.getName()).append("\n");
        }
        return installedApps.toString();
    }

    private List<String> parseCapabilities(String capabilitiesJson) {
        try {
            JSONObject capabilities = new JSONObject(capabilitiesJson);
            JSONArray capabilitiesArray = capabilities.getJSONArray("capabilities");
            String subject = capabilities.getString("subject");
            Log.d(TAG, "Subject: " + subject);
            List<String> capabilitiesList = new ArrayList<>();
            for (int i = 0; i < capabilitiesArray.length(); i++) {
                capabilitiesList.add(capabilitiesArray.getString(i));
            }
            return capabilitiesList;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing capabilities and subject", e);
            return null;
        }
    }

    private String makeApiCall(String prompt, String systemPrompt) {
        try {
            // Get conversation history
            JSONArray conversationHistory = getConversationHistory();

            // Build messages array
            JSONArray messages = new JSONArray();
            // Add previous messages
            for (int i = 0; i < conversationHistory.length(); i++) {
                messages.put(conversationHistory.getJSONObject(i));
            }
            // Add system prompt
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.put(systemMessage);


            // Add current prompt as user message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);

            // Build request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-4o-mini"); // Specify the model you want to use
            requestBody.put("messages", messages);
            JSONObject responseFormat = new JSONObject();
            responseFormat.put("type", "json_object");
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
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    String assistantReply = message.getString("content");
                    try {
                        JSONObject jsonResponseContent = new JSONObject(assistantReply);
                        if (jsonResponseContent.has("trigger_intent") && jsonResponseContent.has("intent_package") && jsonResponseContent.has("extra_input_value") && jsonResponseContent.has("return_user_message")) {
                            String triggerIntent = jsonResponseContent.getString("trigger_intent");
                            String intentPackage = jsonResponseContent.getString("intent_package");
                            String extraIntentInput = jsonResponseContent.getString("extra_input_value");
                            String returnMessage = jsonResponseContent.getString("return_user_message");
                            executeIntent(triggerIntent, intentPackage, extraIntentInput);
                            return returnMessage;
                        } else {
                            return assistantReply;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                        return assistantReply;
                    }
                }
            } else {
                Log.e(TAG, "API call failed with code: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during API call", e);
        }
        return null;
    }

    private void executeIntent(String triggerIntent, String intentPackage, String extraIntentInput) {
        Log.d(TAG, "Values : " + triggerIntent + ", " + intentPackage + ", " + extraIntentInput);
        switch (triggerIntent) {
            case "OPEN_APP": {
                //KissApplication.getApplication(context).getDataHandler().addToHistory("app://" + extraIntentInput + "/" + extraIntentInput);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage(intentPackage);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            break;
            case "NAVIGATE": {
                Intent intentNavigate = new Intent(Intent.ACTION_VIEW);
                intentNavigate.setData(Uri.parse("geo:0,0?q=" + extraIntentInput));
                intentNavigate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intentNavigate);
            }
            break;
            case "CALL_CONTACT": {
                Intent phoneIntent = new Intent(Intent.ACTION_CALL);
                phoneIntent.setData(Uri.parse(extraIntentInput));
                phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!Permission.checkPermission(context, Permission.PERMISSION_CALL_PHONE)) {
                    Permission.askPermission(Permission.PERMISSION_CALL_PHONE, new Permission.PermissionResultListener() {
                        @Override
                        public void onGranted() {
                            // Great! Start the intent we stored for later use.
                            context.startActivity(phoneIntent);
                        }

                        @Override
                        public void onDenied() {
                            Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                } else {
                    context.startActivity(phoneIntent);
                }
                Log.d(TAG, "Calling: " + intentPackage + ", " + extraIntentInput);
            }
            break;
            case "RUN_SHORTCUT": {
                KissApplication.getApplication(context).getDataHandler().addToHistory("shortcut://" + intentPackage + "/" + extraIntentInput);
                Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
                shortcutIntent.setPackage(intentPackage);
                shortcutIntent.setData(Uri.parse(extraIntentInput));
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(shortcutIntent);
            }
            break;
            case "NONE":
                break;
        }
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

    // Step 1: Analyze Intent using a fast model
    private String analyzeIntent(String prompt) {
        try {
            JSONArray messages = new JSONArray();

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a phone-based AI assistant that analyzes user prompts to determine:\n" +
                    "\n" +
                    "1. **Capabilities Required**: Identify which capabilities are needed to fulfill the user's request. Possible capabilities are: `\"contacts\"`, `\"shortcuts\"`, `\"apps\"`, `\"notes\"`, `\"navigate\"`. If none are required, return an empty array.\n" +
                    "\n" +
                    "2. **Subject Extraction**: Extract the expected topic or subject from the user's prompt.\n" +
                    "\n" +
                    "**Response Format**: Provide a JSON object with the following structure:\n" +
                    "\n" +
                    "{\n" +
                    "  \"capabilities\": [\"contacts\", \"notes\"], // An array of required capabilities\n" +
                    "  \"subject\": \"John Doe\"                  // The extracted subject from the prompt\n" +
                    "}\n" +
                    "\n" +
                    "or" + "\n" +
                    "{\n" +
                    "  \"capabilities\": [\"navigate\", \"apps\"], // An array of required capabilities\n" +
                    "  \"subject\": \"Central Park\"             // The extracted subject from the prompt\n" +
                    "}" +
                    "Ensure that you only respond with the JSON object and do not include any additional text.\n");
            messages.put(systemMessage);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "User prompt: " + prompt);
            messages.put(userMessage);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.0);
            JSONObject responseFormat = new JSONObject();
            responseFormat.put("type", "json_object");
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
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    String assistantReply = message.getString("content");
                    return assistantReply; // Should be JSON array of capabilities
                }
            } else {
                Log.e(TAG, "Intent analysis API call failed with code: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during intent analysis API call", e);
        }
        return null;
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

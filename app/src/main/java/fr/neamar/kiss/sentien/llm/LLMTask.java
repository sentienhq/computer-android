package fr.neamar.kiss.sentien.llm;

import static fr.neamar.kiss.sentien.llm.LLMService.makeApiCall;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.utils.TimeUtils;

public class LLMTask {
    private static final String TAG = "\uD83D\uDCAC LLMTask";
    private String originalUserPrompt;
    private int step;
    private JSONArray nextActions;
    private LLMService.LLMCallback callback;
    private LLMActions llmActions;
    private JSONArray constraints;
    private JSONArray previousMessages;
    private String systemPrompt;
    private JSONArray previousActions;
    private String mainGoal;
    private String workingMemory;


    public LLMTask(String originalUserPrompt, LLMService.LLMCallback callback, LLMActions llmActions) {
        this.originalUserPrompt = originalUserPrompt;
        this.step = 0;
        this.workingMemory = "[]";
        this.callback = callback;
        this.llmActions = llmActions;
        this.systemPrompt = LLMPrompt.buildPreSystemPrompt(llmActions.getStringList());
        this.previousMessages = new JSONArray();
        this.nextActions = new JSONArray();
        this.previousActions = new JSONArray();
        this.mainGoal = "";
        this.constraints = new JSONArray();
        JSONArray initMsg = this.getMessagesForRequest(this.originalUserPrompt);
        if (initMsg == null) {
            this.callback.onError("Error init LLMTask");
        }
        JSONObject responseObject = makeApiCall(initMsg);
        boolean ready = this.processResponseOutput(responseObject);
        if (!ready) {
            this.callback.onError("Error init LLMTask, not ready");
        }
        this.run();

    }

    private boolean processResponseOutput(JSONObject responseObject) {
        try {
            if (responseObject == null) {
                return false;
            }
            JSONArray choices = responseObject.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                if (choice == null) {
                    return false;
                }
                JSONObject message = choice.getJSONObject("message");
                this.previousMessages.put(message);
                String assistantReply = message.getString("content");
                JSONObject jsonResponseContent = new JSONObject(assistantReply);
                this.nextActions = jsonResponseContent.getJSONArray("next_actions");
                this.previousActions = jsonResponseContent.getJSONArray("previous_actions");
                this.mainGoal = jsonResponseContent.getString("main_goal");
                this.constraints = jsonResponseContent.getJSONArray("constraints");
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private JSONArray getMessagesForRequest(String newUserPrompt) {
        try {
            JSONArray messages = new JSONArray();
            if (this.previousMessages.length() > 0) {
                for (int i = 0; i < this.previousMessages.length(); i++) {
                    messages.put(this.previousMessages.getJSONObject(i));
                }
            } else {
                // Add system prompt
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", this.systemPrompt + this.workingMemory);
                messages.put(systemMessage);
            }

            // Add current prompt as user message
            if (newUserPrompt != null) {
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", newUserPrompt);
                messages.put(userMessage);
            }
            this.previousMessages = messages;
            return messages;
        } catch (Exception e) {
            return null;
        }

    }

    private void run() {
        try {
            boolean executionEnded = false;
            while (!executionEnded) {
                JSONArray newPreviousActions = new JSONArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.callback.onError("Error performing LLM task");
            return;
        }

    }
}

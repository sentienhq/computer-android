package fr.neamar.kiss.sentien.llm;

import static fr.neamar.kiss.sentien.llm.LLMService.makeApiCall;

import android.content.Context;
import android.os.Looper;

import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public class LLMTask {
    private static final String TAG = "\uD83D\uDCAC LLMTask";
    public LLMService.LLMCallback callback;
    private Context context;
    private String originalUserPrompt;
    private int step;
    private JSONArray nextActions;
    private LLMActions llmActions;
    private JSONArray previousMessages;
    private String systemPrompt;
    private JSONArray previousActions;
    private String mainGoal;
    private String workingMemory;
    private String lastUserInfoMessage;


    public LLMTask(Context context, String originalUserPrompt, LLMService.LLMCallback callback, LLMActions llmActions) {
        this.context = context;
        this.originalUserPrompt = originalUserPrompt;
        this.step = 0;
        this.workingMemory = "[]";
        this.callback = callback;
        this.llmActions = llmActions;
        this.systemPrompt = LLMPrompt.buildPreSystemPrompt(llmActions.getStringList());
        this.previousMessages = new JSONArray();
        this.mainGoal = "";
        this.previousActions = new JSONArray();
        this.nextActions = new JSONArray();
        this.lastUserInfoMessage = "";
        JSONArray initMsg = this.prepareMessagesForRequest(this.originalUserPrompt);
        if (initMsg == null) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error performing LLM task"));
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
                Log.d(TAG, "LLMTask: nextActions: " + this.nextActions.toString());
                this.previousActions = jsonResponseContent.getJSONArray("previous_actions");
                this.mainGoal = jsonResponseContent.getString("main_goal");
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private JSONArray prepareMessagesForRequest(String newUserPrompt) {
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
                for (int i = 0; i < this.nextActions.length(); i++) {
                    JSONObject action = this.nextActions.getJSONObject(i);
                    String actionName = action.getString("action_name");
                    JSONObject params = action.getJSONObject("params");
                    if (actionName.equals("EXECUTION_REEVALUATE")) {
                        // get all previous actions into string
                        String previousActionsString = this.previousActions.toString();
                        String wholeRquestString = "\"main_goal\":" + this.mainGoal + "\n\"previous_actions\":" + previousActionsString;
                        JSONArray prepMessage = this.prepareMessagesForRequest(wholeRquestString);
                        JSONObject prepResponse = makeApiCall(prepMessage);
                        boolean respBool = this.processResponseOutput(prepResponse);
                        if (!respBool) {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error init LLMTask, not ready"));
                        }
                        this.run();
                        executionEnded = true;
                        break;
                    } else if (actionName.equals("EXECUTION_END")) {
                        executionEnded = true;
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(this.lastUserInfoMessage));
                        break;
                    } else {
                        LLMService.ActionResultImpl result = this.llmActions.processAction(this.context, actionName, params);
                        JSONObject actionWithResult = new JSONObject();
                        actionWithResult.put("action_name", actionName);
                        actionWithResult.put("params", params);
                        actionWithResult.put("result", result.getActionResultValues());
                        this.lastUserInfoMessage = result.getUserMessage();
                        this.previousActions.put(actionWithResult);
                        if (!result.isSuccess()) {
                            executionEnded = true;
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError(this.lastUserInfoMessage));
                            break;
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onUpdate(this.lastUserInfoMessage));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.callback.onError("Error performing LLM task");
            return;
        }

    }
}

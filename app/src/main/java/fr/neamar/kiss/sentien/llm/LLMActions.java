package fr.neamar.kiss.sentien.llm;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.searcher.BackgroundQuerySearcher;
import fr.neamar.kiss.utils.Permission;

enum LLMActionType {
    STATIC, // actions that can be performed by predefined intents
    DYNAMIC, // actions that can be triggered by shortcuts or other found intents
    DISABLED // actions that are disabled by the user and not accessible to LLM or user
}

enum LLMDataAccessCapability {
    CONTACTS,
    SHORTCUTS,
    APPS,
    NOTES, // notes consists of content, type, parentId, childIds, timestamp, tags and types can be notes, aiConvo or aiMemory
    NONE
}

class LLMAction {
    String actionName; // Name of the action
    String actionDescription;
    LLMActionType actionType;
    String[] actionProps; // Properties of the action (e.g. contact_name, contact_phone) that will be passed to Intent or appropriate function, if property is empty string ("") then it will be ignored
    LLMDataAccessCapability[] requiredCapabilities; // Capabilities are know-how or data that is required to perform the action

    public LLMAction(String actionName, String actionDescription, LLMActionType actionType, String[] actionProps, LLMDataAccessCapability[] requiredCapabilities) {
        this.actionName = actionName;
        this.actionDescription = actionDescription;
        this.actionType = actionType;
        this.actionProps = actionProps;
        this.requiredCapabilities = requiredCapabilities;
    }

    public String getPropsAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (String prop : this.actionProps) {
            sb.append("\"").append(prop).append("\"").append(", ");
        }
        if (this.actionProps.length > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("]");
        return sb.toString();
    }

    public String getRequiredCapabilitiesAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (LLMDataAccessCapability capability : this.requiredCapabilities) {
            if (capability.toString().equals("NONE")) {
                return "[]";
            } else {
                sb.append("\"").append(capability.toString()).append("\"").append(", ");
            }
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("]");
        return sb.toString();
    }

    public String getNicelyFormattedAction() {
        return "\"action_name\": \"" + this.actionName + "\", " +
                "\"description\": \"" + this.actionDescription + "\"," +
//                "\"type\": \"" + this.actionType + "\", " +
                "\"paramKeys\": " + this.getPropsAsString() + ", " +
                "\"required_capabilities\": " + this.getRequiredCapabilitiesAsString();
    }
}

public class LLMActions {
    private static final String TAG = "\uD83D\uDCAC LLMActions";
    private final ArrayList<LLMAction> ACTIONS = new ArrayList<>();
    public String stringList = "";

    public LLMActions() {

        // NONE or ANSWER
        LLMDataAccessCapability[] noneCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE};
        ACTIONS.add(new LLMAction("SIMPLE_ANSWER", "Just reply to user with answer and do no other activity.", LLMActionType.STATIC, new String[]{"smart_answer"}, noneCap));
        ACTIONS.add(new LLMAction("ASK_QUESTION", "Ask a question to the user. The question should be in the user_request field.", LLMActionType.DISABLED, new String[]{"user_question"}, noneCap));
        ACTIONS.add(new LLMAction("ASK_OPTIONS", "Ask a question to the user. The question should be in the user_request field. The options should be in the array with string values.", LLMActionType.DISABLED, new String[]{"user_question", "options"}, noneCap));
        // GET CAPABILITIES
        // for example, to get the capabilities of the user's contacts send capabilityType="CONTACTS" and capabilityQuery="imrich" for specific subjects
        // to get the capabilities of the user's notes send capabilityType="NOTES" and capabilityQuery="imrich, work, personal" for specific subjects (note: the subjects are comma separated) and filtered by the user's tags and content
        // to get all contacts send capabilityType="CONTACTS" and capabilityQuery="*" (avoid it as much as possible)
        // it is recommended to send specific capabilityQuery to get more specific data and save the result in memory
        ACTIONS.add(new LLMAction("GET_CAPABILITY", "Get the required data capabilities for the AI to make decisions. Possible types are CONTACTS, SHORTCUTS, APPS or NOTES. To get the capabilities of the user's contacts send capabilityType=\"CONTACTS\" and capabilityQuery=\"imrich\" for specific subjects. To get the capabilities of the user's notes send capabilityType=\"NOTES\" and capabilityQuery=\"imrich, work, personal\" for specific subjects (note: the subjects are comma separated) and filtered by the user's tags and content. To get all contacts send capabilityType=\"CONTACTS\" and capabilityQuery=\"*\" (avoid it as much as possible). It is recommended to send specific capabilityQuery to get more specific data and save the result in memory.", LLMActionType.STATIC, new String[]{"capabilityType", "capabilityQuery"}, noneCap));

        // execution tasks
        ACTIONS.add(new LLMAction("EXECUTION_REEVALUATE", "This command should be used to re-evaluate the action list with results and continue processing.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("EXECUTION_END", "This is returned in the end of task list for the everyone to know when to stop processing.", LLMActionType.STATIC, new String[]{}, noneCap));

        // WORKING MEMORY
        // working memory is a temporary storage for data that is not saved to the next prompt
        ACTIONS.add(new LLMAction("WORKING_MEMORY_CLEAR", "Clear AI working memory.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("WORKING_MEMORY_SAVE", "Save data to working memory for future AI processing.", LLMActionType.STATIC, new String[]{"data"}, noneCap));

        // CONTACTS
        LLMDataAccessCapability[] contactsCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.CONTACTS};
        ACTIONS.add(new LLMAction("CONTACTS_FIND", "Find a contact.", LLMActionType.DISABLED, new String[]{"contact_query"}, noneCap));
        ACTIONS.add(new LLMAction("CONTACTS_CREATE", "Add a new contact.", LLMActionType.DISABLED, new String[]{"contact_name", "contact_phone", "contact_nickname", "contact_email"}, noneCap));
        ACTIONS.add(new LLMAction("CONTACTS_DELETE", "Delete a contact.", LLMActionType.DISABLED, new String[]{"contact_id", "contact_name", "contact_phone"}, contactsCap));
        ACTIONS.add(new LLMAction("CONTACTS_EDIT", "Edit a contact.", LLMActionType.DISABLED, new String[]{"original_contact_name", "original_contact_phone", "new_contact_name", "new_contact_phone", "new_contact_nickname", "new_contact_email"}, contactsCap));

        // EVENTS
        LLMDataAccessCapability[] appShortcutContactsCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.CONTACTS, LLMDataAccessCapability.APPS, LLMDataAccessCapability.SHORTCUTS};
        ACTIONS.add(new LLMAction("EVENTS_CREATE", "Create a new event in calendar or user defined app.", LLMActionType.DISABLED, new String[]{"app_shortcut_id", "event_name", "event_location", "event_description", "event_start_time", "event_end_time", "all_day", "extra_emails"}, appShortcutContactsCap));
        ACTIONS.add(new LLMAction("EVENTS_NEW_TASK", "Create a new task in calendar or user defined app.", LLMActionType.DISABLED, new String[]{"app_shortcut_id", "task_name", "task_location", "task_description", "task_start_time", "task_end_time"}, appShortcutContactsCap));

        // CLOCK
        ACTIONS.add(new LLMAction("CLOCK_SET_ALARM", "Set an alarm.", LLMActionType.DISABLED, new String[]{"alarm_name_msg", "alarm_time_hour", "alarm_time_minutes", "alarm_days"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_NEW_TIMER", "Create a new timer.", LLMActionType.DISABLED, new String[]{"timer_name_msg", "timer_time_hour", "timer_time_minutes", "timer_days"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_DISMISS_TIMER", "Dismiss a timer.", LLMActionType.DISABLED, new String[]{"timer_name_msg"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_NEW_STOPWATCH", "Create a new stopwatch.", LLMActionType.DISABLED, new String[]{"stopwatch_name_msg", "stopwatch_time_hour", "stopwatch_time_minutes", "stopwatch_days"}, noneCap));

        // NAVIGATION
        LLMDataAccessCapability[] appShortcutCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.SHORTCUTS, LLMDataAccessCapability.APPS};
        ACTIONS.add(new LLMAction("NAVIGATION_TO", "Opens default navigation app and navigate to a location string address.", LLMActionType.STATIC, new String[]{"location_name"}, appShortcutCap));
        ACTIONS.add(new LLMAction("NAVIGATION_GET_MY_LOCATION", "Get my current location lat and long.", LLMActionType.DISABLED, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("NAVIGATION_GET_MY_ADDRESS", "Get my current address.", LLMActionType.DISABLED, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        // find nearby places
        // possible categories: restaurants, hotels, tourist_attractions, gas_stations, parking_garages, shopping_centers, entertainment, sports, religious_sites, bars, nightlife, museums, movie_theaters, amusement_parks, art_galleries, music_venues, financial_services, healthcare, retail, transportation, education, government, libraries, fitness, religion, history,
        ACTIONS.add(new LLMAction("NAVIGATION_FIND_NEARBY_CATEGORY", "Find nearby category.", LLMActionType.STATIC, new String[]{"category_name"}, noneCap));

        // CAMERA
        ACTIONS.add(new LLMAction("CAMERA_TAKE_PICTURE", "Take a picture.", LLMActionType.DISABLED, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_RECORD_VIDEO", "Record a video.", LLMActionType.DISABLED, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_TAKE_SELFIE", "Take a selfie.", LLMActionType.DISABLED, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_SCAN_QR_CODE", "Scan a QR code.", LLMActionType.DISABLED, new String[]{"app_or_shortcut_id"}, appShortcutCap));

        // MESSAGING
        ACTIONS.add(new LLMAction("MESSAGING_SEND_MESSAGE", "Send a message to a contact on a user defined app or shortcut.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "message"}, appShortcutContactsCap));
        ACTIONS.add(new LLMAction("MESSAGING_SEND_EMAIL", "Send an email to a contact on a users default app.", LLMActionType.STATIC, new String[]{"contact_name", "contact_email", "email_subject", "email_body"}, contactsCap));
        ACTIONS.add(new LLMAction("MESSAGING_SEND_SMS", "Send an SMS to a contact on a user on a default sms app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "sms_body"}, contactsCap));

        // NOTES
        LLMDataAccessCapability[] notesCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.NOTES};
        ACTIONS.add(new LLMAction("NOTES_FIND", "Find a similar notes.", LLMActionType.STATIC, new String[]{"note_queries[]"}, notesCap));
        ACTIONS.add(new LLMAction("NOTES_CREATE", "Create a new note.", LLMActionType.STATIC, new String[]{"note_content", "note_tags"}, noneCap));
        ACTIONS.add(new LLMAction("NOTES_DELETE", "Delete a note.", LLMActionType.DISABLED, new String[]{"note_id"}, notesCap));
        ACTIONS.add(new LLMAction("NOTES_EDIT", "Edit a note.", LLMActionType.DISABLED, new String[]{"note_id", "note_content", "note_tags"}, notesCap));

        // WEB
        // TODO - test this to make sure it works
        ACTIONS.add(new LLMAction("WEB_SEARCH_OPEN_UI", "Open web search UI. LLM is not able to read the UI, so it is only for the user to interact with.", LLMActionType.STATIC, new String[]{"search_query"}, noneCap));
        ACTIONS.add(new LLMAction("WEB_OPEN_URL", "Open a URL.", LLMActionType.DISABLED, new String[]{"url"}, noneCap));
        ACTIONS.add(new LLMAction("WEB_SEARCH_TOP_RESULTS", "Search for a query on the web and return top results.", LLMActionType.DISABLED, new String[]{"search_query"}, noneCap));
        ACTIONS.add(new LLMAction("WEB_SEARCH_WIKI_INFO", "Search for a query on the web and return wiki info.", LLMActionType.DISABLED, new String[]{"search_query"}, noneCap));
        ACTIONS.add(new LLMAction("WEB_GET_URL_CONTENT", "Get the content of a URL.", LLMActionType.DISABLED, new String[]{"url"}, noneCap));

        // Clipboard
        ACTIONS.add(new LLMAction("CLIPBOARD_SET_TEXT", "Copy text to clipboard.", LLMActionType.DISABLED, new String[]{"text"}, noneCap));
        ACTIONS.add(new LLMAction("CLIPBOARD_GET_TEXT", "Get text from clipboard.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("CLIPBOARD_CLEAR", "Clear clipboard.", LLMActionType.DISABLED, new String[]{}, noneCap));

        // MEMORY - long-term AI memory
        // notes consists of content, type, parentId, childIds, timestamp, tags and types can be notes, aiConvo or aiMemory
        ACTIONS.add(new LLMAction("MEMORY_ADD", "Add a value to memory.", LLMActionType.DISABLED, new String[]{"key", "value", "tags"}, noneCap));
        ACTIONS.add(new LLMAction("MEMORY_GET", "Get a value from memory.", LLMActionType.DISABLED, new String[]{"key"}, notesCap));
        ACTIONS.add(new LLMAction("MEMORY_DELETE", "Delete a value from memory.", LLMActionType.DISABLED, new String[]{"key"}, notesCap));
        ACTIONS.add(new LLMAction("MEMORY_FIND", "Find a value in memory.", LLMActionType.DISABLED, new String[]{"query"}, notesCap));
        ACTIONS.add(new LLMAction("MEMORY_UPDATE", "Update a value in memory.", LLMActionType.DISABLED, new String[]{"key", "value", "tags"}, notesCap));

        // ORDERS via shortcuts
        ACTIONS.add(new LLMAction("ORDERS_CREATE_PRODUCT", "Create an order for product.", LLMActionType.DISABLED, new String[]{"product_query"}, appShortcutCap));
        ACTIONS.add(new LLMAction("ORDERS_CREATE_FOOD", "Create an order for food.", LLMActionType.DISABLED, new String[]{"food_query"}, appShortcutCap));
        ACTIONS.add(new LLMAction("ORDERS_CREATE_TAXI", "Create an order for taxi.", LLMActionType.DISABLED, new String[]{"taxi_query"}, appShortcutCap));

        // NOTIFICATIONS
        ACTIONS.add(new LLMAction("NOTIFICATIONS_GET", "Get notifications.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("NOTIFICATIONS_CLEAR", "Clear notifications.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("NOTIFICATIONS_SEND_SELF", "Send a notification.", LLMActionType.DISABLED, new String[]{"notification_title", "notification_body", "intent_package", "intent_action", "intent_extra_input_value"}, noneCap));

        // MUSIC
        ACTIONS.add(new LLMAction("MUSIC_PLAY", "Play music.", LLMActionType.DISABLED, new String[]{"music_name_or_style"}, appShortcutCap));
        ACTIONS.add(new LLMAction("MUSIC_PAUSE", "Pause music.", LLMActionType.DISABLED, new String[]{}, appShortcutCap));
        ACTIONS.add(new LLMAction("MUSIC_SEARCH", "Search for music.", LLMActionType.DISABLED, new String[]{"music_query"}, appShortcutCap));

        // SOUND
        ACTIONS.add(new LLMAction("SOUND_GET_VOLUME", "Get sound volume level.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("SOUND_SET_VOLUME", "Set sound volume level.", LLMActionType.DISABLED, new String[]{"volume_level"}, noneCap));
        ACTIONS.add(new LLMAction("SOUND_MUTE", "Mute sound.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("SOUND_UNMUTE", "Unmute sound.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("SOUND_BEEP", "Make a beep sound.", LLMActionType.DISABLED, new String[]{}, noneCap));

        // VOICE IO
        ACTIONS.add(new LLMAction("VOICE_SAY_USER", "Say something to user.", LLMActionType.DISABLED, new String[]{"voice_message"}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_SAY_DEVICE", "Say something to device or caller.", LLMActionType.DISABLED, new String[]{"voice_message"}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_LISTEN", "Listen to caller.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_LISTEN_FOR", "Listen to caller for a given amount of time.", LLMActionType.DISABLED, new String[]{"timeout_in_ms"}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_STOP_LISTEN", "Stop listening to caller.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_WAIT_FOR_KEYWORD", "Wait for caller to say a given keyword.", LLMActionType.DISABLED, new String[]{"keyword"}, noneCap));

        // USER INPUT
        ACTIONS.add(new LLMAction("USER_INPUT", "Get user input text, boolean or number.", LLMActionType.DISABLED, new String[]{"input_type"}, noneCap));

        // CALLING
        ACTIONS.add(new LLMAction("PHONE_CALL", "Call a phone number.", LLMActionType.STATIC, new String[]{"contact_name", "contact_phone"}, contactsCap));
        ACTIONS.add(new LLMAction("PHONE_CALL_WITH_MESSAGE", "Call a phone number with a message.", LLMActionType.DISABLED, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "message"}, appShortcutContactsCap));
        ACTIONS.add(new LLMAction("PHONE_HANGUP", "Hang up a phone call.", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("PHONE_PICK_UP", "Pick up a phone call.", LLMActionType.DISABLED, new String[]{}, noneCap));

        // APPS & SHORTCUTS
        LLMDataAccessCapability[] appCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.APPS};
        ACTIONS.add(new LLMAction("APP_OPEN", "Open an app. Provide package and activity name", LLMActionType.STATIC, new String[]{"app_package_name", "app_activity_name"}, appCap));
        ACTIONS.add(new LLMAction("APP_CLOSE", "Close an app.", LLMActionType.DISABLED, new String[]{"app_package_name", "app_activity_name"}, appCap));
        ACTIONS.add(new LLMAction("SHORTCUT_OPEN", "Launching shortcut is like opening App with extra input parameter.", LLMActionType.STATIC, new String[]{"shortcut_package_name", "shortcut_id", "extra_input_value"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_LAUNCH_SHORTCUT_OR_APP_WITH_PARAMS", "Launch a shortcut or app with extra intent input parameters - use String separated by comma.", LLMActionType.DISABLED, new String[]{"app_or_shortcut_id", "extra_input_values"}, appShortcutCap));
        // add accessibility actions to control apps or shortcuts
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SCROLL_FORWARD", "Scroll forward in an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SCROLL_BACKWARD", "Scroll backward in an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SCROLL_TO_POSITION", "Scroll to a specific position in an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "position"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SCROLL_TO_TOP", "Scroll to the top of an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SCROLL_TO_BOTTOM", "Scroll to the bottom of an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SCROLL_TO_LEFT", "Scroll to the left of an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SCROLL_TO_RIGHT", "Scroll to the right of an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_CLICK", "Click on an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_LONG_CLICK", "Long click on an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_DOUBLE_CLICK", "Double click on an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SWIPE", "Swipe on an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_PINCH", "Pinch on an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_ZOOM_IN", "Zoom in on an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_ZOOM_OUT", "Zoom out on an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_BACK", "Back to an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SCREENSHOT", "Get screenshot of an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
//        ACTIONS.add(new LLMAction("APP_ACCESSIBILITY_SIZE", "Get size of an app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));

        // UTILS delays and awaits
        ACTIONS.add(new LLMAction("UTILS_DELAY", "Delay for a given amount of time in ms.", LLMActionType.STATIC, new String[]{"delay_in_ms"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_WAIT_FOR", "Wait for a given condition to be true.", LLMActionType.DISABLED, new String[]{"condition", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_WAIT_FOR_DATE", "Wait for a given date to be reached.", LLMActionType.DISABLED, new String[]{"day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_ON_DATE", "Repeat an action on a given date.", LLMActionType.DISABLED, new String[]{"action", "day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_UNTIL", "Repeat an action until a given condition is true.", LLMActionType.DISABLED, new String[]{"action", "condition", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_UNTIL_DATE", "Repeat an action until a given date is reached.", LLMActionType.DISABLED, new String[]{"action", "day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));

        ACTIONS.add(new LLMAction("UTILS_CLEANUP_MEMORY", "Cleanup memory.", LLMActionType.DISABLED, new String[]{}, noneCap));

        // WIFI
        ACTIONS.add(new LLMAction("WIFI_ON", "Turn WIFI on", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("WIFI_OFF", "Turn WIFI on", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("WIFI_GET_NEARBY_LIST", "Scan for nearby devices", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("WIFI_CONNECT_NETWORK", "Connect to nearby network", LLMActionType.DISABLED, new String[]{"network_name"}, noneCap));
        ACTIONS.add(new LLMAction("WIFI_DISCONNECT_NETWORK", "Disconnect from nearby network", LLMActionType.DISABLED, new String[]{"network_name"}, noneCap));

        // Bluetooth
        ACTIONS.add(new LLMAction("BLUETOOTH_ON", "Turn Bluetooth on", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("BLUETOOTH_OFF", "Turn Bluetooth off", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("BLUETOOTH_GET_LIST", "Scan for nearby devices and get list", LLMActionType.DISABLED, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("BLUETOOTH_CONNECT_DEVICE", "Connect to nearby device", LLMActionType.DISABLED, new String[]{"device_name"}, noneCap));
        ACTIONS.add(new LLMAction("BLUETOOTH_DISCONNECT_DEVICE", "Disconnect from nearby device", LLMActionType.DISABLED, new String[]{"device_name"}, noneCap));


        stringList = getNiceActionListForLLM();
    }

    public String getNiceActionListForLLM() {
        StringBuilder sb = new StringBuilder();
        for (LLMAction action : ACTIONS) {
            if (!action.actionType.toString().equals("DISABLED")) {
                sb.append("{").append(action.getNicelyFormattedAction()).append("},\n");
            }
        }
        return sb.toString();
    }

    public LLMService.ActionResultImpl processAction(Context context, String actionName, JSONObject actionParams) {
        Context appContext = context.getApplicationContext();
        try {
            Log.d(TAG, "processAction: " + actionName + " params: " + actionParams.toString());
            switch (actionName) {
                case "SIMPLE_ANSWER": {
                    String smartAnswer = actionParams.getString("smart_answer");
                    if (smartAnswer != null) {
                        return new LLMService.ActionResultImpl(true, smartAnswer, "replied to user:" + smartAnswer);
                    }
                    return new LLMService.ActionResultImpl(false, "Error: LLM send a wrong message", "smart_answer is null or not provided");
                }
                case "GET_CAPABILITY": {
                    String capabilityType = actionParams.getString("capabilityType");
                    String capabilityQuery = actionParams.getString("capabilityQuery");
                    String resultString = new BackgroundQuerySearcher(context, capabilityType, capabilityQuery).searchGetResultString();
                    Log.d(TAG, "GET_CAPABILITY result: " + resultString);
                    return new LLMService.ActionResultImpl(true, "LLM obtained new " + capabilityType + " capabilities", resultString);
                }
                case "NAVIGATION_TO": {
                    String extraIntentInput = actionParams.getString("location_name");
                    Intent intentNavigate = new Intent(Intent.ACTION_VIEW);
                    intentNavigate.setData(Uri.parse("geo:0,0?q=" + extraIntentInput));
                    intentNavigate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(intentNavigate);
                    return new LLMService.ActionResultImpl(true, "Navigating to" + extraIntentInput, "Navigated to location " + extraIntentInput);
                }
                case "WEB_SEARCH_OPEN_UI": {
                    String searchQuery = actionParams.getString("search_query");
                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY, searchQuery);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(intent);
                    return new LLMService.ActionResultImpl(true, "Opened web search UI", "Opened web search UI");
                }
                case "SHORTCUT_OPEN": {
                    String shortcutPackageName = actionParams.getString("shortcut_package_name");
                    String shortcutId = actionParams.getString("shortcut_id");
                    String extraInputValue = actionParams.getString("extra_input_value");
                    Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
                    shortcutIntent.setPackage("shortcut://" + shortcutPackageName + "/" + shortcutId);
                    shortcutIntent.setData(Uri.parse(extraInputValue));
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(shortcutIntent);
                    return new LLMService.ActionResultImpl(true, "Shortcut opened", "Shortcut opened");
                }
                case "APP_OPEN": {
                    String intentPackageName = actionParams.getString("app_package_name");
                    String intentActivityName = actionParams.getString("app_activity_name");
                    LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                    assert launcher != null;
                    Rect sourceBounds = null;
                    Bundle opts = null;
                    ComponentName className = new ComponentName(intentPackageName, intentActivityName);
                    UserHandle userHandle = android.os.Process.myUserHandle();
                    launcher.startMainActivity(className, userHandle, sourceBounds, opts);
                    return new LLMService.ActionResultImpl(true, "App opened", "App opened");
                }
                case "PHONE_CALL": {
                    // String intentPackage = actionParams.getString("app_or_shortcut_id");
                    String extraIntentInput = actionParams.getString("contact_phone");
                    String callerName = actionParams.getString("contact_name");
                    if (extraIntentInput.isEmpty()) {
                        return new LLMService.ActionResultImpl(false, "Phone number not provided", "Phone number is null or empty");
                    }
                    // KissApplication.getApplication(context).getDataHandler().addToHistory("contact://" + extraIntentInput + "/" + extraIntentInput);
                    // Find contact and get phone number
                    Intent phoneIntent = new Intent(Intent.ACTION_CALL);
                    phoneIntent.setData(Uri.parse("tel:" + extraIntentInput));
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

                    } else {
                        context.startActivity(phoneIntent);
                        return new LLMService.ActionResultImpl(true, "Calling: " + callerName + ": " + extraIntentInput, "Calling: " + callerName + ": " + extraIntentInput);
                    }
                }
                case "UTILS_DELAY": {
                    try {
                        String delayMs = actionParams.getString("delay_in_ms");
                        if (delayMs == null) {
                            return new LLMService.ActionResultImpl(false, "No delay specified", "No delay specified");
                        }
                        Log.d(TAG, "Delaying for " + delayMs + " ms");
                        Thread.sleep(Long.parseLong(delayMs));
                        return new LLMService.ActionResultImpl(true, "Delay complete", "Delay complete");
                    } catch (Exception e) {
                        return new LLMService.ActionResultImpl(false, "Error in delay: " + e.getMessage(), "Error in delay: " + e.getMessage());
                    }
                }
                // TODO: Add support for voice messages to user
//                case "VOICE_SAY_USER": {
//                    try {
//                        String voiceMessage = actionParams.getString("voice_message");
//                        if (!voiceMessage.isEmpty()) {
//                            final TextToSpeech[] tts = new TextToSpeech[1];
//                            tts[0] = new TextToSpeech(context, status -> {
//                                if (status == TextToSpeech.SUCCESS) {
//                                    int result = tts[0].setLanguage(Locale.getDefault());
//                                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                                        Log.e(TAG, "Language not supported");
//                                        tts[0].shutdown();
//                                    } else {
//                                        tts[0].setOnUtteranceProgressListener(new UtteranceProgressListener() {
//                                            @Override
//                                            public void onStart(String utteranceId) {
//                                                // Speech started
//                                            }
//
//                                            @Override
//                                            public void onDone(String utteranceId) {
//                                                // Speech completed
//                                                tts[0].shutdown();
//                                            }
//
//                                            @Override
//                                            public void onError(String utteranceId) {
//                                                // Error occurred
//                                                tts[0].shutdown();
//                                            }
//                                        });
//                                        tts[0].speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, null, "UniqueID");
//                                    }
//                                } else {
//                                    Log.e(TAG, "TTS initialization failed");
//                                    tts[0].shutdown();
//                                }
//                            });
//                            return new LLMService.ActionResultImpl(true, "Spoken to user: " + voiceMessage, "Spoken to user: " + voiceMessage);
//                        } else {
//                            return new LLMService.ActionResultImpl(false, "No voice message provided", "No voice message provided");
//                        }
//                    } catch (Exception e) {
//                        return new LLMService.ActionResultImpl(false, "Error in voice say: " + e.getMessage(), "Error in voice say: " + e.getMessage());
//                    }
//
//                }

                default:
                    return new LLMService.ActionResultImpl(false, "Unknown action: " + actionParams, "action not implemented");
            }
        } catch (Exception e) {
            //Log.e(TAG, "Error processing action", e);
            return new LLMService.ActionResultImpl(false, "In " + actionName + " error: " + e.getMessage(), "Error in " + actionName + " error: " + e.getMessage());
        }
    }


    public String getStringList() {
        return this.stringList;
    }
}

class LLMPrompt {
    private final String systemPrompt;

    LLMPrompt(String actionList) {
        this.systemPrompt = buildPreSystemPrompt(actionList);
    }

    LLMPrompt(String actionList, String customPromptOverride) {
        if (customPromptOverride != null) {
            this.systemPrompt = customPromptOverride;
        } else {
            this.systemPrompt = buildPreSystemPrompt(actionList);
        }
    }

    public static String buildPreSystemPrompt(String actionList) {
        return "Your primary task is to interpret the user's requests and generate responses in a specific JSON format that the application can parse and execute.You are an autonomous AI genius designed to accomplish tasks based on user instructions.\n" +
                "Your workflow involves the following components:" +
                "1. Main goal: The primary objective derived from the user's request. Clearly define what the user wants to achieve.\n" +
                "2. Constraints: Limitations or guidelines you must follow while achieving the main goal. Examples include resource limits, required capabilities, ethical guidelines, or specific user information." +
                "3. Previous actions: A array of actions you've already performed. The actions executed by the user device and AI so far with results providing more context and capabilities. Array containing JSON objects with the following structure:" +
                "- action_name: The name of the action." +
                "- params: The parameters required for the action in array of object, with key and value defined in action list." +
                "- result: The result of the action." +
                "4. Next actions: The actions that the user device and AI will execute in the next step. Array containing JSON objects with the following structure:" +
                "- action_name: The name of the action." +
                "- params: The parameters required for the action in array format.\n" +
                "Response format:" +
                "Your responses must always be a single JSON object with the following structure:\n" +
                "{\"main_goal\":\"string\",\"constraints\":[\"constraint1\",\"constraint2\"],\"previous_actions\":[],\"next_actions\":[{\"action_name\":\"string\",\"params\":{\"param1key\":\"value1\",\"param2key\":\"value2\"}}]}\n" +
                "Important Guidelines:\n" +
                "Consistency: Always use the JSON format provided." +
                "Clarity: Ensure each field is accurately filled." +
                "Capabilities: For required capabilities use GET_CAPABILITY action with capabilityType ('APPS', 'CONTACTS', etc ...) and capabilityQuery with the query to search for." +
                "Action Selection: Choose only actions from the available action list." +
                "Action Execution: Execute actions in the order specified." +
                "Execution Updates: Use EXECUTION_REEVALUATE when you need to re-evaluate your next actions based on new information - for example after use of GET_CAPABILITY to update next_actions.                                                                                                        Termination: Use the EXECUTION_END action when no further actions are needed.                                         No Extra Text: Do not include any text outside the JSON object." +
                "Be Smart: Observe the user's request and generate a response that is relevant and appropriate. Check shortcuts alternatives in capabilities to use if applicable." +
                "Well-Formed JSON: Ensure the JSON is well-formed and parsable.\n" +
                "Available Actions:\n" +
                actionList +
                "\n\nExamples:" +
                "Simple question example:" +
                "{\"user_request\": \"What's the capital of France?\"}" +
                "Response:\n" +
                "{\"main_goal\":\"Provide the capital city of France.\",\"constraints\":[],\"previous_actions\":[],\"next_actions\":[{\"action_name\":\"SIMPLE_ANSWER\",\"params\":{\"smart_answer\":\"The capital of France is Paris.\"}},{\"action_name\":\"EXECUTION_END\",\"params\":{}}]}\n" +
                "Example with constraints:" +
                "{\"user_request\": \"Please open the calculator app\"}\n" +
                "Response:\n" +
                "{\"main_goal\":\"Open the calculator app.\",\"constraints\":[\"No App capabilities\"],\"previous_actions\":[],\"next_actions\":[{\"action_name\":\"GET_CAPABILITY\",\"params\":{\"capabilityType\":\"APPS\",\"capabilityQuery\":\"calculator\"}},{\"action_name\":\"EXECUTION_REEVALUATE\",\"params\":{}},{\"action_name\":\"APP_OPEN\",\"params\":{\"app_id\":\"com.android.calculator\"}},{\"action_name\":\"EXECUTION_END\",\"params\":{}}]}                                                                                               \n" +
                "Complex Task with Constraints example: \n" +
                "{\"user_request\": \"Schedule a meeting with Johnny at 10AM and send him an email with the meeting details.\"}\n" +
                "Response:\n" +
                "{\"main_goal\":\"Schedule a meeting with John at 10AM tomorrow and send him an email with the meeting details.\",\"constraints\":[\"Find a contact named John\",\"Find a calendar app\",\"Find an event named Meeting with John\",\"Schedule a meeting with John at 10AM tomorrow and send him an email with the meeting details.\"],\"previous_actions\":[],\"next_actions\":[{\"action_name\":\"GET_CAPABILITY\",\"params\":{\"capabilityType\":\"CONTACTS\",\"capabilityQuery\":\"John\"}},{\"action_name\":\"GET_CAPABILITY\",\"params\":{\"capabilityType\":\"SHORTCUTS\",\"capabilityQuery\":\"calendar\"}},{\"action_name\":\"GET_CAPABILITY\",\"params\":{\"capabilityType\":\"APPS\",\"capabilityQuery\":\"calendar\"}},{\"action_name\":\"EXECUTION_REEVALUATE\",\"params\":{}},{\"action_name\":\"WORKING_MEMORY_SAVE\",\"params\":{\"data\":\"contact_name: Johny, event_name: Meeting with John, event_start_time: 2023-10-23T15:00:00, event_end_time: 2023-10-23T16:00:00,app_shortcut_id: com.android.calendar\"}},{\"action_name\":\"EVENTS_CREATE\",\"params\":{\"app_shortcut_id\":\"com.android.calendar\",\"event_name\":\"Meeting with John\",\"event_location\":\"\",\"event_description\":\"\",\"event_start_time\":\"2023-10-23T15:00:00\",\"event_end_time\":\"2023-10-23T16:00:00\",\"all_day\":false,\"extra_emails\":\"\",\"contact_name\":\"Johny\"}},{\"action_name\":\"END_OF_EXECUTION\",\"params\":{}}]}                                                                                            \n" +
                "Explanation: \n" +
                "GET_CAPABILITY actions are used to retrieve necessary information:\n" +
                "First, to get the contact details for \"John\".\n" +
                "Second, to get any shortcuts related to the calendar.\n" +
                "Third, to get the default calendar application.\n" +
                "WORKING_MEMORY_SAVE stores the retrieved data and important parameters for use in subsequent actions.\n" +
                "EXECUTION_REEVALUATE is used to re-evaluate the action list with the results obtained so far and continue processing.\n" +
                "After re-evaluation, the assistant proceeds with creating the event and so on ...\n" +
                "Working memory:";

    }
}


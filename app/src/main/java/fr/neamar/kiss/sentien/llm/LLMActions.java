package fr.neamar.kiss.sentien.llm;

import java.util.ArrayList;

enum LLMActionType {
    STATIC, // Static actions are actions that can be performed by predefined intents
    DYNAMIC, // Dynamic actions are actions that can be triggered by shortcuts or other found intents
    DISABLED, // Disabled actions are actions that are disabled by the user
    UNDEFINED // Undefined actions are actions that are not defined and will be processed by the LLM
}

enum LLMDataAccessCapability {
    CONTACTS,
    SHORTCUTS,
    APPS,
    NOTES,
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
}

public class LLMActions {
    private ArrayList<LLMAction> ACTIONS = new ArrayList<>();

    public LLMActions() {

        // NONE or ANSWER
        LLMDataAccessCapability[] noneCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE};
        ACTIONS.add(new LLMAction("ANSWER", "Just reply to user with answer and do no other activity.", LLMActionType.STATIC, new String[]{"smart_answer"}, noneCap));

        // CONTACTS
        LLMDataAccessCapability[] contactsCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.CONTACTS};
        ACTIONS.add(new LLMAction("CONTACTS_FIND", "Find a contact.", LLMActionType.STATIC, new String[]{"contact_name", "contact_phone"}, contactsCap));
        ACTIONS.add(new LLMAction("CONTACTS_CREATE", "Add a new contact.", LLMActionType.STATIC, new String[]{"contact_name", "contact_phone", "contact_nickname", "contact_email"}, contactsCap));
        ACTIONS.add(new LLMAction("CONTACTS_DELETE", "Delete a contact.", LLMActionType.STATIC, new String[]{"contact_name", "contact_phone"}, contactsCap));
        ACTIONS.add(new LLMAction("CONTACTS_EDIT", "Edit a contact.", LLMActionType.STATIC, new String[]{"original_contact_name", "original_contact_phone", "new_contact_name", "new_contact_phone", "new_contact_nickname", "new_contact_email"}, contactsCap));

        // EVENTS
        LLMDataAccessCapability[] eventsCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.CONTACTS, LLMDataAccessCapability.APPS, LLMDataAccessCapability.SHORTCUTS};
        ACTIONS.add(new LLMAction("EVENTS_CREATE", "Create a new event in calendar or user defined app.", LLMActionType.UNDEFINED, new String[]{"app_shortcut_id", "event_name", "event_location", "event_description", "event_start_time", "event_end_time", "all_day", "extra_emails"}, eventsCap));
        ACTIONS.add(new LLMAction("EVENTS_NEW_TASK", "Create a new task in calendar or user defined app.", LLMActionType.UNDEFINED, new String[]{"app_shortcut_id", "task_name", "task_location", "task_description", "task_start_time", "task_end_time"}, eventsCap));

        // CLOCK
        ACTIONS.add(new LLMAction("CLOCK_SET_ALARM", "Set an alarm.", LLMActionType.STATIC, new String[]{"alarm_name_msg", "alarm_time_hour", "alarm_time_minutes", "alarm_days"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_NEW_TIMER", "Create a new timer.", LLMActionType.STATIC, new String[]{"timer_name_msg", "timer_time_hour", "timer_time_minutes", "timer_days"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_DISMISS_TIMER", "Dismiss a timer.", LLMActionType.STATIC, new String[]{"timer_name_msg"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_NEW_STOPWATCH", "Create a new stopwatch.", LLMActionType.STATIC, new String[]{"stopwatch_name_msg", "stopwatch_time_hour", "stopwatch_time_minutes", "stopwatch_days"}, noneCap));

        // NAVIGATE
        LLMDataAccessCapability[] appShortcutCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.SHORTCUTS, LLMDataAccessCapability.APPS};
        ACTIONS.add(new LLMAction("NAVIGATE_TO", "Navigate to a location string address.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id", "location_name"}, appShortcutCap));
        ACTIONS.add(new LLMAction("NAVIGATE_GET_MY_LOCATION", "Get my current location lat and long.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("NAVIGATE_GET_MY_ADDRESS", "Get my current address.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));

        // CAMERA
        ACTIONS.add(new LLMAction("CAMERA_TAKE_PICTURE", "Take a picture.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_RECORD_VIDEO", "Record a video.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_TAKE_SELFIE", "Take a selfie.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_SCAN_QR_CODE", "Scan a QR code.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));

        // MESSAGING
        LLMDataAccessCapability[] messagingCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.APPS, LLMDataAccessCapability.SHORTCUTS, LLMDataAccessCapability.CONTACTS};
        ACTIONS.add(new LLMAction("MESSAGING_SEND_MESSAGE", "Send a message to a contact on a user defined app or shortcut.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "message"}, messagingCap));
        ACTIONS.add(new LLMAction("MESSAGING_SEND_EMAIL", "Send an email to a contact on a users default app.", LLMActionType.STATIC, new String[]{"contact_name", "contact_email", "email_subject", "email_body"}, messagingCap));
        ACTIONS.add(new LLMAction("MESSAGING_SEND_SMS", "Send an SMS to a contact on a user on a default sms app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "sms_body"}, messagingCap));

        // NOTES
        LLMDataAccessCapability[] notesCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.NOTES};
        ACTIONS.add(new LLMAction("NOTES_FIND", "Find a note.", LLMActionType.DYNAMIC, new String[]{"note_query"}, notesCap));
        ACTIONS.add(new LLMAction("NOTES_CREATE", "Create a new note.", LLMActionType.DYNAMIC, new String[]{"note_content", "note_tags"}, notesCap));
        ACTIONS.add(new LLMAction("NOTES_DELETE", "Delete a note.", LLMActionType.DYNAMIC, new String[]{"note_id"}, notesCap));
        ACTIONS.add(new LLMAction("NOTES_EDIT", "Edit a note.", LLMActionType.DYNAMIC, new String[]{"note_id", "note_content", "note_tags"}, notesCap));

        // WEB
        ACTIONS.add(new LLMAction("WEB_SEARCH_OPEN_UI", "Open web search.", LLMActionType.STATIC, new String[]{"search_query"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("WEB_SEARCH_TOP_RESULTS", "Search for a query on the web and return top results.", LLMActionType.STATIC, new String[]{"search_query"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("WEB_SEARCH_WIKI_INFO", "Search for a query on the web and return wiki info.", LLMActionType.STATIC, new String[]{"search_query"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("WEB_GET_URL_CONTENT", "Get the content of a URL.", LLMActionType.STATIC, new String[]{"url"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));

        // Clipboard
        ACTIONS.add(new LLMAction("CLIPBOARD_SET_TEXT", "Copy text to clipboard.", LLMActionType.STATIC, new String[]{"text"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("CLIPBOARD_GET_TEXT", "Get text from clipboard.", LLMActionType.STATIC, new String[]{}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("CLIPBOARD_CLEAR", "Clear clipboard.", LLMActionType.STATIC, new String[]{}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));

        // MEMORY - long-term AI memory
        ACTIONS.add(new LLMAction("MEMORY_ADD", "Add a value to memory.", LLMActionType.STATIC, new String[]{"key", "value", "tags"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("MEMORY_GET", "Get a value from memory.", LLMActionType.STATIC, new String[]{"key"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("MEMORY_DELETE", "Delete a value from memory.", LLMActionType.STATIC, new String[]{"key"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("MEMORY_FIND", "Find a value in memory.", LLMActionType.STATIC, new String[]{"query"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));
        ACTIONS.add(new LLMAction("MEMORY_UPDATE", "Update a value in memory.", LLMActionType.STATIC, new String[]{"key", "value", "tags"}, new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE}));

        // ORDERS via shortcuts
        ACTIONS.add(new LLMAction("ORDERS_CREATE_PRODUCT", "Create an order for product.", LLMActionType.UNDEFINED, new String[]{"product_query"}, appShortcutCap));
        ACTIONS.add(new LLMAction("ORDERS_CREATE_FOOD", "Create an order for food.", LLMActionType.UNDEFINED, new String[]{"food_query"}, appShortcutCap));
        ACTIONS.add(new LLMAction("ORDERS_CREATE_TAXI", "Create an order for taxi.", LLMActionType.UNDEFINED, new String[]{"taxi_query"}, appShortcutCap));

        // NOTIFICATIONS
        ACTIONS.add(new LLMAction("NOTIFICATIONS_GET", "Get notifications.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("NOTIFICATIONS_CLEAR", "Clear notifications.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("NOTIFICATIONS_SEND_SELF", "Send a notification.", LLMActionType.STATIC, new String[]{"notification_title", "notification_body", "intent_package", "intent_action", "intent_extra_input_value"}, noneCap));

        // MUSIC
        ACTIONS.add(new LLMAction("MUSIC_PLAY", "Play music.", LLMActionType.UNDEFINED, new String[]{"music_name_or_style"}, appShortcutCap));
        ACTIONS.add(new LLMAction("MUSIC_PAUSE", "Pause music.", LLMActionType.UNDEFINED, new String[]{}, appShortcutCap));
        ACTIONS.add(new LLMAction("MUSIC_SEARCH", "Search for music.", LLMActionType.UNDEFINED, new String[]{"music_query"}, appShortcutCap));

        // VOICE IO
        ACTIONS.add(new LLMAction("VOICE_SAY_USER", "Say something to user.", LLMActionType.STATIC, new String[]{"voice_message"}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_SAY_DEVICE", "Say something to device or caller.", LLMActionType.STATIC, new String[]{"voice_message"}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_LISTEN", "Listen to caller.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_LISTEN_FOR", "Listen to caller for a given amount of time.", LLMActionType.STATIC, new String[]{"timeout_in_ms"}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_STOP_LISTEN", "Stop listening to caller.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("VOICE_WAIT_FOR_KEYWORD", "Wait for caller to say a given keyword.", LLMActionType.STATIC, new String[]{"keyword"}, noneCap));

        // USER INPUT
        ACTIONS.add(new LLMAction("USER_INPUT", "Get user input text, boolean or number.", LLMActionType.STATIC, new String[]{"input_type"}, noneCap));

        // CALLING
        ACTIONS.add(new LLMAction("PHONE_CALL", "Call a phone number.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone"}, noneCap));
        ACTIONS.add(new LLMAction("PHONE_CALL_WITH_MESSAGE", "Call a phone number with a message.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "message"}, noneCap));
        ACTIONS.add(new LLMAction("PHONE_HANGUP", "Hang up a phone call.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("PHONE_PICK_UP", "Pick up a phone call.", LLMActionType.STATIC, new String[]{}, noneCap));

        // APPS & SHORTCUTS
        LLMDataAccessCapability[] appCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.APPS};
        ACTIONS.add(new LLMAction("APP_OPEN", "Open an app.", LLMActionType.STATIC, new String[]{"app_id"}, appCap));
        ACTIONS.add(new LLMAction("APP_CLOSE", "Close an app.", LLMActionType.STATIC, new String[]{"app_id"}, appCap));
        ACTIONS.add(new LLMAction("APP_LAUNCH_SHORTCUT_OR_APP", "Launch a shortcut or app.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("APP_LAUNCH_SHORTCUT_OR_APP_WITH_PARAMS", "Launch a shortcut or app with extra parameters.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "extra_input_values"}, appShortcutCap));
        // add accessability actions to control apps or shortcuts
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
        ACTIONS.add(new LLMAction("UTILS_WAIT_FOR", "Wait for a given condition to be true.", LLMActionType.STATIC, new String[]{"condition", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_WAIT_FOR_DATE", "Wait for a given date to be reached.", LLMActionType.STATIC, new String[]{"day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_ON_DATE", "Repeat an action on a given date.", LLMActionType.STATIC, new String[]{"action", "day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_UNTIL", "Repeat an action until a given condition is true.", LLMActionType.STATIC, new String[]{"action", "condition", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_UNTIL_DATE", "Repeat an action until a given date is reached.", LLMActionType.STATIC, new String[]{"action", "day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));

        ACTIONS.add(new LLMAction("UTILS_CLEANUP_MEMORY", "Cleanup memory.", LLMActionType.STATIC, new String[]{}, noneCap));

    }
}

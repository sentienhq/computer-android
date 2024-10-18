package fr.neamar.kiss.sentien.llm;

import java.util.ArrayList;

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
}

public class LLMActionList {
    private ArrayList<LLMAction> ACTIONS = new ArrayList<>();

    public LLMActionList() {

        // NONE or ANSWER
        LLMDataAccessCapability[] noneCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.NONE};
        ACTIONS.add(new LLMAction("ANSWER", "Just reply to user with answer and do no other activity.", LLMActionType.STATIC, new String[]{"smart_answer"}, noneCap));

        // GET CAPABILITIES
        // for example, to get the capabilities of the user's contacts send capabilityType="CONTACTS" and capabilityQuery="imrich" for specific subjects
        // to get the capabilities of the user's notes send capabilityType="NOTES" and capabilityQuery="imrich, work, personal" for specific subjects (note: the subjects are comma separated) and filtered by the user's tags and content
        // to get all contacts send capabilityType="CONTACTS" and capabilityQuery="*" (avoid it as much as possible)
        // it is recommended to send specific capabilityQuery to get more specific data and save the result in memory
        ACTIONS.add(new LLMAction("GET_CAPABILITY", "Get the required data capabilities for the LLM to make decisions.", LLMActionType.STATIC, new String[]{"capabilityType", "capabilityQuery"}, noneCap));

        // WORKING MEMORY
        // working memory is a temporary storage for data that is not saved to the next prompt
        ACTIONS.add(new LLMAction("WORKING_MEMORY_CLEAR", "Clear working memory.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("WORKING_MEMORY_SAVE", "Save data to working memory.", LLMActionType.STATIC, new String[]{"data"}, noneCap));

        // CONTACTS
        LLMDataAccessCapability[] contactsCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.CONTACTS};
        ACTIONS.add(new LLMAction("CONTACTS_FIND", "Find a contact.", LLMActionType.STATIC, new String[]{"contact_query"}, noneCap));
        ACTIONS.add(new LLMAction("CONTACTS_CREATE", "Add a new contact.", LLMActionType.STATIC, new String[]{"contact_name", "contact_phone", "contact_nickname", "contact_email"}, noneCap));
        ACTIONS.add(new LLMAction("CONTACTS_DELETE", "Delete a contact.", LLMActionType.STATIC, new String[]{"contact_id", "contact_name", "contact_phone"}, contactsCap));
        ACTIONS.add(new LLMAction("CONTACTS_EDIT", "Edit a contact.", LLMActionType.STATIC, new String[]{"original_contact_name", "original_contact_phone", "new_contact_name", "new_contact_phone", "new_contact_nickname", "new_contact_email"}, contactsCap));

        // EVENTS
        LLMDataAccessCapability[] appShortcutContactsCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.CONTACTS, LLMDataAccessCapability.APPS, LLMDataAccessCapability.SHORTCUTS};
        ACTIONS.add(new LLMAction("EVENTS_CREATE", "Create a new event in calendar or user defined app.", LLMActionType.DYNAMIC, new String[]{"app_shortcut_id", "event_name", "event_location", "event_description", "event_start_time", "event_end_time", "all_day", "extra_emails"}, appShortcutContactsCap));
        ACTIONS.add(new LLMAction("EVENTS_NEW_TASK", "Create a new task in calendar or user defined app.", LLMActionType.DYNAMIC, new String[]{"app_shortcut_id", "task_name", "task_location", "task_description", "task_start_time", "task_end_time"}, appShortcutContactsCap));

        // CLOCK
        ACTIONS.add(new LLMAction("CLOCK_SET_ALARM", "Set an alarm.", LLMActionType.STATIC, new String[]{"alarm_name_msg", "alarm_time_hour", "alarm_time_minutes", "alarm_days"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_NEW_TIMER", "Create a new timer.", LLMActionType.STATIC, new String[]{"timer_name_msg", "timer_time_hour", "timer_time_minutes", "timer_days"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_DISMISS_TIMER", "Dismiss a timer.", LLMActionType.STATIC, new String[]{"timer_name_msg"}, noneCap));
        ACTIONS.add(new LLMAction("CLOCK_NEW_STOPWATCH", "Create a new stopwatch.", LLMActionType.STATIC, new String[]{"stopwatch_name_msg", "stopwatch_time_hour", "stopwatch_time_minutes", "stopwatch_days"}, noneCap));

        // NAVIGATION
        LLMDataAccessCapability[] appShortcutCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.SHORTCUTS, LLMDataAccessCapability.APPS};
        ACTIONS.add(new LLMAction("NAVIGATION_TO", "Navigate to a location string address.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id", "location_name"}, appShortcutCap));
        ACTIONS.add(new LLMAction("NAVIGATION_GET_MY_LOCATION", "Get my current location lat and long.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("NAVIGATION_GET_MY_ADDRESS", "Get my current address.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        // find nearby places
        // possible categories: restaurants, hotels, tourist_attractions, gas_stations, parking_garages, shopping_centers, entertainment, sports, religious_sites, bars, nightlife, museums, movie_theaters, amusement_parks, art_galleries, music_venues, financial_services, healthcare, retail, transportation, education, government, libraries, fitness, religion, history,
        ACTIONS.add(new LLMAction("NAVIGATION_FIND_NEARBY_CATEGORY", "Find nearby category.", LLMActionType.STATIC, new String[]{"category_name"}, noneCap));

        // CAMERA
        ACTIONS.add(new LLMAction("CAMERA_TAKE_PICTURE", "Take a picture.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_RECORD_VIDEO", "Record a video.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_TAKE_SELFIE", "Take a selfie.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("CAMERA_SCAN_QR_CODE", "Scan a QR code.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));

        // MESSAGING
        ACTIONS.add(new LLMAction("MESSAGING_SEND_MESSAGE", "Send a message to a contact on a user defined app or shortcut.", LLMActionType.DYNAMIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "message"}, appShortcutContactsCap));
        ACTIONS.add(new LLMAction("MESSAGING_SEND_EMAIL", "Send an email to a contact on a users default app.", LLMActionType.STATIC, new String[]{"contact_name", "contact_email", "email_subject", "email_body"}, contactsCap));
        ACTIONS.add(new LLMAction("MESSAGING_SEND_SMS", "Send an SMS to a contact on a user on a default sms app or shortcut.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "sms_body"}, contactsCap));

        // NOTES
        LLMDataAccessCapability[] notesCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.NOTES};
        ACTIONS.add(new LLMAction("NOTES_FIND", "Find a similar notes.", LLMActionType.STATIC, new String[]{"note_queries[]"}, notesCap));
        ACTIONS.add(new LLMAction("NOTES_CREATE", "Create a new note.", LLMActionType.STATIC, new String[]{"note_content", "note_tags"}, noneCap));
        ACTIONS.add(new LLMAction("NOTES_DELETE", "Delete a note.", LLMActionType.STATIC, new String[]{"note_id"}, notesCap));
        ACTIONS.add(new LLMAction("NOTES_EDIT", "Edit a note.", LLMActionType.STATIC, new String[]{"note_id", "note_content", "note_tags"}, notesCap));

        // WEB
        // TODO - test this to make sure it works
        ACTIONS.add(new LLMAction("WEB_SEARCH_OPEN_UI", "Open web search.", LLMActionType.STATIC, new String[]{"search_query"}, noneCap));
        ACTIONS.add(new LLMAction("WEB_OPEN_URL", "Open a URL.", LLMActionType.STATIC, new String[]{"url"}, noneCap));
        ACTIONS.add(new LLMAction("WEB_SEARCH_TOP_RESULTS", "Search for a query on the web and return top results.", LLMActionType.STATIC, new String[]{"search_query"}, noneCap));
        ACTIONS.add(new LLMAction("WEB_SEARCH_WIKI_INFO", "Search for a query on the web and return wiki info.", LLMActionType.STATIC, new String[]{"search_query"}, noneCap));
        ACTIONS.add(new LLMAction("WEB_GET_URL_CONTENT", "Get the content of a URL.", LLMActionType.STATIC, new String[]{"url"}, noneCap));

        // Clipboard
        ACTIONS.add(new LLMAction("CLIPBOARD_SET_TEXT", "Copy text to clipboard.", LLMActionType.STATIC, new String[]{"text"}, noneCap));
        ACTIONS.add(new LLMAction("CLIPBOARD_GET_TEXT", "Get text from clipboard.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("CLIPBOARD_CLEAR", "Clear clipboard.", LLMActionType.STATIC, new String[]{}, noneCap));

        // MEMORY - long-term AI memory
        // notes consists of content, type, parentId, childIds, timestamp, tags and types can be notes, aiConvo or aiMemory
        ACTIONS.add(new LLMAction("MEMORY_ADD", "Add a value to memory.", LLMActionType.STATIC, new String[]{"key", "value", "tags"}, noneCap));
        ACTIONS.add(new LLMAction("MEMORY_GET", "Get a value from memory.", LLMActionType.STATIC, new String[]{"key"}, notesCap));
        ACTIONS.add(new LLMAction("MEMORY_DELETE", "Delete a value from memory.", LLMActionType.STATIC, new String[]{"key"}, notesCap));
        ACTIONS.add(new LLMAction("MEMORY_FIND", "Find a value in memory.", LLMActionType.STATIC, new String[]{"query"}, notesCap));
        ACTIONS.add(new LLMAction("MEMORY_UPDATE", "Update a value in memory.", LLMActionType.STATIC, new String[]{"key", "value", "tags"}, notesCap));

        // ORDERS via shortcuts
        ACTIONS.add(new LLMAction("ORDERS_CREATE_PRODUCT", "Create an order for product.", LLMActionType.DYNAMIC, new String[]{"product_query"}, appShortcutCap));
        ACTIONS.add(new LLMAction("ORDERS_CREATE_FOOD", "Create an order for food.", LLMActionType.DYNAMIC, new String[]{"food_query"}, appShortcutCap));
        ACTIONS.add(new LLMAction("ORDERS_CREATE_TAXI", "Create an order for taxi.", LLMActionType.DYNAMIC, new String[]{"taxi_query"}, appShortcutCap));

        // NOTIFICATIONS
        ACTIONS.add(new LLMAction("NOTIFICATIONS_GET", "Get notifications.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("NOTIFICATIONS_CLEAR", "Clear notifications.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("NOTIFICATIONS_SEND_SELF", "Send a notification.", LLMActionType.STATIC, new String[]{"notification_title", "notification_body", "intent_package", "intent_action", "intent_extra_input_value"}, noneCap));

        // MUSIC
        ACTIONS.add(new LLMAction("MUSIC_PLAY", "Play music.", LLMActionType.DYNAMIC, new String[]{"music_name_or_style"}, appShortcutCap));
        ACTIONS.add(new LLMAction("MUSIC_PAUSE", "Pause music.", LLMActionType.DYNAMIC, new String[]{}, appShortcutCap));
        ACTIONS.add(new LLMAction("MUSIC_SEARCH", "Search for music.", LLMActionType.DYNAMIC, new String[]{"music_query"}, appShortcutCap));

        // SOUND
        ACTIONS.add(new LLMAction("SOUND_GET_VOLUME", "Get sound volume level.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("SOUND_SET_VOLUME", "Set sound volume level.", LLMActionType.STATIC, new String[]{"volume_level"}, noneCap));
        ACTIONS.add(new LLMAction("SOUND_MUTE", "Mute sound.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("SOUND_UNMUTE", "Unmute sound.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("SOUND_BEEP", "Make a beep sound.", LLMActionType.STATIC, new String[]{}, noneCap));

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
        ACTIONS.add(new LLMAction("PHONE_CALL", "Call a phone number.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone"}, appShortcutContactsCap));
        ACTIONS.add(new LLMAction("PHONE_CALL_WITH_MESSAGE", "Call a phone number with a message.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "contact_name", "contact_phone", "message"}, appShortcutContactsCap));
        ACTIONS.add(new LLMAction("PHONE_HANGUP", "Hang up a phone call.", LLMActionType.STATIC, new String[]{}, noneCap));
        ACTIONS.add(new LLMAction("PHONE_PICK_UP", "Pick up a phone call.", LLMActionType.STATIC, new String[]{}, noneCap));

        // APPS & SHORTCUTS
        LLMDataAccessCapability[] appCap = new LLMDataAccessCapability[]{LLMDataAccessCapability.APPS};
        ACTIONS.add(new LLMAction("APP_OPEN", "Open an app.", LLMActionType.STATIC, new String[]{"app_id"}, appCap));
        ACTIONS.add(new LLMAction("APP_CLOSE", "Close an app.", LLMActionType.STATIC, new String[]{"app_id"}, appCap));
        ACTIONS.add(new LLMAction("APP_LAUNCH_SHORTCUT_OR_APP", "Launch a shortcut or app.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id"}, appShortcutCap));
        ACTIONS.add(new LLMAction("APP_LAUNCH_SHORTCUT_OR_APP_WITH_PARAMS", "Launch a shortcut or app with extra parameters.", LLMActionType.STATIC, new String[]{"app_or_shortcut_id", "extra_input_values"}, appShortcutCap));
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
        ACTIONS.add(new LLMAction("UTILS_WAIT_FOR", "Wait for a given condition to be true.", LLMActionType.STATIC, new String[]{"condition", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_WAIT_FOR_DATE", "Wait for a given date to be reached.", LLMActionType.STATIC, new String[]{"day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_ON_DATE", "Repeat an action on a given date.", LLMActionType.STATIC, new String[]{"action", "day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_UNTIL", "Repeat an action until a given condition is true.", LLMActionType.STATIC, new String[]{"action", "condition", "return_success", "timeout_in_ms", "return_error"}, noneCap));
        ACTIONS.add(new LLMAction("UTILS_REPEAT_UNTIL_DATE", "Repeat an action until a given date is reached.", LLMActionType.STATIC, new String[]{"action", "day", "month", "year", "hour", "minute", "second", "return_success", "timeout_in_ms", "return_error"}, noneCap));

        ACTIONS.add(new LLMAction("UTILS_CLEANUP_MEMORY", "Cleanup memory.", LLMActionType.STATIC, new String[]{}, noneCap));

    }
}

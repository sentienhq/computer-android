package fr.neamar.kiss.sentien;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactsPojo;

public class DataService {
    private static final String TAG = "\uD83D\uDCC1 DataService";
    private final DataHandler dataHandler;
    private final Gson gson;

    private String JSON_Contacts = "";
    private String JSON_ContactsHash = "";

    private String JSON_Apps = "";
    private String JSON_AppsHash = "";

    private String rootBlob = "";
    private String rootHash = "";

    public DataService(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        gson = new Gson();
        setupContacts();
        setupApps();
        rootBlob = calculateNewRootBlob();
    }

    public String getContactsJSON() {
        return JSON_Contacts;
    }

    public String getAppsJSON() {
        return JSON_Apps;
    }

    private void setupContacts() {
        if (dataHandler != null) {
            List<ContactsPojo> contacts = dataHandler.getContacts();
            JSON_Contacts = gson.toJson(contacts);
        }
    }

    private void setupApps() {
        if (dataHandler != null) {
            List<AppPojo> apps = dataHandler.getApplications();
            JSON_Apps = gson.toJson(apps);
        }
    }

    public String getRootBlob() {
        return rootBlob;
    }

    public String getRootHash() {
        return rootHash;
    }

    private String calculateNewRootBlob() {
        try {
            Map<String, String> jsonMap = new HashMap<>();
            JSON_ContactsHash = CryptoService.generateSHA256(JSON_Apps);
            jsonMap.put("apps", JSON_ContactsHash);
            JSON_AppsHash = CryptoService.generateSHA256(JSON_Contacts);
            jsonMap.put("contacts", JSON_AppsHash);
            String clipboardText = ClipboardService.getClipboardText();
            jsonMap.put("clipboardText", clipboardText);
            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(jsonMap);
            rootHash = CryptoService.generateSHA256(body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}

package fr.neamar.kiss.sentien;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactsPojo;

public class DataService {
    private final DataHandler dataHandler;
    private final Gson gson;
    private String JSON_Contacts = "";
    private String contactsSHA256 = "";
    private String JSON_Apps = "";
    private String appsSHA256 = "";

    public DataService(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        gson = new Gson();
        setupContacts();
        setupApps();

    }

    public String getContactsJSON() {
        return JSON_Contacts;
    }

    public String getContactsSHA256() {
        return contactsSHA256;
    }

    public String getAppsSHA256() {
        return appsSHA256;
    }

    public String getAppsJSON() {
        return JSON_Apps;
    }

    private void setupContacts() {
        if (dataHandler != null) {
            List<ContactsPojo> contacts = dataHandler.getContacts();
            JSON_Contacts = gson.toJson(contacts);
            contactsSHA256 = getSHA256(JSON_Contacts);
        }
    }

    private void setupApps() {
        if (dataHandler != null) {
            List<AppPojo> apps = dataHandler.getApplications();
            JSON_Apps = gson.toJson(apps);
            appsSHA256 = getSHA256(JSON_Apps);
        }
    }

    private String getSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashInBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}

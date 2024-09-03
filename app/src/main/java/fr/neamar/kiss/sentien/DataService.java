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
    private String JSON_Apps = "";

    public DataService(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        gson = new Gson();
        setupContacts();
        setupApps();

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
}

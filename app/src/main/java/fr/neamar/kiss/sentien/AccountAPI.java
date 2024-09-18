package fr.neamar.kiss.sentien;

import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AccountAPI {
    public static final MediaType JSON = MediaType.get("application/json");
    private static final String TAG = "\uD83C\uDF10 AccountAPI";
    private static final String API_URL = "http://10.0.0.27:8787/api";
    private static OkHttpClient client = new OkHttpClient();


    public static ApiResponse login(String userId, String deviceId, String rootHash, String signature) {
        try {
            final String method = "POST";
            Map<String, String> jsonMap = new HashMap<>();
            jsonMap.put("userId", userId);
            jsonMap.put("deviceId", deviceId);
            jsonMap.put("rootHash", rootHash);
            jsonMap.put("ed25519Signature", signature);
            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(jsonMap);
            Log.i(TAG, "Request body: " + body);
            RequestBody bodyRequest = RequestBody.create(body, JSON);
            Request request = new Request.Builder()
                    .url(API_URL + "/login")
                    .header("userId", userId)
                    .post(bodyRequest)
                    .build();
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                Log.e(TAG, "Response body is null");
                return null;
            }
            Log.i(TAG, "Response code: " + response.code());
            String responseString = responseBody.string();
            JSONObject jsonResponse = new JSONObject(responseString);
            int statusCode = response.code();
            boolean success = jsonResponse.getBoolean("success");
            Log.i(TAG, "Response: " + responseString);
            if (!success) {
                String error = jsonResponse.getString("error");
                return new ApiResponse(statusCode, false, "ERROR_" + error, jsonResponse);
            }
            return new ApiResponse(statusCode, true, "", jsonResponse);
        } catch (Exception e) {
            if (e instanceof IOException) {
                return new ApiResponse(-1, false, "CONNECTION_ERROR", null);
            }
            e.printStackTrace();
            return new ApiResponse(-1, false, "ERROR_" + e.getMessage(), null);
        }

    }

    public static ApiResponse registerAccount(String userId, String userAlias, String deviceId, String deviceAlias, String publicKey, String ed25519Signature) {
        try {
            final String method = "POST";
            Map<String, String> jsonMap = new HashMap<>();
            jsonMap.put("userId", userId);
            jsonMap.put("userAlias", userAlias);
            jsonMap.put("deviceId", deviceId);
            jsonMap.put("deviceAlias", deviceAlias);
            jsonMap.put("devicePubKey", publicKey);
            jsonMap.put("ed25519Signature", ed25519Signature);
            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(jsonMap);
            Log.i(TAG, "Request body: " + body);
            RequestBody bodyRequest = RequestBody.create(body, JSON);
            Request request = new Request.Builder()
                    .url(API_URL + "/register")
                    .header("userId", userId)
                    .post(bodyRequest)
                    .build();
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }
            Log.i(TAG, "Response code: " + response.code());
            String responseString = responseBody.string();
            JSONObject jsonResponse = new JSONObject(responseString);
            int statusCode = response.code();
            boolean success = jsonResponse.getBoolean("success");
            Log.i(TAG, "Response: " + responseString);
            if (!success) {
                String error = jsonResponse.getString("error");
                return new ApiResponse(statusCode, false, "ERROR_" + error, jsonResponse);
            }
            return new ApiResponse(statusCode, true, "", jsonResponse);
        } catch (Exception e) {
            if (e instanceof IOException) {
                return new ApiResponse(-1, false, "CONNECTION_ERROR", null);
            }
            e.printStackTrace();
            return new ApiResponse(-1, false, "ERROR_" + e.getMessage(), null);
        }

    }

    public static ApiResponse uploadFile(String userId, String deviceId, String token, String data, String fileName) {
        try {
            final String method = "PUT";
            Map<String, String> jsonMap = new HashMap<>();
            jsonMap.put("data", data);
            jsonMap.put("fileName", fileName);
            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(jsonMap);
            Log.i(TAG, "Request body: " + body);
            RequestBody bodyRequest = RequestBody.create(body, JSON);
            Request request = new Request.Builder()
                    .url(API_URL + "/upload")
                    .header("userId", userId)
                    .header("deviceId", deviceId)
                    .header("Authorization", "Bearer " + token)
                    .put(bodyRequest)
                    .build();
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                Log.e(TAG, "Response body is null");
                return null;
            }
            Log.i(TAG, "Response code: " + response.code());
            String responseString = responseBody.string();
            JSONObject jsonResponse = new JSONObject(responseString);
            int statusCode = response.code();
            boolean success = jsonResponse.getBoolean("success");
            Log.i(TAG, "Response: " + responseString);
            if (!success) {
                String error = jsonResponse.getString("error");
                return new ApiResponse(statusCode, false, "ERROR_" + error, jsonResponse);
            }
            return new ApiResponse(statusCode, true, "", jsonResponse);
        } catch (Exception e) {
            if (e instanceof IOException) {
                return new ApiResponse(-1, false, "CONNECTION_ERROR", null);
            }
            e.printStackTrace();
            return new ApiResponse(-1, false, "ERROR_" + e.getMessage(), null);
        }
    }

    public static class ApiResponse {
        public final int statusCode;
        public final boolean success;
        public final String error;
        public final JSONObject body;

        public ApiResponse(int statusCode, boolean success, String error, JSONObject body) {
            this.statusCode = statusCode;
            this.success = success;
            this.error = error;
            this.body = body;
        }
    }

}

package fr.neamar.kiss.sentien;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.UUID;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;
import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.keys.KeyPair;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;
import org.libsodium.jni.keys.SigningKey;
import org.libsodium.jni.keys.VerifyKey;


import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// TODO finish it -> https://gist.github.com/JosiasSena/3bf4ca59777f7dedcaf41a495d96d984
// https://medium.com/@hossein_kheirollahpour/implementing-authentication-with-jwt-in-android-without-using-a-library-5b01ea53a9e9

public class CryptoService {

    static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final String TAG = "\uD83D\uDD12 CryptoService";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String AES_KEY_ALIAS = "sentien_aes_key";
    private static final String PREFS_NAME = "ComputerModule:Prefs";
    private static final String USER_KEY_PREF = "encrypted_user_key";
    private static final String SYNC_KEY_PREF = "encrypted_sync_key";
    private static final String DEVICE_ID_PREF = "device_id";
    private static final String USER_ID_PREF = "user_id";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Context context;
    private final KeyStore keystore;
    private KeyPair userKeypair;
    private KeyPair syncKeypair;
    private String deviceId = "";
    private String userId = "";
    private boolean isInitialized = false;
    private boolean hasCredentials = false;

    CryptoService(Context context) {
        try {
            NaCl.sodium();
            this.context = context;
            keystore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keystore.load(null, null);

            if (!keystore.containsAlias(AES_KEY_ALIAS)) {
                generateLocalAESKey();
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            if (!prefs.contains(USER_KEY_PREF) || !prefs.contains(SYNC_KEY_PREF)) {
                Log.i(TAG, "No account found, creating new one");
                generateAndStoreKeys();
                hasCredentials = true;
            } else {
                Log.i(TAG, "Found account");
                loadExistingKeys();
                hasCredentials = true;
            }
            isInitialized = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void generateAndStoreKeys() throws Exception {
        byte[] userSeed = new Random().randomBytes(SodiumConstants.SECRETKEY_BYTES);
        byte[] syncSeed = new Random().randomBytes(SodiumConstants.SECRETKEY_BYTES);

        userKeypair = new KeyPair(userSeed);
        syncKeypair = new KeyPair(syncSeed);

        deviceId = "device_" + generateUUIDv4();
        userId = "user_" + bytesToHex(calculateHMAC(syncKeypair.getPrivateKey().toBytes(), "user_id".getBytes(), 4000)).substring(0, 18);

        byte[] encryptedUserKey = encryptData(userSeed);
        byte[] encryptedSyncKey = encryptData(syncSeed);

        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(USER_KEY_PREF, bytesToBase64(encryptedUserKey));
        editor.putString(SYNC_KEY_PREF, bytesToBase64(encryptedSyncKey));
        editor.putString(DEVICE_ID_PREF, deviceId);
        editor.putString(USER_ID_PREF, userId);

        editor.apply();

        Log.i(TAG, "Generated user public key: " + userKeypair.getPublicKey().toString());
        Log.i(TAG, "Generated sync public key: " + syncKeypair.getPublicKey().toString());
        Log.i(TAG, "Generated device id: " + deviceId);
        Log.i(TAG, "Generated user id: " + userId);
    }

    private void loadExistingKeys() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        byte[] encryptedUserKey = base64ToBytes(prefs.getString(USER_KEY_PREF, ""));
        byte[] encryptedSyncKey = base64ToBytes(prefs.getString(SYNC_KEY_PREF, ""));
        byte[] userSeed;
        byte[] syncSeed;
        try {
            userSeed = decryptData(encryptedUserKey);
            syncSeed = decryptData(encryptedSyncKey);
        } catch (Exception e) {
            // Key is corrupted, generate new ones
            Log.i(TAG, "Key is corrupted, generating new ones");
            generateLocalAESKey();
            generateAndStoreKeys();
            return;
        }

        userKeypair = new KeyPair(userSeed);
        syncKeypair = new KeyPair(syncSeed);

        userId = prefs.getString(USER_ID_PREF, "");
        deviceId = prefs.getString(DEVICE_ID_PREF, "");

        Log.i(TAG, "Loaded user public key: " + userKeypair.getPublicKey().toString());
        Log.i(TAG, "Loaded sync public key: " + syncKeypair.getPublicKey().toString());
        Log.i(TAG, "Loaded device id: " + deviceId);
        Log.i(TAG, "Loaded user id: " + userId);
    }

    public String bytesToBase64(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_WRAP | Base64.URL_SAFE);
    }

    public byte[] base64ToBytes(String inputString) {
        return Base64.decode(inputString, Base64.NO_WRAP | Base64.URL_SAFE);
    }

    public byte[] calculateHMAC(byte[] key, byte[] data, int iterations) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            for (int i = 0; i < iterations; i++) {
                mac.update(data);
            }
            return mac.doFinal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] encryptData(byte[] data) throws Exception {
        SecretKey secretKey = (SecretKey) keystore.getKey(AES_KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        byte[] encryptedData = cipher.doFinal(data);
        byte[] combined = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);
        return combined;
    }

    private byte[] decryptData(byte[] encryptedData) throws Exception {
        SecretKey secretKey = (SecretKey) keystore.getKey(AES_KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(128, encryptedData, 0, 12);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        return cipher.doFinal(encryptedData, 12, encryptedData.length - 12);
    }

    private void generateLocalAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        keyGenerator.generateKey();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean hasCredentials() {
        return hasCredentials;
    }

    public String generateRandomAESEncKey() {
        try {
            byte[] key = new Random().randomBytes(SodiumConstants.SECRETKEY_BYTES);
            return bytesToHex(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String generateUUIDv4() {
        return UUID.randomUUID().toString();
    }

    public Object generateRegistrationObject() {
        try {
            if (!isInitialized) {
                throw new RuntimeException("CryptoService is not initialized");
            }
            if (!hasCredentials) {
                throw new RuntimeException("No credentials found");
            }
            // Generate Ed25519 keypair from the sync seed
            byte[] publicKey = new byte[Sodium.crypto_sign_publickeybytes()];
            byte[] secretKey = new byte[Sodium.crypto_sign_secretkeybytes()];
            Sodium.crypto_sign_seed_keypair(publicKey, secretKey, syncKeypair.getPrivateKey().toBytes());
            final String signingMaterial = userId + ":" + deviceId + ":" + bytesToHex(publicKey);
            final byte[] signingMaterialBytes = signingMaterial.getBytes(StandardCharsets.UTF_8);

            byte[] signature = new byte[Sodium.crypto_sign_bytes()];
            int[] signatureLengthArray = new int[1];
            int result = Sodium.crypto_sign_detached(signature, signatureLengthArray, signingMaterialBytes, signingMaterialBytes.length, secretKey);
            if (result != 0) {
                throw new RuntimeException("Failed to sign message");
            }
            Log.i(TAG, "Signature in hex: " + bytesToHex(signature));

            result = Sodium.crypto_sign_verify_detached(signature, signingMaterialBytes, signingMaterialBytes.length, publicKey);
            if (result != 0) {
                throw new RuntimeException("Signature verification failed");
            }
            // return signed_message;
            return new Object();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

package fr.neamar.kiss.sentien;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

// TODO finish it -> https://gist.github.com/JosiasSena/3bf4ca59777f7dedcaf41a495d96d984
// https://medium.com/@hossein_kheirollahpour/implementing-authentication-with-jwt-in-android-without-using-a-library-5b01ea53a9e9

public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private KeyStore keystore;
    private byte[] encryption;
    private byte[] iv;

    CryptoService() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
        initKeyStore();
    }

    private void initKeyStore() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
        keystore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keystore.load(null);
    }
}

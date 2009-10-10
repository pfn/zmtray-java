package com.zimbra.app.systray;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Prefs {

    private final static String SYMMETRIC_ALGORITHM = "AES";
    private final static String SECRET_KEY = "secretKey";
    private final static Prefs INSTANCE;
    private final SecretKey key;
    private final Cipher cipher;

    static {
        try {
            INSTANCE = new Prefs();
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
    private final Preferences prefs;

    private Prefs() throws GeneralSecurityException {
        prefs = Preferences.userNodeForPackage(Prefs.class);

        byte[] keyBytes = prefs.getByteArray(SECRET_KEY, null);
        if (keyBytes == null) { // generate a secret key to encrypting passwords
            KeyGenerator kg = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
            SecretKey newKey = kg.generateKey();
            keyBytes = newKey.getEncoded();
            prefs.putByteArray(SECRET_KEY, keyBytes);
        }
        key = new SecretKeySpec(keyBytes, SYMMETRIC_ALGORITHM);
        cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
    }

    public static Prefs getPrefs() { return INSTANCE; }

    public String[] listAccounts() {
        try {
            return prefs.childrenNames();
        }
        catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getPassword(String account) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
        }
        catch (InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    public void setPassword(String account, String password) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        }
        catch (InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}

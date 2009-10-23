package com.zimbra.app.systray;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Prefs {

    private final static String SYMMETRIC_ALGORITHM = "AES";
    
    private final static String ACCOUNTS_KEY = "accounts";
    private final static String SECRET_KEY   = "secret";
    private final static String PORT_KEY     = "localPort";
    private final static String IPC_KEY      = "ipckey";
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

    public List<String> getAccountNames() {
        try {
            Preferences accounts = prefs.node(ACCOUNTS_KEY);
            String[] uuids = accounts.childrenNames();
            ArrayList<String> names = new ArrayList<String>();
            for (String id : uuids) {
                names.add(accounts.node(id).get(Account.NAME_KEY, null));
            }
            return names;
        }
        catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }

    public Account createAccount(String name) {
        String id = UUID.randomUUID().toString();
        Preferences accounts = prefs.node(ACCOUNTS_KEY);
        Account acct = new Account(accounts.node(id), cipher, key);

        acct.setAccountName(name);
        return acct;
    }
    
    public Account getAccount(String name) {
        Preferences accounts = prefs.node(ACCOUNTS_KEY);
        try {
            String[] uuids = accounts.childrenNames();
            for (String id : uuids) {
                if (name.equals(accounts.node(id).get(Account.NAME_KEY, null)))
                    return new Account(accounts.node(id), cipher, key);
            }
        }
        catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
    
    public int getPort() {
        return prefs.getInt(PORT_KEY, -1);
    }
    
    public void setPort(int port) {
        prefs.putInt(PORT_KEY, port);
    }
    
    public String getIPCKey() {
        return prefs.get(IPC_KEY, null);
    }
    
    public void setIPCKey(String key) {
        prefs.put(IPC_KEY, key);
    }
}

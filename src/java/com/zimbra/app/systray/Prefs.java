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

    public enum ScreenLocation {
        TOP_LEFT, TOP_RIGHT, CENTER, BOTTOM_RIGHT, BOTTOM_LEFT
    }
    
    private final static String SYMMETRIC_ALGORITHM = "AES";
    
    private final static String SOAP_DEBUG_KEY      = "soapDebug";
    private final static String ACCOUNTS_KEY        = "accounts";
    private final static String SECRET_KEY          = "secret";
    private final static String PORT_KEY            = "localPort";
    private final static String IPC_KEY             = "ipckey";
    private final static String APPT_SOUND_KEY      = "apptSound";
    private final static String MSG_SOUND_KEY       = "msgSound";
    private final static String DISABLED_SOUND_KEY  = "soundsDisabled";
    private final static String APPT_LOCATION_KEY   = "apptLocation";
    private final static String MSG_LOCATION_KEY    = "msgLocation";
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

    public void removeAccount(Account acct) {
        Preferences accounts = prefs.node(ACCOUNTS_KEY);
        try {
            accounts.node(acct.getId()).removeNode();
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
    
    public void setAppointmentSound(String file) {
        prefs.put(APPT_SOUND_KEY, file);
    }
    
    public String getAppointmentSound() {
        return prefs.get(APPT_SOUND_KEY, null);
    }
    
    public void setMessageSound(String file) {
        prefs.put(MSG_SOUND_KEY, file);
    }
    
    public String getMessageSound() {
        return prefs.get(MSG_SOUND_KEY, null);
    }
    
    public void setSoundDisabled(boolean b) {
        prefs.putBoolean(DISABLED_SOUND_KEY, b);
    }
    
    public boolean isSoundDisabled() {
        return prefs.getBoolean(DISABLED_SOUND_KEY, false);
    }
    
    public void setSoapDebug(boolean b) {
        prefs.putBoolean(SOAP_DEBUG_KEY, b);
    }
    
    public boolean getSoapDebug() {
        return prefs.getBoolean(SOAP_DEBUG_KEY, false);
    }
    
    public void setAppointmentAlertLocation(ScreenLocation l) {
        prefs.putInt(APPT_LOCATION_KEY, l.ordinal());
    }
    
    public ScreenLocation getAppointmentAlertLocation() {
        int i = prefs.getInt(
                APPT_LOCATION_KEY, ScreenLocation.CENTER.ordinal());
        return ScreenLocation.values()[i];
    }
    
    public void setMessageAlertLocation(ScreenLocation l) {
        prefs.putInt(MSG_LOCATION_KEY, l.ordinal());
    }
    
    public ScreenLocation getMessageAlertLocation() {
        int i = prefs.getInt(
                MSG_LOCATION_KEY, ScreenLocation.BOTTOM_RIGHT.ordinal());
        return ScreenLocation.values()[i];
    }
}

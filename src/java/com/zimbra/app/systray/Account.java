package com.zimbra.app.systray;

import java.util.prefs.Preferences;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class Account {
    private final Cipher cipher;
    private final SecretKey key;
    private final Preferences prefs;
    
    private final static String PREAUTH_URI =
        "/service/preauth?isredirect=1&authtoken=";
    public final static String SERVICE_URI = "/service/soap";

    private final static String ENABLED_KEY   = "enabled";
    private final static String SERVER_KEY    = "server";
    private final static String SECURE_KEY    = "ssl";
    private final static String CERT_KEY      = "certificate";
    private final static String FOLDERS_KEY   = "folders";
    private final static String CALENDARS_KEY = "calendars";
    private final static String ICON_KEY      = "icon";
    private final static String SOUND_KEY     = "sound";
    //private final static String NAME_KEY    = "name";
    private final static String PASS_KEY      = "password";
    private final static String SALT_KEY      = "salt";
    private final static String USER_KEY      = "login";
    
    Account(Preferences prefs, Cipher cipher, SecretKey key) {
        this.prefs = prefs;
        this.cipher = cipher;
        this.key = key;
    }

    public void setServer(String server) {
        prefs.put(SERVER_KEY, server);
    }

    public boolean isSSL() {
        return prefs.getBoolean(SECURE_KEY, false);
    }

    public void setSSL(boolean ssl) {
        prefs.putBoolean(SECURE_KEY, ssl);
    }

    public String getServer() {
        return prefs.get(SERVER_KEY, null);
    }

    public String getAccountName() {
        return prefs.name();
    }

    public void setAccountName(String name) {
        //prefs.put(NAME_KEY, name);
        throw new UnsupportedOperationException("setAccountName");
    }

    public List<String> getSubscribedMailFolders() {
        return getSubscribedNames(FOLDERS_KEY);
    }

    public void setSubscribedMailFolders(List<String> names) {
        setSubscribedNames(FOLDERS_KEY, names);
    }
    
    public List<String> getSubscribedCalendarNames() {
        return getSubscribedNames(CALENDARS_KEY);
    }

    public void setSubscribedCalendarNames(List<String> names) {
        setSubscribedNames(CALENDARS_KEY, names);
    }
    
    private List<String> getSubscribedNames(String key) {
        String names = prefs.get(key, null);
        ArrayList<String> nameList = new ArrayList<String>();
        if (names != null && !"".equals(names))
            nameList.addAll(Arrays.asList(names.split(",")));
        return nameList;
    }

    private void setSubscribedNames(String key, List<String> names) {
        StringBuilder namebuf = new StringBuilder();

        for (String name : names) {
            namebuf.append(name);
            namebuf.append(",");
        }
        namebuf.setLength(namebuf.length() - 1);

        if (namebuf.length() > 0)
            prefs.put(key, namebuf.toString());
    }

    public void setLogin(String login) {
        prefs.put(USER_KEY, login);
    }

    public String getLogin() {
        return prefs.get(USER_KEY, null);
    }

    public String getPassword() {
        try {
            byte[] pwbuf = prefs.getByteArray(PASS_KEY, null);
            if (pwbuf == null)
                return null;

            cipher.init(Cipher.DECRYPT_MODE, key);
            pwbuf = cipher.doFinal(pwbuf);
            String salt = prefs.get(SALT_KEY, null);
            if (salt == null)
                throw new IllegalStateException("no salt");
            
            String saltpw = new String(pwbuf, "utf-8");
            
            if (!saltpw.startsWith(salt + ":")) {
                throw new IllegalStateException("salt mismatch: "
                        + saltpw + " expected: " + salt);
            }
            return saltpw.substring(saltpw.indexOf(":") + 1);
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setPassword(String password) {
        try {
            String salt = prefs.get(SALT_KEY, null);
            if (salt == null) {
                salt = UUID.randomUUID().toString();
                prefs.put(SALT_KEY, salt);
            }
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] pwbuf = (salt + ":" + password).getBytes("utf-8");
            pwbuf = cipher.doFinal(pwbuf);
            prefs.putByteArray(PASS_KEY, pwbuf);
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public X509Certificate getCertificate() {
        byte[] certbuf = prefs.getByteArray(CERT_KEY, null);
        if (certbuf == null) return null;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(certbuf));
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setCertificate(X509Certificate cert) {
        try {
            prefs.putByteArray(CERT_KEY, cert.getEncoded());
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public String getIconName() {
    	return prefs.get(ICON_KEY, null);
    }
    
    public void setIconName(String name) {
    	prefs.put(ICON_KEY, name);
    }
    
    public String getSoundName() {
    	return prefs.get(SOUND_KEY, null);
    }
    
    public void setSoundName(String name) {
    	prefs.put(SOUND_KEY, name);
    }
    
    public boolean isEnabled() {
        return prefs.getBoolean(ENABLED_KEY, false);
    }
    
    public void setEnabled(boolean e) {
        prefs.putBoolean(ENABLED_KEY, e);
    }
    
    public URL getServiceURL() {
        try {
            return getServiceURL(getServer(), isSSL());
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static URL getServiceURL(String server, boolean isSSL)
    throws MalformedURLException {
        return new URL((isSSL ? "https" : " http") + "://" +
                server + SERVICE_URI);
        
    }
    
    public URI getPreauthURI(String authToken) {
        try {
            return new URI((isSSL() ? "https" : "http") + "://"
                    + getServer() + PREAUTH_URI + authToken);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.zimbra.app.systray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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
    public  final static String NAME_KEY      = "name";
    private final static String PASS_KEY      = "password";
    private final static String SALT_KEY      = "salt";
    private final static String USER_KEY      = "login";
    
    private X509TrustManager accountTrustManager;
    
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

    public String getId() {
        return prefs.name();
    }

    public String getAccountName() {
        return prefs.get(NAME_KEY, null);
    }

    public void setAccountName(String name) {
        prefs.put(NAME_KEY, name);
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

            synchronized (cipher) {
                cipher.init(Cipher.DECRYPT_MODE, key);
                pwbuf = cipher.doFinal(pwbuf);
            }
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
            synchronized (cipher) {
                cipher.init(Cipher.ENCRYPT_MODE, key);

                byte[] pwbuf = (salt + ":" + password).getBytes("utf-8");
                pwbuf = cipher.doFinal(pwbuf);
                prefs.putByteArray(PASS_KEY, pwbuf);
            }
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
        accountTrustManager = null;
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
        return URI.create((isSSL() ? "https" : "http") + "://"
                + getServer() + PREAUTH_URI + authToken);
    }
    
    public URI getMessageUri(String authToken, Message m) {
        return URI.create(getPreauthURI(authToken).toString() +
                "&app=mail&id=" + m.getId());
    }

    public X509TrustManager getTrustManager() {
        X509Certificate cert = getCertificate();
        if (cert == null) return null;
        if (accountTrustManager != null)
            return accountTrustManager;
        
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry(getAccountName(), cert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            javax.net.ssl.TrustManager[] tm = tmf.getTrustManagers();
            if (tm.length == 0)
                throw new IllegalStateException("Can't create trust manager");
            accountTrustManager = (X509TrustManager) tm[0];
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        catch (CertificateException e) {
            throw new IllegalStateException(e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        return accountTrustManager;
    }
    
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        boolean equals = false;
        if (other instanceof Account) {
            equals = ((Account) other).getId().equals(getId());
        }
        return equals;
    }
}

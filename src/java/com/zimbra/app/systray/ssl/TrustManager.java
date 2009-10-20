package com.zimbra.app.systray.ssl;

import com.hanhuy.common.ui.ResourceBundleForm;

import com.zimbra.app.systray.Prefs;
import com.zimbra.app.systray.Account;
import com.zimbra.app.systray.ZimbraTray;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JOptionPane;

import java.io.IOException;
import java.util.UUID;
import java.util.ArrayList;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStore;
import java.security.KeyStoreException;

public class TrustManager extends ResourceBundleForm
implements X509TrustManager {
    private final X509TrustManager defaultTrustManager;
    private X509TrustManager ourTrustManager;
    private final ZimbraTray zmtray;
    private ArrayList<X509Certificate> sessionOnlyCerts =
            new ArrayList<X509Certificate>();

    public TrustManager(ZimbraTray zt) {
        zmtray = zt;
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            javax.net.ssl.TrustManager[] tm = tmf.getTrustManagers();
            if (tm.length == 0)
                throw new IllegalStateException("No trust managers found");
            defaultTrustManager = (X509TrustManager) tm[0];
            initOurTrustManager();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }
    }

    private void initOurTrustManager()
    throws NoSuchAlgorithmException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            ks.load(null, null);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        catch (CertificateException e) {
            throw new IllegalStateException(e);
        }
        Prefs prefs = Prefs.getPrefs();
        for (String name : prefs.getAccountNames()) {
            Account acct = prefs.getAccount(name);
            X509Certificate cert = acct.getCertificate();
            if (cert != null) {
                ks.setCertificateEntry(acct.getAccountName(), cert);
            }
        }
        for (X509Certificate cert : sessionOnlyCerts) {
            ks.setCertificateEntry(UUID.randomUUID().toString(), cert);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        javax.net.ssl.TrustManager[] tm = tmf.getTrustManagers();
        if (tm.length == 0)
            throw new IllegalStateException("No custom trust managers found");
        ourTrustManager = (X509TrustManager) tm[0];
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        throw new UnsupportedOperationException("checkClientTrusted");
    }

    public X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
    throws CertificateException {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType);
        }
        catch (CertificateException e) {
            if (chain == null || chain.length == 0)
                throw e;
            try {
                ourTrustManager.checkServerTrusted(chain, authType);
            }
            catch (CertificateException e2) {
                promptForCertificate(chain, e);
            }
            catch (RuntimeException e2) {
                promptForCertificate(chain, e);
            }
        }
    }

    private void promptForCertificate(
            X509Certificate[] chain, CertificateException e)
    throws CertificateException {

        String[] options = {
            getString("yesAlways"),
            getString("sessionOnly"),
            getString("never")
        };
        int r = JOptionPane.showOptionDialog(zmtray.HIDDEN_PARENT,
             format("certificateWarning"), getString("unknownCertificateTitle"),
             JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
             null, options, options[2]);
        switch (r) {
        case 0:
        case 1:
            sessionOnlyCerts.add(chain[0]);
            try {
                initOurTrustManager();
            }
            catch (KeyStoreException ex) {
                throw new IllegalStateException(ex);
            }
            catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException(ex);
            }
            break;
        case 2:
            throw e;
        }
    }
}

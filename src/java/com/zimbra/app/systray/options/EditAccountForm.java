package com.zimbra.app.systray.options;

import com.zimbra.app.systray.Account;
import com.zimbra.app.systray.ZimbraTray;
import com.zimbra.app.systray.Prefs;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.xml.soap.SOAPException;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;
import com.zimbra.app.soap.SOAPFaultException;
import com.zimbra.app.soap.SoapInterface;
import com.zimbra.app.soap.messages.AuthRequest;
import com.zimbra.app.soap.messages.AuthResponse;

public class EditAccountForm extends ResourceBundleForm {
    private JPanel panel;
    private final ZimbraTray zt;

    private JTextField     server = new JTextField();
    private JTextField     name   = new JTextField();
    private JTextField     user   = new JTextField();
    private JPasswordField pass   = new JPasswordField();
    private JCheckBox      http   = new JCheckBox();
    private JButton        ok     = new JButton();
    private JButton        test   = new JButton();
    private JButton        cancel = new JButton();
    
    private String serverName;
    private String accountName;
    private String username;
    private String password;
    private boolean isSSL;
    private boolean isAccountCreated;

    public EditAccountForm(ZimbraTray zt) {
        this.zt = zt;
        panel = new JPanel();
        layout();
    }

    public JPanel getComponent() {
        return panel;
    }

    private void layout() {
        panel.setLayout(createLayoutManager());

        panel.add(name,   "nameField");
        panel.add(server, "serverField");
        panel.add(user,   "userField");
        panel.add(pass,   "passField");
        panel.add(http,   "httpField");
        panel.add(test,   "testButton");
        panel.add(ok,     "okButton");
        panel.add(cancel, "cancelButton");
        
        test.addActionListener(new TestActionListener());
        ok.addActionListener(new SaveActionListener());

        final boolean[] nameSet = new boolean[1];

        final Runnable nameSetCheck = new Runnable() {
            public void run() {
                nameSet[0] = name.getText().length() > 0;
            }
        };
        final Runnable nameSetter = new Runnable() {
            public void run() {
                if (!nameSet[0])
                    name.setText(server.getText() + " - " + user.getText());
            }
        };
        name.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                EventQueue.invokeLater(nameSetCheck);
            }
        });

        KeyAdapter nameSetterListener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                EventQueue.invokeLater(nameSetter);
            }
        };
        user.addKeyListener(nameSetterListener);
        server.addKeyListener(nameSetterListener);
    }
    
    public boolean isAccountCreated() {
        return isAccountCreated;
    }

    public Account getAccount() {
        if (!isAccountCreated)
            return null;
        return Prefs.getPrefs().getAccount(accountName);
    }
    
    private class SaveActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!verifySettings())
                return;
            Account account = Prefs.getPrefs().getAccount(accountName);
            if (account != null) {
                JOptionPane.showMessageDialog(zt.HIDDEN_PARENT,
                        format("accountExistsError", accountName),
                        getString("errorString"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            account = Prefs.getPrefs().createAccount(accountName);
            account.setEnabled(true);
            account.setLogin(username);
            account.setPassword(password);
            account.setServer(serverName);
            account.setSSL(isSSL);
            isAccountCreated = true;
            //dialog.setVisible(false);
            //dialog.dispose();
        }
    }
    private class TestActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!verifySettings())
                return;
            JOptionPane pane = new JOptionPane(
                    getString("testingMessage"),
                    JOptionPane.INFORMATION_MESSAGE);
            pane.setOptions(new Object[] { "Cancel" });
            final JDialog dlg = pane.createDialog(zt.HIDDEN_PARENT,
                    getString("testingTitle"));
            dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dlg.setAlwaysOnTop(true);
            
            final boolean[] finished = new boolean[1];
            final String[] error = new String[1];
            Thread t = new Thread(new Runnable() {
                public void run() {
                    
                    AuthRequest req = new AuthRequest();
                    req.account = new AuthRequest.Account();
                    req.password = password;
                    req.account.by = "name";
                    req.account.name = username;
                    try {
                        SoapInterface.call(req, AuthResponse.class,
                                Account.getServiceURL(serverName, isSSL));
                    }
                    catch (SSLHandshakeException e) {
                        error[0] = getString("sslConnectionError") + " " +
                                e.getLocalizedMessage();
                    }
                    catch (IOException e) {
                        error[0] = getString("connectionError") + " " +
                                e.getLocalizedMessage();
                        e.printStackTrace();
                    }
                    catch (SOAPFaultException e) {
                        error[0] = e.reason.text;
                        e.printStackTrace();
                    }
                    catch (SOAPException e) {
                        error[0] = getString("soapResponseError") + " " +
                                e.getLocalizedMessage();
                        e.printStackTrace();
                    }
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            dlg.setVisible(false);
                        }
                    });
                    finished[0] = true;
                }
            }, getString("testingThreadName"));
            
            t.start();
            dlg.setVisible(true);
            if (finished[0]) {
                String message = getString("connectionTestSuccess");
                int type = JOptionPane.INFORMATION_MESSAGE;
                if (error[0] != null) {
                    message = error[0];
                    type = JOptionPane.ERROR_MESSAGE;
                }
                JOptionPane.showMessageDialog(zt.HIDDEN_PARENT,
                        message, getString("connectionTestString"), type);
            }
        }
    }
    
    private boolean verifySettings() {
        accountName = name.getText();
        serverName = server.getText();
        username = user.getText();
        password = new String(pass.getPassword());
        isSSL = !http.isSelected();
        
        String error = null;
        if (accountName == null || accountName.trim().equals(""))
            error = getString("accountNameEmptyError");
        if (serverName == null || serverName.trim().equals(""))
            error = getString("serverNameEmptyError");
        if (username == null || username.trim().equals(""))
            error = getString("usernameEmptyError");
        if (password == null || password.trim().equals(""))
            error = getString("passwordEmptyError");
        if (error != null) {
            JOptionPane.showMessageDialog(zt.HIDDEN_PARENT,
                    error, getString("errorString"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
}

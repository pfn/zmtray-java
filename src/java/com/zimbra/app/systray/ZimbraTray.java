package com.zimbra.app.systray;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.hanhuy.common.ui.DimensionEditor;
import com.hanhuy.common.ui.FontEditor;
import com.hanhuy.common.ui.IntEditor;
import com.hanhuy.common.ui.PointEditor;
import com.hanhuy.common.ui.ResourceBundleForm;

public class ZimbraTray extends ResourceBundleForm implements Runnable {
    private boolean hasSystemTray = false;
    
    private TrayIcon trayicon;
    private PopupMenu menu;
    
    private HashMap<String,MenuItem> accountMenuMap =
            new HashMap<String,MenuItem>();
    private HashMap<String,AccountHandler> accountHandlerMap =
            new HashMap<String,AccountHandler>();
    
    private final static int THREAD_POOL_SIZE = 20;
    
    private ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    public final JFrame HIDDEN_PARENT;
    
    private final OpenClientAction openClientAction = new OpenClientAction();
    
    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable t) {
                System.err.println(t.getClass().getName() + ": " +
                        thread.getName());
                t.printStackTrace();
            }
        });
        ZimbraTray zt = new ZimbraTray();
        initSSL(zt);
        PropertyEditorManager.registerEditor(Font.class, FontEditor.class);
        PropertyEditorManager.registerEditor(Dimension.class,
                DimensionEditor.class);
        PropertyEditorManager.registerEditor(Point.class, PointEditor.class);
        PropertyEditorManager.registerEditor(int.class, IntEditor.class);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        EventQueue.invokeLater(zt);
    }

    private static void initSSL(ZimbraTray zt) throws Exception {
        SSLContext ctx = SSLContext.getInstance("SSL");
        ctx.init(null, new TrustManager[] { new TrustManager(zt) }, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
    }
    
    public ZimbraTray() {
        checkIfRunning();

        ImageIcon icon = (ImageIcon) getIcon("emailIcon");
        HIDDEN_PARENT = new JFrame("zmtray hidden parent frame");
        HIDDEN_PARENT.setIconImage(icon.getImage());
    }

    public void run() {
        setupSystemTray();
        Prefs prefs = Prefs.getPrefs();

        List<String> names = prefs.getAccountNames();

        if (names.size() == 0) {
            // show configuration/add accounts dialog
            NewAccountForm form = new NewAccountForm(this);
            form.show();
            if (!form.isAccountCreated())
                System.exit(0);
            names = prefs.getAccountNames();
        }
        // perform logins and start polling
        for (String name : names) {
            Account account = prefs.getAccount(name);
            if (!account.isEnabled()) continue;
            AccountHandler handler = new AccountHandler(account, this);
            accountHandlerMap.put(name, handler);
            addAccountToTray(account);
        }
    }
    
    private void checkIfRunning() {
        TrayServer ts = new TrayServer();
        if (ts.checkIfRunning())
            System.exit(0);
        ts.start();
    }

    private void setupSystemTray() {
        SystemTray tray;
        
        try {
            tray = SystemTray.getSystemTray();
        }
        catch (NoClassDefFoundError e) {
            return;
        }
        if (!SystemTray.isSupported())
            return;
        
        hasSystemTray = true;
        
        MenuItem item;
        menu = new PopupMenu();
        menu.addSeparator();
        item = new MenuItem(getString("optionsMenu"));
        item.addActionListener(new OptionMenuAction());
        menu.add(item);
        menu.addSeparator();
        item = new MenuItem(getString("exitMenu"));
        item.addActionListener(new ExitAction());
        menu.add(item);
        
        ImageIcon icon = (ImageIcon) getIcon("emailIcon");
        trayicon = new TrayIcon(icon.getImage(),
                getString("defaultToolTip"), menu);
        trayicon.setImageAutoSize(true);
        try {
            tray.add(trayicon);
        }
        catch (AWTException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private void addAccountToTray(Account acct) {
        if (!hasSystemTray)
            return;
        String name = acct.getAccountName();
        MenuItem item = new MenuItem(name);
        item.setActionCommand(name);
        item.addActionListener(openClientAction);
        menu.insert(item, 0);
        accountMenuMap.put(name, item);
    }
    
    //private void removeAccountFromTray(Account acct) {
    //    if (!hasSystemTray)
    //        return;
    //}
    
    public void setTrayIcon(Image icon) {
        if (!hasSystemTray)
            return;
        trayicon.setImage(icon);
    }
    
    public void setTrayTip(String text) {
        if (!hasSystemTray)
            return;
        trayicon.setToolTip(text);
    }
    
    public void setTrayMessage(String title, String text) {
        if (!hasSystemTray)
            return;
        trayicon.displayMessage(title, text, TrayIcon.MessageType.NONE);
    }
    public void setTrayError(String title, String text) {
        if (!hasSystemTray)
            return;
        trayicon.displayMessage(title, text, TrayIcon.MessageType.ERROR);
    }
    
    private static class ExitAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }
    
    private class OpenClientAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!Desktop.isDesktopSupported()) {
                JOptionPane.showMessageDialog(HIDDEN_PARENT,
                        "java.awt.Desktop is not supported",
                        "Unable to open webclient",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            Desktop d = Desktop.getDesktop();
            String name = e.getActionCommand();
            AccountHandler h = accountHandlerMap.get(name);
            Account acct = h.getAccount();
            String authToken = h.getAuthToken();
            if (authToken == null) {
                JOptionPane.showMessageDialog(HIDDEN_PARENT,
                        format("notLoggedIn", name), getString("errorString"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                d.browse(acct.getPreauthURI(authToken));
            }
            catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
    
    ScheduledExecutorService getExecutor() {
        return executor;
    }

    private class OptionMenuAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            NewAccountForm form = new NewAccountForm(ZimbraTray.this);
            form.show();
            if (form.isAccountCreated()) {
                Account account = form.getAccount();
                String name = account.getAccountName();
                if (!account.isEnabled()) return;
                AccountHandler handler = new AccountHandler(
                        account, ZimbraTray.this);
                accountHandlerMap.put(name, handler);
                addAccountToTray(account);
            }
        }
    }
}

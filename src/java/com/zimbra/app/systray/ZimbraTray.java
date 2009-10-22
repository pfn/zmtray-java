package com.zimbra.app.systray;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.hanhuy.common.ui.DimensionEditor;
import com.hanhuy.common.ui.FontEditor;
import com.hanhuy.common.ui.IntEditor;
import com.hanhuy.common.ui.PointEditor;
import com.hanhuy.common.ui.ResourceBundleForm;

public class ZimbraTray extends ResourceBundleForm implements Runnable {
    private boolean hasSystemTray = false;
    
    private TrayIcon trayicon;
    private JPopupMenu menu;
    
    private final ImageIcon NORMAL_ICON;
    private final ImageIcon NEW_MAIL_ICON;
    
    private HashMap<String,JMenuItem> accountMenuMap =
            new HashMap<String,JMenuItem>();
    private HashMap<String,AccountHandler> accountHandlerMap =
            new HashMap<String,AccountHandler>();
    
    private HashMap<Account,List<Message>> newMessages =
            new HashMap<Account,List<Message>>();

    private HashMap<Account,Set<Appointment>> appointments =
            new HashMap<Account,Set<Appointment>>();

    // TODO tune this value, or make adjustable
    private final static int THREAD_POOL_SIZE = 5;
    
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

        NORMAL_ICON = (ImageIcon) getIcon("emailIcon");
        NEW_MAIL_ICON = (ImageIcon) getIcon("newEmailIcon");
        HIDDEN_PARENT = new JFrame("zmtray hidden parent frame");
        HIDDEN_PARENT.setIconImage(NORMAL_ICON.getImage());
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
        TrayServer ts = new TrayServer(this);
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
        
        JMenuItem item;
        menu = new JPopupMenu();
        menu.addSeparator();
        item = new JMenuItem(getString("optionsMenu"));
        item.addActionListener(new NewAccountMenuAction());
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(getString("exitMenu"));
        item.addActionListener(new ExitAction());
        menu.add(item);
        
        trayicon = new TrayIcon(NORMAL_ICON.getImage(), this);
        trayicon.setJPopupMenu(menu);
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
        JMenuItem item = new JMenuItem(name);
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
            List<Message> msgs = newMessages.get(acct);
            if (msgs != null)
                msgs.clear();
            
            updateTrayIcon();
            JMenuItem item = accountMenuMap.get(name);
            item.setText(name);
            
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
    
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    private class NewAccountMenuAction implements ActionListener {
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

    public void showNewMessages() {
        ArrayList<Object> list = new ArrayList<Object>();
        for (Account a : newMessages.keySet()) {
            List<Message> msgs = newMessages.get(a);
            if (msgs != null && msgs.size() > 0) {
                //if (newMessages.size() > 1)
                    list.add(a.getAccountName());
                for (Message m : msgs)
                    list.add(m);
            }
        }
        
        if (list.size() > 0)
            MessageListView.showView(this, list);
    }

    /**
     * Make sure that the notifier doesn't contain any read messages
     */
    public void updateUnreadMessages(Account account, List<Message> messages) {
        List<Message> msgs = newMessages.get(account);
        if (messages == null)
            messages = Collections.emptyList();
        if (msgs != null) {
            if (msgs.retainAll(messages))
                showNewMessages();
        }
        String name = account.getAccountName();
        JMenuItem item = accountMenuMap.get(name);
        if (item != null) {
            item.setText(msgs != null && msgs.size() > 0 ?
                    format("accountMessageCount", name, msgs.size()) : name);
        }
        updateTrayIcon();
    }

    public void newMessagesFound(Account account, List<Message> messages) {
        List<Message> msgs = newMessages.get(account);
        if (msgs == null) {
            msgs = new ArrayList<Message>();
            newMessages.put(account, msgs);
        }
        msgs.addAll(messages);
        JMenuItem item = accountMenuMap.get(account.getAccountName());
        if (item != null) {
            item.setText(format("accountMessageCount",
                    account.getAccountName(), msgs.size()));
        }
        showNewMessages();
        updateTrayIcon();
    }

    public void appointmentsFound(Account account,
            List<Appointment> appts) {
        if (!appointments.containsKey(account)) {
            appointments.put(account, new HashSet<Appointment>());
        }
        Set<Appointment> acctappts = appointments.get(account);
        // add new appointments
        for (Appointment a : appts) {
            if (!acctappts.contains(a)) {
                System.out.println("Adding new alarm for: " + a.getName());
                acctappts.add(a);
                a.createAlarm(this);
            }
        }
        
        // remove appointments dismissed elsewhere
        Set<Appointment> removedAppointments =
                new HashSet<Appointment>(acctappts);
        acctappts.retainAll(appts);
        removedAppointments.removeAll(acctappts);
        for (Appointment a : removedAppointments) {
            System.out.println("Cancelling alarm for: " + a.getName());
            a.cancelAlarm();
        }
    }
    
    public void pollNow() {
        for (AccountHandler h : accountHandlerMap.values()) {
            h.pollNow();
        }
    }

    public void updateTrayIcon() {
        boolean hasNew = false;
        for (List<Message> messages : newMessages.values()) {
            hasNew |= messages.size() > 0;
        }

        setTrayIcon(hasNew ? NEW_MAIL_ICON.getImage() : NORMAL_ICON.getImage());

        if (!hasNew)
            MessageListView.hideView();
    }
}

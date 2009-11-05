package com.zimbra.app.systray;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import com.apple.eawt.Application;
import com.hanhuy.common.ui.ConsoleViewer;
import com.hanhuy.common.ui.DimensionEditor;
import com.hanhuy.common.ui.FontEditor;
import com.hanhuy.common.ui.IntEditor;
import com.hanhuy.common.ui.PointEditor;
import com.hanhuy.common.ui.ResourceBundleForm;
import com.zimbra.app.soap.SoapInterface;
import com.zimbra.app.systray.options.OptionsDialog;

public class ZimbraTray extends ResourceBundleForm implements Runnable {
    private boolean hasSystemTray = false;
    private boolean suppressMailAlerts = false;
    private boolean showingNewAccountForm = false;
    
    private TrayIconSupport trayicon;
    private JPopupMenu menu;
    private PopupMenu awtMenu; // used for OSX dock menu
    
    private final ImageIcon NORMAL_ICON;
    private final ImageIcon NEW_MAIL_ICON;
    
    private HashMap<String,JMenuItem> accountMenuMap =
            new HashMap<String,JMenuItem>();
    private HashMap<String,MenuItem> accountAWTMenuMap =
            new HashMap<String,MenuItem>();
    private HashMap<String,AccountHandler> accountHandlerMap =
            new HashMap<String,AccountHandler>();
    
    private HashMap<Account,List<Message>> newMessages =
            new HashMap<Account,List<Message>>();

    private HashMap<Account,Set<Appointment>> appointments =
            new HashMap<Account,Set<Appointment>>();

    private final static int THREAD_POOL_SIZE = 5;
    
    private ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    public final JFrame HIDDEN_PARENT;
    
    private final OpenClientAction openClientAction = new OpenClientAction();
    
    public static void main(String[] args) throws Exception {
        final ZimbraTray zt = new ZimbraTray();
        zt.checkIfRunning();

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable t) {
                System.err.println( thread.getName() + ":" +
                        t.getClass().getName());
                t.printStackTrace();
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        new ConsoleViewer(zt.HIDDEN_PARENT);
                    }
                });
            }
        });
        System.setOut(ConsoleViewer.OUT);
        System.setErr(ConsoleViewer.OUT);
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
        NORMAL_ICON = (ImageIcon) getIcon("emailIcon");
        NEW_MAIL_ICON = (ImageIcon) getIcon("newEmailIcon");
        HIDDEN_PARENT = new JFrame("zmtray hidden parent frame");
        HIDDEN_PARENT.setIconImage(NORMAL_ICON.getImage());
    }

    public void run() {
        setupSystemTray();
        setupForOSX();
        Prefs prefs = Prefs.getPrefs();
        SoapInterface.setDebug(prefs.getSoapDebug());

        List<String> names = prefs.getAccountNames();

        if (names.size() == 0) {
            showingNewAccountForm = true;
            OptionsDialog.showNewAccountForm(this);
            showingNewAccountForm = false;
            names = prefs.getAccountNames();
            if (names.size() == 0) {
                JOptionPane.showMessageDialog(HIDDEN_PARENT,
                        getString("noAccountCreated"),
                        getString("errorString"), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        // perform logins and start polling
        int runningAccounts = 0;
        for (String name : names) {
            Account account = prefs.getAccount(name);
            if (!account.isEnabled()) continue;
            AccountHandler handler = new AccountHandler(account, this);
            accountHandlerMap.put(account.getId(), handler);
            addAccountToTray(account);
            runningAccounts++;
        }
        if (runningAccounts == 0)
            OptionsDialog.showForm(this);
    }
    
    private void setupForOSX() {
        String os = System.getProperty("os.name");
        if (!os.contains("Mac OS X"))
            return;
        Application app = Application.getApplication();
        app.addPreferencesMenuItem();
        app.setEnabledPreferencesMenu(true);
        OSXSupport.addApplicationAdapter(this, app);
        // Hack for java 1.6 on OSX
        try {
            Method m =  Application.class.getDeclaredMethod(
                    "setDockMenu", PopupMenu.class);
            m.invoke(app, awtMenu);
        }
        catch (NoSuchMethodException e) {
            System.out.println(
                    "Dock menus are not supported on this version of Java/OSX");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
        MenuItem awtItem;
        menu = new JPopupMenu();
        awtMenu = new PopupMenu();
        
        menu.addSeparator();
        awtMenu.addSeparator();
        
        PauseMailMenuAction pauseAction = new PauseMailMenuAction();
        item = new JCheckBoxMenuItem(getString("pauseMailMenu"));
        item.addActionListener(pauseAction);
        awtItem = new CheckboxMenuItem(getString("pauseMailMenu"));
        awtItem.addActionListener(pauseAction);
        awtMenu.add(awtItem);
        menu.add(item);
        
        OptionsMenuAction optionsAction = new OptionsMenuAction();
        item = new JMenuItem(getString("optionsMenu"));
        item.addActionListener(optionsAction);
        awtItem = new MenuItem(getString("optionsMenu"));
        awtItem.addActionListener(optionsAction);
        awtMenu.add(awtItem);
        menu.add(item);
        
        menu.addSeparator();
        awtMenu.addSeparator();
        
        item = new JMenuItem(getString("exitMenu"));
        item.addActionListener(new ExitAction());
        menu.add(item);
        
        // don't use exit on OSX, so instead we'll have a poll now option
        // in case we don't have a trayicon
        awtItem = new MenuItem(getString("pollNow"));
        awtItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pollNow();
            }
        });
        
        trayicon = new TrayIconSupport(NORMAL_ICON.getImage(), this);
        trayicon.setJPopupMenu(menu);
        trayicon.setToolTip(getString("defaultToolTip"));
        trayicon.setImageAutoSize(true);
        try {
            tray.add(trayicon.getTrayIcon());
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
        item.setActionCommand(acct.getId());
        item.addActionListener(openClientAction);
        
        MenuItem awtItem = new MenuItem(name);
        awtItem.setActionCommand(acct.getId());
        awtItem.addActionListener(openClientAction);
        menu.insert(item, 0);
        accountMenuMap.put(acct.getId(), item);
        
        awtMenu.insert(awtItem, 0);
        accountAWTMenuMap.put(acct.getId(), awtItem);
    }
    
    /**
     * deletes the account
     */
    public void removeAccount(Account acct) {
        updateUnreadMessages(acct, null);
        Prefs.getPrefs().removeAccount(acct);
        AccountHandler h = accountHandlerMap.remove(acct.getId());
        h.shutdown();
        removeAccountFromTray(acct);
    }
    
    public void startNewAccount(Account account) {
        if (showingNewAccountForm) return;
        if (!account.isEnabled()) return;
        AccountHandler handler = new AccountHandler(
                account, ZimbraTray.this);
        accountHandlerMap.put(account.getId(), handler);
        addAccountToTray(account);
    }

    public void resetAccount(Account acct) {
        AccountHandler h = accountHandlerMap.remove(acct.getId());
        h.shutdown();
        removeAccountFromTray(acct);
        newMessages.remove(acct);
        startNewAccount(Prefs.getPrefs().getAccount(acct.getAccountName()));
    }
    
    public void removeAccountFromTray(Account acct) {
        if (!hasSystemTray)
            return;
        menu.remove(accountMenuMap.get(acct.getId()));
        awtMenu.remove(accountAWTMenuMap.get(acct.getId()));
    }
    
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
    
    private static class ExitAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }
    
    private class OpenClientAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String id = e.getActionCommand();

            AccountHandler h = accountHandlerMap.get(id);
            Account acct = h.getAccount();
            openClient(acct, null);
        }
    }
    
    public void openClient(Account acct, Message m) {
        try {
            Class.forName("java.awt.Desktop");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(HIDDEN_PARENT,
                    getString("noJavaAwtDesktop"),
                    getString("cannotOpenWebClient"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(HIDDEN_PARENT,
                    getString("noJavaAwtDesktop"),
                    getString("cannotOpenWebClient"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Desktop d = Desktop.getDesktop();
        String name = acct.getAccountName();
        List<Message> msgs = newMessages.get(acct);
        if (m == null && msgs != null) {
            msgs.clear();
        } else if (m != null && msgs != null) {
            msgs.remove(m);
        }

        updateTrayIcon();
        if (m == null) {
            JMenuItem item = accountMenuMap.get(acct.getId());
            item.setText(name);
        } else
            updateUnreadMessages(acct, msgs);

        AccountHandler h = accountHandlerMap.get(acct.getId());
        String authToken = h.getAuthToken();
        if (authToken == null) {
            JOptionPane.showMessageDialog(HIDDEN_PARENT,
                    format("notLoggedIn", name), getString("errorString"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            URI u = acct.getPreauthURI(authToken);
            if (m != null) {
                u = acct.getMessageUri(authToken, m);
                showNewMessages(false);
            }
            d.browse(u);
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public void dismissMessage(Message m) {
        if (m == null)
            return;
        List<Message> msgs = newMessages.get(m.getAccount());
        if (msgs == null)
            return;
        msgs.remove(m);
        updateUnreadMessages(m.getAccount(), msgs);
        showNewMessages(false);
    }
    
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    private class PauseMailMenuAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
            suppressMailAlerts = item.isSelected();
        }
    }
    private class OptionsMenuAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            OptionsDialog.showForm(ZimbraTray.this);
        }
    }

    public void showNewMessages(boolean showView) {
        ArrayList<Object> list = new ArrayList<Object>();
        for (Account a : newMessages.keySet()) {
            List<Message> msgs = newMessages.get(a);
            if (msgs != null && msgs.size() > 0) {
                list.add(a.getAccountName());
                for (Message m : msgs)
                    list.add(m);
            }
        }
        
        if (list.size() > 0 && showView)
            MessageListView.showView(this, list);
        else
            MessageListView.refreshView(list);
    }

    /**
     * Make sure that the notifier doesn't contain any read messages
     */
    public void updateUnreadMessages(Account account, List<Message> messages) {
        List<Message> msgs = newMessages.get(account);
        if (messages == null)
            messages = Collections.emptyList();
        if (msgs != null) {
            if (msgs.retainAll(messages) && !suppressMailAlerts)
                showNewMessages(false);
        }
        String name = account.getAccountName();
        JMenuItem item = accountMenuMap.get(account.getId());
        if (item != null) {
            item.setText(msgs != null && msgs.size() > 0 ?
                    format("accountMessageCount", name, msgs.size()) : name);
        }
        updateTrayIcon();
    }
    
    private void updateOSXBadge() {
        String os = System.getProperty("os.name");
        if (!os.contains("Mac OS X"))
            return;
        Application app = Application.getApplication();
        // Hack for java 1.6 on OSX
        try {
            int count = 0;
            for (List<Message> msgs : newMessages.values())
                count += msgs.size();
            
            Method m =  Application.class.getDeclaredMethod(
                    "setDockIconBadge", String.class);
            m.invoke(app, count == 0 ? null : "" + count);
        }
        catch (NoSuchMethodException e) {
            // ignore
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void newMessagesFound(Account account, List<Message> messages) {
        List<Message> msgs = newMessages.get(account);
        if (msgs == null) {
            msgs = new ArrayList<Message>();
            newMessages.put(account, msgs);
        }
        msgs.addAll(messages);
        JMenuItem item = accountMenuMap.get(account.getId());
        if (item != null) {
            item.setText(format("accountMessageCount",
                    account.getAccountName(), msgs.size()));
        }
        if (!suppressMailAlerts) {
            playSound(Prefs.getPrefs().getMessageSound());
            showNewMessages(true);
        }

        updateTrayIcon();
    }

    public void appointmentsFound(Account account, List<Appointment> appts) {
        if (!appointments.containsKey(account)) {
            appointments.put(account, new HashSet<Appointment>());
        }
        Set<Appointment> acctappts = appointments.get(account);
        // add new appointments
        for (Appointment a : appts) {
            if (!acctappts.contains(a)) {
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
            a.cancelAlarm();
        }
        AppointmentListView.refreshView();
    }
    
    public void dismissAppointments(List<Appointment> appts) {
        final HashMap<Account,List<Appointment>> apptmap =
                new HashMap<Account,List<Appointment>>();
        for (Appointment a : appts) {
            if (!apptmap.containsKey(a.getAccount())) {
                apptmap.put(a.getAccount(), new ArrayList<Appointment>());
            }
            List<Appointment> acctappts = apptmap.get(a.getAccount());
            acctappts.add(a);
        }
        
        getExecutor().submit(new Runnable() {
            public void run() {
                try {
                    for (Account account : apptmap.keySet()) {
                        AccountHandler h = accountHandlerMap.get(
                                account.getId());
                        h.dismissAppointmentAlarms(apptmap.get(account));
                    }
                }
                catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        });
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
        updateOSXBadge();
    }

    public void playSound(String name) {
        if (Prefs.getPrefs().isSoundDisabled() ||
                name == null || "".equals(name.trim()))
            return;
        FileInputStream fin = null;
        try {
            File f = new File(name);
            fin = new FileInputStream(f);
            AudioInputStream ain = AudioSystem.getAudioInputStream(fin);
            final Clip audioClip = AudioSystem.getClip();
            audioClip.addLineListener(new LineListener() {
                public void update(LineEvent e) {
                    if (LineEvent.Type.STOP == e.getType())
                        audioClip.close();
                }
            });
            audioClip.open(ain);
            audioClip.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
        catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        finally {
            try { if (fin != null) fin.close(); } catch (IOException e2) { }
        }
    }

    public AccountHandler getAccountHandlerBy(Account acct) {
         return accountHandlerMap.get(acct.getId());
    }
}

package com.zimbra.app.systray;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.JOptionPane;
import javax.xml.soap.SOAPException;

import com.zimbra.app.soap.SOAPFaultException;
import com.zimbra.app.soap.SoapInterface;
import com.zimbra.app.soap.messages.AuthRequest;
import com.zimbra.app.soap.messages.AuthResponse;
import com.zimbra.app.soap.messages.BatchRequest;
import com.zimbra.app.soap.messages.BatchResponse;
import com.zimbra.app.soap.messages.CreateTagRequest;
import com.zimbra.app.soap.messages.CreateTagResponse;
import com.zimbra.app.soap.messages.DismissCalendarItemAlarmRequest;
import com.zimbra.app.soap.messages.DismissCalendarItemAlarmResponse;
import com.zimbra.app.soap.messages.GetFolderRequest;
import com.zimbra.app.soap.messages.GetFolderResponse;
import com.zimbra.app.soap.messages.GetInfoRequest;
import com.zimbra.app.soap.messages.GetInfoResponse;
import com.zimbra.app.soap.messages.GetPrefsRequest;
import com.zimbra.app.soap.messages.GetPrefsResponse;
import com.zimbra.app.soap.messages.GetTagRequest;
import com.zimbra.app.soap.messages.GetTagResponse;
import com.zimbra.app.soap.messages.MsgActionRequest;
import com.zimbra.app.soap.messages.MsgActionResponse;
import com.zimbra.app.soap.messages.SearchRequest;
import com.zimbra.app.soap.messages.SearchResponse;

public class AccountHandler implements Runnable {
    private final ZimbraTray zmtray;
    private volatile String authToken;
    private final Account account;
    private String serverVersionString;
    private String serverUsername;
    private int pollInterval = -1;

    private volatile boolean isRunning;

    private final static String NEED_TO_REAUTH = "zmtray::NEED_TO_REAUTH";
    
    private final static ThreadLocal<Account> currentAccount =
            new ThreadLocal<Account>();
    
    private final static String DEFAULT_MAIL_FOLDER = "Inbox";
    private final static String DEFAULT_CALENDAR    = "Calendar";
    private final static String MAIL_FOLDER_VIEW    = "message";
    private final static String CALENDAR_VIEW       = "appointment";

    private boolean shutdown;
    private ScheduledFuture<?> f;
    
    private HashSet<Long> seenMailMessages = new HashSet<Long>();
    private HashMap<String,GetFolderResponse.Folder> nameFolderMap =
            new HashMap<String,GetFolderResponse.Folder>();
    private ArrayList<GetFolderResponse.Folder> mailFolders =
            new ArrayList<GetFolderResponse.Folder>();
    
    private ArrayList<GetFolderResponse.Folder> calendars =
            new ArrayList<GetFolderResponse.Folder>();
    
    // interval in which to loop in case we get an error authenticating or
    // retrieving the polling preferences
    private final static int ERROR_POLL_INTERVAL = 60;
    
    private final static String POLL_INTERVAL_PREF =
            "zimbraPrefMailPollingInterval";

    public enum MessageAction {
        READ("read"), FLAG("flag"), UPDATE("update"),
        MOVE("move"), SPAM("spam"), TRASH("trash");
        public final String op;
        MessageAction(String value) {
            this.op = value;
        }
    }
    
    public AccountHandler(Account account, ZimbraTray zmtray) {
        this.account = account;
        this.zmtray = zmtray;
        zmtray.getExecutor().submit(this);
    }
    
    private void requestAuthToken() {
        AuthRequest req = new AuthRequest();
        req.account = new AuthRequest.Account();
        req.account.by = "name";
        req.account.name = account.getLogin();
        req.password = account.getPassword();

        try {
            AuthResponse resp = SoapInterface.call(req, AuthResponse.class,
                    account.getServiceURL());
            authToken = resp.authToken;
        }
        catch (SOAPFaultException e) {
            showMessage(account.getAccountName() + " : " +
                    e.reason.text, "AuthRequest",
                    JOptionPane.ERROR_MESSAGE);
        }
        catch (SSLHandshakeException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        catch (SOAPException e) {
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
        }
            
        
    }
    
    private void parsePollInterval(GetPrefsResponse r) {
        for (GetPrefsRequest.Pref pref : r.prefs) {
            if (POLL_INTERVAL_PREF.equals(pref.name)) {
                char unit = pref.value.charAt(pref.value.length() - 1);
                TU tu = null;
                boolean dontchop = false;
                switch (unit) {
                case 'h': tu = TU.HOURS;   break;
                case 'm': tu = TU.MINUTES; break;
                case 's': tu = TU.SECONDS; break;
                case 'd': tu = TU.DAYS;    break;
                default:
                    tu = TU.SECONDS;
                    dontchop = true;
                }
                
                int interval = Integer.parseInt(dontchop ? pref.value :
                        pref.value.substring(0, pref.value.length() - 1));
                pollInterval = (int) TU.SECONDS.convert(interval, tu);
                break;
            }
        }
    }
    
    private void parseInfo(GetInfoResponse r) {
        serverVersionString = r.version;
        serverUsername = r.name;
        System.out.printf("Server Version for account [%s]: %s\n",
                account.getAccountName(), serverVersionString);
    }
    
    private void parseFolderList(GetFolderResponse r) {
        parseFolderList(r.folders);
        if (account.getSubscribedCalendarNames().size() == 0 &&
                calendars.size() == 1) {
            account.setSubscribedCalendarNames(
                    Arrays.asList(calendars.get(0).name));
        }
        if (account.getSubscribedMailFolders().size() == 0 &&
                mailFolders.size() == 1) {
            account.setSubscribedMailFolders(
                    Arrays.asList(mailFolders.get(0).name));
        }
        if (account.getSubscribedMailFolders().size() == 0) {
            for (GetFolderResponse.Folder f : mailFolders) {
                if (DEFAULT_MAIL_FOLDER.equals(f.name)) {
                    account.setSubscribedMailFolders(
                            Arrays.asList(DEFAULT_MAIL_FOLDER));
                    break;
                }
            }
        }
        if (account.getSubscribedCalendarNames().size() == 0) {
            for (GetFolderResponse.Folder f : calendars) {
                if (DEFAULT_CALENDAR.equals(f.name)) {
                    account.setSubscribedCalendarNames(
                            Arrays.asList(DEFAULT_CALENDAR));
                    break;
                }
            }
        }
    }
    
    private void parseFolderList(List<GetFolderResponse.Folder> folders) {
        for (GetFolderResponse.Folder f : folders){
            if (f.owner != null && !serverUsername.equals(f.owner))
                continue;
            parseFolderList(f.folders);
            if (MAIL_FOLDER_VIEW.equals(f.view)) {
                mailFolders.add(f);
            } else if (CALENDAR_VIEW.equals(f.view)) {
                calendars.add(f);
            } else {
                continue;
            }
            nameFolderMap.put(f.name, f);
        }
    }
    
    private void requestAccountInfo() {
        BatchRequest req = new BatchRequest();
        req.folderRequest = new GetFolderRequest();
        req.folderRequest.folder = new GetFolderRequest.Folder();
        req.folderRequest.folder.text = "";
        req.infoRequest = new GetInfoRequest();
        req.infoRequest.sections = "idents";
        req.prefsRequest = new GetPrefsRequest();
        req.prefsRequest.pref = new GetPrefsRequest.Pref();
        req.prefsRequest.pref.name = POLL_INTERVAL_PREF;
        try {
            BatchResponse resp = SoapInterface.call(req, BatchResponse.class,
                    account.getServiceURL(), authToken);
            parsePollInterval(resp.prefsResponse);
            parseInfo(resp.infoResponse);
            parseFolderList(resp.folderResponse);
        } catch (SOAPFaultException e) {
            showMessage(account.getAccountName() + " : " + 
                    e.reason.text, "BatchRequest",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SOAPException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchForNewItems() {
        BatchRequest req = new BatchRequest();
        SearchRequest r1 = new SearchRequest();
        r1.type = MAIL_FOLDER_VIEW;

        StringBuilder folderQuery = new StringBuilder();
        for (String name : account.getSubscribedMailFolders()) {
            GetFolderResponse.Folder f = nameFolderMap.get(name);
            folderQuery.append(Integer.toString(f.id));
            folderQuery.append(" or ");
        }
        folderQuery.setLength(folderQuery.length() - 4);
        r1.query = "is:unread inid:(" + folderQuery + ")";

        SearchRequest r2 = new SearchRequest();
        r2.type = CALENDAR_VIEW;
        // also get undismissed reminders from the past few days
        // (over the weekend possibly: 3 days)
        r2.calendarSearchStartTime = System.currentTimeMillis() -
                TU.MILLISECONDS.convert(3, TU.DAYS);
        r2.calendarSearchEndTime   = System.currentTimeMillis() +
                TU.MILLISECONDS.convert(1, TU.DAYS);
        folderQuery = new StringBuilder();
        for (String name : account.getSubscribedCalendarNames()) {
            GetFolderResponse.Folder f = nameFolderMap.get(name);
            folderQuery.append(Integer.toString(f.id));
            folderQuery.append(" or ");
        }
        folderQuery.setLength(folderQuery.length() - 4);
        r2.query = "inid:(" + folderQuery + ")";
        req.searchRequests.add(r1);
        req.searchRequests.add(r2);
        try {
            BatchResponse resp = SoapInterface.call(req, BatchResponse.class,
                    account.getServiceURL(), authToken);
            boolean hasMessages = false;
            for (SearchResponse r : resp.searchResponses) {
                if (r.messages.size() > 0) {
                    hasMessages = true;
                    HashSet<Long> foundMessages = new HashSet<Long>();
                    ArrayList<Message> newMessages = new ArrayList<Message>();
                    ArrayList<Message> unread = new ArrayList<Message>();
                    for (SearchResponse.Message m : r.messages) {
                        Message message = new Message(account, m);
                        unread.add(message);
                        foundMessages.add(m.id);
                        if (seenMailMessages.contains(m.id))
                            continue;
                        newMessages.add(message);
                        seenMailMessages.add(m.id);
                    }
                    // prevent from growing unbounded
                    seenMailMessages.retainAll(foundMessages);
                    zmtray.updateUnreadMessages(account, unread);
                    if (newMessages.size() > 0)
                        zmtray.newMessagesFound(account, newMessages);
                } else if (r.appointments.size() > 0) {
                    ArrayList<Appointment> appointments =
                            new ArrayList<Appointment>();
                    long alarmStartRange = System.currentTimeMillis() -
                            TU.MILLISECONDS.convert(3, TU.DAYS);
                    long alarmEndRange   = System.currentTimeMillis() +
                            TU.MILLISECONDS.convert(1, TU.DAYS);
        
                    for (SearchResponse.Appointment a : r.appointments) {
                        if (a.alarmData != null &&
                                a.alarmData.alarmTime > alarmStartRange &&
                                a.alarmData.alarmTime < alarmEndRange) {
                            appointments.add(new Appointment(account, a));
                        }
                    }
                    zmtray.appointmentsFound(account, appointments);
                }
            }
            if (!hasMessages) {
                zmtray.updateUnreadMessages(account, null);
            }
        } catch (SOAPFaultException e) {
            authToken = NEED_TO_REAUTH;
            System.out.println(account.getAccountName() + ":" + e.reason.text +
                    ":" + e.code.value);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SOAPException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public synchronized void run() {
        isRunning = true;
        currentAccount.set(account);
        try {
            _run();
        }
        catch (Error e) {
            e.printStackTrace();
            throw e;
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            if (!shutdown) {
                if (NEED_TO_REAUTH.equals(authToken)) {
                    System.out.println(account.getAccountName() +
                            ": need to reauth, polling immediately");
                    authToken = null;
                    f = zmtray.getExecutor().schedule(
                            this, 0, TimeUnit.SECONDS);
                } else {
                    f = zmtray.getExecutor().schedule(this,
                            pollInterval == -1 ?
                                    ERROR_POLL_INTERVAL : pollInterval,
                                    TimeUnit.SECONDS);
                }
            }
            currentAccount.set(null);
            isRunning = false;
        }
    }

    private void _run() {
        if (authToken == null) {
            requestAuthToken();
            pollInterval = -1;
        }
        if (pollInterval == -1 && authToken != null) {
            requestAccountInfo();
            System.out.println("Poll interval set to: " + pollInterval);
        }
        
        if (authToken != null) {
            if (account.getSubscribedCalendarNames().size() == 0 ||
                    account.getSubscribedMailFolders().size() == 0) {
                JOptionPane.showMessageDialog(zmtray.HIDDEN_PARENT,
                        "At least one calendar and mail folder must be selected");
                return;
            }
        
            searchForNewItems();
        }
        
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public Account getAccount() {
        return account;
    }
    
    private void showMessage(final String message, final String title,
            final int type) {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(zmtray.HIDDEN_PARENT,
                            message, title, type);
                }
            });
        } catch (InterruptedException e) { // ignore
        } catch (InvocationTargetException e) { // ignore
        }
    }
    
    public void dismissAppointmentAlarms(List<Appointment> appts) {
        DismissCalendarItemAlarmRequest r =
                new DismissCalendarItemAlarmRequest();
        HashMap<Long,Appointment> apptmap =
                new HashMap<Long,Appointment>();
        for (Appointment appt : appts) {
            System.out.println("Attempting to dismiss: " + appt.getName());
            apptmap.put(appt.getId(), appt);
            DismissCalendarItemAlarmRequest.Appointment a =
                new DismissCalendarItemAlarmRequest.Appointment();
            a.dismissedAt = System.currentTimeMillis();
            a.id = appt.getId();
            r.appointments.add(a);
        }
        try {
            currentAccount.set(account);
            DismissCalendarItemAlarmResponse ret = SoapInterface.call(
                    r, DismissCalendarItemAlarmResponse.class,
                    account.getServiceURL(), authToken);
            for (DismissCalendarItemAlarmResponse.Appointment ap :
                ret.appointments) {
                Appointment appt = apptmap.get(ap.id);
                appt.cancelAlarm();
                System.out.println("Successfully dismissed: " + appt.getName());
            }
        } catch (SOAPFaultException e) {
            showMessage(account.getAccountName() + " : " + 
                    e.reason.text, "DismissCalendarItemAlarmRequest",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SOAPException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
        }
        finally {
            currentAccount.set(null);
        }
    }
    
    public void tagMessage(Message m, String[] tags) {
        GetTagRequest greq = new GetTagRequest();
        currentAccount.set(account);
        
        try {
            GetTagResponse gresp = SoapInterface.call(greq,
                    GetTagResponse.class, account.getServiceURL(), authToken);
            HashSet<String> toCreate = new HashSet<String>();
            HashMap<String,Integer> tagMap =
                    new HashMap<String,Integer>();
            for (GetTagResponse.Tag t : gresp.tags)
                tagMap.put(t.name, t.id);
            for (String t : tags) {
                if (!tagMap.containsKey(t)) {
                    toCreate.add(t);
                }
            }
            for (String t : toCreate) {
                CreateTagRequest creq = new CreateTagRequest();
                creq.tag.name = t;
                CreateTagResponse cresp = SoapInterface.call(creq,
                        CreateTagResponse.class, account.getServiceURL(),
                        authToken);
                tagMap.put(t, cresp.tag.id);
            }
            HashSet<Integer> tagIds = new HashSet<Integer>();
            for (String t : tags) {
                tagIds.add(tagMap.get(t));
            }
            
            StringBuilder b = new StringBuilder();
            for (Integer i : tagIds) {
                b.append(i).append(",");
            }
            b.setLength(b.length() - 1);
            doMessageAction(m, MessageAction.UPDATE, b.toString());
        } catch (SOAPFaultException e) {
            showMessage(account.getAccountName() + " : " + 
                    e.reason.text, "Tag Message",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SOAPException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
        }
        
        currentAccount.set(null);
    }

    public void moveMessage(Message m, String folder) {
        String folderId = null;
        for (GetFolderResponse.Folder f : mailFolders) {
            if (f.name.equals(folder)) {
                folderId = Integer.toString(f.id);
                break;
            }
        }
        if (folderId == null) {
            System.out.println("Unable to find folder: " + folder);
            return;
        }
        doMessageAction(m, MessageAction.MOVE, folderId);
    }

    public void doMessageAction(Message m, MessageAction op, String args) {
        MsgActionRequest r = new MsgActionRequest();
        r.action.id = "" + m.getId();
        r.action.op = op.op;
        switch (op) {
        case UPDATE:
            r.action.tags = args;
            break;
        case MOVE:
            r.action.folderId = args;
            break;
        }
        
        try {
            currentAccount.set(account);
            MsgActionResponse ret = SoapInterface.call(
                    r, MsgActionResponse.class,
                    account.getServiceURL(), authToken);
            System.out.println(ret.action.id + ": Succesful action: " +
                    ret.action.op);
        } catch (SOAPFaultException e) {
            showMessage(account.getAccountName() + " : " + 
                    e.reason.text, "MsgActionRequest",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SOAPException e) {
            e.printStackTrace();
            showMessage(account.getAccountName() + " : " + 
                    e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
        }
        finally {
            currentAccount.set(null);
        }
    }

    public void shutdown() {
        System.out.println(account.getAccountName() + ": shutting down poller");
        shutdown = true;
        if (!f.isDone()) {
            f.cancel(false);
        }
    }
    
    public void pollNow() {
        if (isRunning || shutdown)
            return;
        if (f.getDelay(TimeUnit.SECONDS) > 1) {
            f.cancel(true);
            zmtray.getExecutor().submit(this);
        }
    }

    public static Account getCurrentAccount() {
        return currentAccount.get();
    }

    public List<String> getAvailableMailFolders() {
        ArrayList<String> l = new ArrayList<String>();
        for (GetFolderResponse.Folder f : mailFolders) {
            l.add(f.name);
        }
        return l;
    }

    public List<String> getAvailableCalendars() {
        ArrayList<String> l = new ArrayList<String>();
        for (GetFolderResponse.Folder f : calendars) {
            l.add(f.name);
        }
        return l;
    }
}

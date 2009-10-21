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
import com.zimbra.app.soap.messages.GetFolderRequest;
import com.zimbra.app.soap.messages.GetFolderResponse;
import com.zimbra.app.soap.messages.GetInfoRequest;
import com.zimbra.app.soap.messages.GetInfoResponse;
import com.zimbra.app.soap.messages.GetPrefsRequest;
import com.zimbra.app.soap.messages.GetPrefsResponse;
import com.zimbra.app.soap.messages.SearchRequest;
import com.zimbra.app.soap.messages.SearchResponse;

public class AccountHandler implements Runnable {
    private final ZimbraTray zmtray;
    private String authToken;
    private final Account account;
    private String serverVersionString;
    private String serverUsername;
    private int pollInterval = -1;

    private final static ThreadLocal<Account> currentAccount =
            new ThreadLocal<Account>();
    
    private final static String DEFAULT_MAIL_FOLDER = "Inbox";
    private final static String DEFAULT_CALENDAR    = "Calendar";
    private final static String MAIL_FOLDER_VIEW    = "message";
    private final static String CALENDAR_VIEW       = "appointment";

    private boolean shutdown;
    private ScheduledFuture<?> f;
    
    private HashSet<Integer> seenMailMessages = new HashSet<Integer>();
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
            showMessage(e.reason.text, "AuthRequest",
                    JOptionPane.ERROR_MESSAGE);
        }
        catch (SSLHandshakeException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            showMessage(e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        catch (SOAPException e) {
            showMessage(e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
        }
            
        
    }
    
    private void parsePollInterval(GetPrefsResponse r) {
        for (GetPrefsRequest.Pref pref : r.prefs) {
            if (POLL_INTERVAL_PREF.equals(pref.name)) {
                char unit = pref.value.charAt(pref.value.length() - 1);
                TimeUnit tu = null;
                boolean dontchop = false;
                switch (unit) {
                case 'h': tu = TimeUnit.HOURS;   break;
                case 'm': tu = TimeUnit.MINUTES; break;
                case 's': tu = TimeUnit.SECONDS; break;
                case 'd': tu = TimeUnit.DAYS;    break;
                default:
                    tu = TimeUnit.SECONDS;
                    dontchop = true;
                }
                
                int interval = Integer.parseInt(dontchop ? pref.value :
                        pref.value.substring(0, pref.value.length() - 1));
                pollInterval = (int) TimeUnit.SECONDS.convert(interval, tu);
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
            showMessage(e.reason.text, "BatchRequest",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            showMessage(e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (SOAPException e) {
            showMessage(e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
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
        r2.calendarSearchStartTime = System.currentTimeMillis();
        r2.calendarSearchEndTime   = System.currentTimeMillis() +
                TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
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
            for (SearchResponse r : resp.searchResponses) {
                if (r.messages.size() > 0) {
                    HashSet<Integer> foundMessages = new HashSet<Integer>();
                    ArrayList<Message> newMessages = new ArrayList<Message>();
                    for (SearchResponse.Message m : r.messages) {
                        foundMessages.add(m.id);
                        if (seenMailMessages.contains(m.id))
                            continue;
                        Message message = new Message(account, m);
                        newMessages.add(message);
                        System.out.println("From: " + m.sender.fullName
                                + " <" + m.sender.emailAddress + ">");
                        System.out.println("Subject: " + m.subject);
                        System.out.println(m.fragment);
                        System.out.println();
                        seenMailMessages.add(m.id);
                    }
                    // prevent from growing unbounded
                    seenMailMessages.retainAll(foundMessages);
                    zmtray.newMessagesFound(account, newMessages);
                } else if (r.appointments.size() > 0) {
                    ArrayList<Appointment> appointments =
                            new ArrayList<Appointment>();
                    for (SearchResponse.Appointment a : r.appointments) {
                        appointments.add(new Appointment(account, a));
                    }
                    zmtray.appointmentsFound(account, appointments);
                }
            }
        } catch (SOAPFaultException e) {
            showMessage(e.reason.text, "SearchRequest",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            showMessage(e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (SOAPException e) {
            showMessage(e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public void run() {
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
                f = zmtray.getExecutor().schedule(this,
                        pollInterval == -1 ? ERROR_POLL_INTERVAL : pollInterval,
                                TimeUnit.SECONDS);
            }
        }
        currentAccount.set(null);
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
            while (account.getSubscribedCalendarNames().size() == 0 ||
                    account.getSubscribedMailFolders().size() == 0) {
                // TODO implement folder selection
                JOptionPane.showMessageDialog(zmtray.HIDDEN_PARENT,
                        "At least one calendar and mail folder must be selected");
            }
        
System.out.println("searching for new items");
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

    public void shutdown() {
        shutdown = true;
        if (!f.isDone()) {
            f.cancel(false);
        }
    }

    public static Account getCurrentAccount() {
        return currentAccount.get();
    }
}

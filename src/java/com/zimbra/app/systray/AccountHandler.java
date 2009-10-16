package com.zimbra.app.systray;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

public class AccountHandler implements Runnable {
    private final ZimbraTray zmtray;
    private String authToken;
    private final Account account;
    private String serverVersionString;
    private String serverUsername;
    private int pollInterval = -1;
    
    private final static String DEFAULT_MAIL_FOLDER   = "Inbox";
    private final static String DEFAULT_CALENDAR      = "Inbox";
    private final static String MAIL_FOLDER_VIEW      = "message";
    private final static String CALENDAR_FOLDER_VIEW  = "appointment";
    
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
        catch (IOException e) {
            showMessage(e.getLocalizedMessage(), "IOException",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        catch (SOAPException e) {
            showMessage(e.getLocalizedMessage(), "SOAPException",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
            
        
    }
    
    private void parsePollInterval(GetPrefsResponse r) {
        for (GetPrefsRequest.Pref pref : r.prefs) {
            if (POLL_INTERVAL_PREF.equals(pref.name)) {
                char unit = pref.value.charAt(pref.value.length() - 1);
                TimeUnit tu = null;
                switch (unit) {
                case 'h':
                    tu = TimeUnit.HOURS;
                    break;
                case 'm':
                    tu = TimeUnit.MINUTES;
                    break;
                case 's':
                    tu = TimeUnit.SECONDS;
                    break;
                case 'd':
                    tu = TimeUnit.DAYS;
                    break;
                default:
                    throw new IllegalArgumentException(pref.value);
                }
                
                int interval = Integer.parseInt(pref.value.substring(0,
                        pref.value.length() - 1));
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
            } else if (CALENDAR_FOLDER_VIEW.equals(f.view)) {
                calendars.add(f);
            } else {
                System.out.println("Skipping folder: " + f.name);
                continue;
            }
            nameFolderMap.put(f.name, f);
        }
    }
    
    private void requestAccountInfo() {
        BatchRequest batchrequest = new BatchRequest();
        batchrequest.folderRequest = new GetFolderRequest();
        batchrequest.folderRequest.folder = new GetFolderRequest.Folder();
        batchrequest.folderRequest.folder.text = "";
        batchrequest.infoRequest = new GetInfoRequest();
        batchrequest.infoRequest.sections = "idents";
        batchrequest.prefsRequest = new GetPrefsRequest();
        batchrequest.prefsRequest.pref = new GetPrefsRequest.Pref();
        batchrequest.prefsRequest.pref.name = POLL_INTERVAL_PREF;
        try {
            BatchResponse resp = SoapInterface.call(batchrequest,
                    BatchResponse.class,
                    account.getServiceURL(), authToken);
            parsePollInterval(resp.prefsResponse);
            parseInfo(resp.infoResponse);
            parseFolderList(resp.folderResponse);
        } catch (SOAPFaultException e) {
            showMessage(e.reason.text, "AuthRequest",
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
        
    }
    
    public void run() {
        if (authToken == null) {
            requestAuthToken();
            pollInterval = -1;
        }
        if (pollInterval == -1 && authToken != null) {
            requestAccountInfo();
            System.out.println("Poll interval set to: " + pollInterval);
        }
        
        while (account.getSubscribedCalendarNames().size() == 0 ||
                account.getSubscribedMailFolders().size() == 0) {
            // TODO implement folder selection
            JOptionPane.showMessageDialog(zmtray.HIDDEN_PARENT,
                    "At least one calendar and mail folder must be selected");
        }
        
        searchForNewItems();
        
        /*ScheduledFuture<?> f =*/ zmtray.getExecutor().schedule(this,
                pollInterval == -1 ? ERROR_POLL_INTERVAL : pollInterval,
                        TimeUnit.SECONDS);
        //f.cancel(false);
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
}

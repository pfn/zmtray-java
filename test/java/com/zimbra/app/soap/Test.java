package com.zimbra.app.soap;

import java.net.URL;
import java.util.Properties;
import java.util.Date;

import javax.xml.soap.SOAPMessage;

import com.zimbra.app.soap.SOAPFaultException;
import com.zimbra.app.soap.messages.ContextHeader;
import com.zimbra.app.soap.messages.AuthRequest;
import com.zimbra.app.soap.messages.AuthResponse;
import com.zimbra.app.soap.messages.GetInfoRequest;
import com.zimbra.app.soap.messages.GetInfoResponse;
import com.zimbra.app.soap.messages.GetFolderRequest;
import com.zimbra.app.soap.messages.GetFolderResponse;
import com.zimbra.app.soap.messages.GetPrefsRequest;
import com.zimbra.app.soap.messages.GetPrefsResponse;
import com.zimbra.app.soap.messages.SearchRequest;
import com.zimbra.app.soap.messages.SearchResponse;
import com.zimbra.app.soap.messages.BatchRequest;
import com.zimbra.app.soap.messages.BatchResponse;

public class Test {
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        p.load(Test.class.getResourceAsStream("/test.properties"));
        URL u = new URL(p.getProperty("url"));

        SOAPMessage m = SoapInterface.newMessage();
        AuthRequest authreq = new AuthRequest();
        authreq.account = new AuthRequest.Account();
        authreq.account.by = "name";
        authreq.account.name = p.getProperty("user");
        authreq.password = "bogus-fake";
        Marshaller.marshal(m.getSOAPBody(), authreq);

        m = SoapInterface.call(m, u);
        try {
            Marshaller.unmarshal(AuthResponse.class, m);
            throw new IllegalStateException("soap fault expected");
        }
        catch (SOAPFaultException e) {
            System.out.println("\nFault Code: " + e.code.value);
            System.out.println("Fault reason: " + e.reason.text);
        }

        m = SoapInterface.newMessage();
        authreq.password = p.getProperty("pass");
        Marshaller.marshal(m.getSOAPBody(), authreq);
        m = SoapInterface.call(m, u);
        AuthResponse authresp = Marshaller.unmarshal(AuthResponse.class, m);

        System.out.println("\n Authtoken: " + authresp.authToken);

        ///////////////////////////////////////////////////////////////////////

        ContextHeader header = new ContextHeader();
        header.authToken = authresp.authToken;

        ///////////////////////////////////////////////////////////////////////

        m = SoapInterface.newMessage();
        GetPrefsRequest prefsreq = new GetPrefsRequest();
        Marshaller.marshal(m.getSOAPHeader(), header);
        Marshaller.marshal(m.getSOAPBody(), prefsreq);
        m = SoapInterface.call(m, u);
        GetPrefsResponse prefsresp = Marshaller.unmarshal(
                GetPrefsResponse.class, m);

        for (GetPrefsRequest.Pref pref : prefsresp.prefs) {
            System.out.printf(" %s = %s\n", pref.name, pref.value);
        }

        ///////////////////////////////////////////////////////////////////////

        m = SoapInterface.newMessage();
        prefsreq = new GetPrefsRequest();
        prefsreq.pref = new GetPrefsRequest.Pref();
        prefsreq.pref.name = "zimbraPrefMailPollingInterval";
        Marshaller.marshal(m.getSOAPHeader(), header);
        Marshaller.marshal(m.getSOAPBody(), prefsreq);
        m = SoapInterface.call(m, u);
        prefsresp = Marshaller.unmarshal(
                GetPrefsResponse.class, m);

        for (GetPrefsRequest.Pref pref : prefsresp.prefs) {
            System.out.printf("SINGLE: %s = %s\n", pref.name, pref.value);
        }

        ///////////////////////////////////////////////////////////////////////

        m = SoapInterface.newMessage();
        GetInfoRequest inforeq = new GetInfoRequest();
        inforeq.sections = "idents";
        Marshaller.marshal(m.getSOAPHeader(), header);
        Marshaller.marshal(m.getSOAPBody(), inforeq);

        m = SoapInterface.call(m, u);
        GetInfoResponse inforesp = Marshaller.unmarshal(
                GetInfoResponse.class, m);

        ///////////////////////////////////////////////////////////////////////

        m = SoapInterface.newMessage();
        GetFolderRequest folderreq = new GetFolderRequest();
        folderreq.folder = new GetFolderRequest.Folder();
        folderreq.folder.text = "";
        Marshaller.marshal(m.getSOAPHeader(), header);
        Marshaller.marshal(m.getSOAPBody(), folderreq);

        m = SoapInterface.call(m, u);

        GetFolderResponse folderresp = Marshaller.unmarshal(
                GetFolderResponse.class, m);

        ///////////////////////////////////////////////////////////////////////

        m = SoapInterface.newMessage();
        BatchRequest batchreq = new BatchRequest();
        batchreq.folderRequest = folderreq;
        batchreq.infoRequest = inforeq;
        Marshaller.marshal(m.getSOAPHeader(), header);
        Marshaller.marshal(m.getSOAPBody(), batchreq);

        m = SoapInterface.call(m, u);

        BatchResponse batchresp = Marshaller.unmarshal(
                BatchResponse.class, m);

        System.out.println("\n Version: " + batchresp.infoResponse.version);
        System.out.println(" Name: " + batchresp.infoResponse.name);
        for (GetFolderResponse.Folder folder : batchresp.folderResponse.folders) {
            System.out.printf("Folder: [%d] %s\n", folder.id, folder.name);
            for (GetFolderResponse.Folder nested : folder.folders) {
                System.out.printf(" Nested Folder: [%d] %s (%s)\n", nested.id, nested.name, nested.view);
            }
        }

        ///////////////////////////////////////////////////////////////////////

        m = SoapInterface.newMessage();
        SearchRequest searchreq = new SearchRequest();
        searchreq.query = "is:unread";
        searchreq.type = "message";
        Marshaller.marshal(m.getSOAPHeader(), header);
        Marshaller.marshal(m.getSOAPBody(), searchreq);

        m = SoapInterface.call(m, u);
        SearchResponse searchresp = Marshaller.unmarshal(
                SearchResponse.class, m);

        for (SearchResponse.Message msg : searchresp.messages) {
            System.out.printf("From: %s <%s>\n",
                    msg.sender.fullName, msg.sender.emailAddress);
            System.out.println("Subject: " + msg.subject);
            System.out.println("Fragment:\n" + msg.fragment);
            System.out.println();
        }

        ///////////////////////////////////////////////////////////////////////

        m = SoapInterface.newMessage();
        searchreq = new SearchRequest();
        searchreq.type = "appointment";
        searchreq.query = "inid:10";
        searchreq.calendarSearchStartTime = System.currentTimeMillis();
        searchreq.calendarSearchEndTime = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000);
        System.out.printf("Start: %s  End: %s\n", searchreq.calendarSearchStartTime, searchreq.calendarSearchEndTime);
        Marshaller.marshal(m.getSOAPHeader(), header);
        Marshaller.marshal(m.getSOAPBody(), searchreq);

        m = SoapInterface.call(m, u);
        searchresp = Marshaller.unmarshal(
                SearchResponse.class, m);

        for (SearchResponse.Appointment appt : searchresp.appointments) {
            System.out.printf("Organizer: %s <%s>\n",
                    appt.organizer.name, appt.organizer.emailAddress);
            System.out.println("Location: " + appt.alarmData.location);
            System.out.println("Fragment: " + appt.fragment);
            System.out.println("Time: " + new Date(appt.alarmData.eventTime));
            System.out.println("Alarm Time: " + new Date(appt.alarmData.alarmTime));
            System.out.println();
        }
    }
}

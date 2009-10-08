package com.zimbra.app.soap;

import java.net.URL;

import javax.xml.soap.SOAPMessage;

import com.zimbra.app.soap.messages.ContextHeader;
import com.zimbra.app.soap.messages.AuthRequest;
import com.zimbra.app.soap.messages.AuthResponse;

public class Test {
    public static void main(String[] args) throws Exception {
        SOAPMessage m = SoapInterface.newMessage();

        AuthRequest authreq = new AuthRequest();
        authreq.account = new AuthRequest.Account();
        authreq.account.by = "name";
        authreq.account.name = "pfnguyen";
        authreq.password = "pfn@zimbra";
        Marshaller.marshal(m.getSOAPBody(), authreq);

        URL u = new URL("http://dogfood.zimbra.com/service/soap");
        m = SoapInterface.call(m, u);

        AuthResponse response = Marshaller.unmarshal(AuthResponse.class, m);
        System.out.println("\n Authtoken: " + response.authToken);

        ContextHeader header = new ContextHeader();
        header.authToken = response.authToken;

        m = SoapInterface.newMessage();
        Marshaller.marshal(m.getSOAPHeader(), header);

        System.out.println("Header:");
        m.writeTo(System.out);
    }
}

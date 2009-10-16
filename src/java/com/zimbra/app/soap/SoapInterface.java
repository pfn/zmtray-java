package com.zimbra.app.soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;

import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;

import com.zimbra.app.soap.messages.ContextHeader;

public class SoapInterface {
    private final static boolean DEBUG = true;
    private final static MessageFactory factory;
    static {
        try {
            factory = MessageFactory.newInstance(
                    SOAPConstants.SOAP_1_2_PROTOCOL);
        }
        catch (SOAPException e) {
            throw new IllegalStateException(e);
        }
    }
    static SOAPMessage newMessage() throws SOAPException {
        return factory.createMessage();
    }

    static SOAPMessage call(SOAPMessage r, URL u)
    throws IOException, SOAPException {
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setDoOutput(true);
        c.setDoInput(true);
        c.connect();
        OutputStream out = c.getOutputStream();
        InputStream in = null;
        try {
            if (DEBUG) {
                System.out.println("\nREQUEST:");
                r.writeTo(System.out);
            }
            r.writeTo(out);
            SOAPMessage response = null;
            try {
                in = c.getInputStream();
            }
            catch (IOException e) {
                in = c.getErrorStream();
            }
            response = factory.createMessage(null, in);
            if (DEBUG) {
                System.out.println("\nRESPONSE:");
                response.writeTo(System.out);
            }
            return response;
        }
        finally {
            out.close();
            if (in != null)
                in.close();
        }
    }
    
    public static <T> T call(Object request, Class<T> resultType,
            URL serviceTarget)
    throws IOException, SOAPException, SOAPFaultException {
        return call(request, resultType, serviceTarget, null);
    }
    public static <T> T call(Object request, Class<T> resultType,
            URL serviceTarget, String authToken)
    throws IOException, SOAPException, SOAPFaultException {
        SOAPMessage m = SoapInterface.newMessage();
        if (authToken != null) {
            ContextHeader header = new ContextHeader();
            header.authToken = authToken;
            Marshaller.marshal(m.getSOAPHeader(), header);
        }
        Marshaller.marshal(m.getSOAPBody(), request);
        m = call(m, serviceTarget);
        return Marshaller.unmarshal(resultType, m);
    }
}

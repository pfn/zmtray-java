package com.zimbra.app.soap;

import javax.xml.soap.SOAPException;

@Element(name="Fault", ns="http://www.w3.org/2003/05/soap-envelope")
public class SOAPFaultException extends SOAPException {
    public SOAPFaultException() {
        super("SOAP:Fault received");
    }

    @Element(name="Code")
    public Code code;

    @Element(name="Reason")
    public Reason reason;

    @Element
    public static class Code {

        @Element(name="Value", type=Type.TEXT)
        public String value;
    }

    @Element
    public static class Reason {
        @Element(name="Text", type=Type.TEXT)
        public String text;
    }
}

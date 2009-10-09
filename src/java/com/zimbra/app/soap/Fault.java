package com.zimbra.app.soap;

@Element(ns="http://www.w3.org/2003/05/soap-envelope")
public class Fault {
    @Element
    public Code code;

    @Element
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

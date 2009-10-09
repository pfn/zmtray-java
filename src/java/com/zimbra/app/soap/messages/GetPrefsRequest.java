package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraAccount")
public class GetPrefsRequest {
    @Element
    public Pref pref;

    @Element(name="pref")
    public static class Pref {
        @Element(type=Type.ATTRIBUTE, optional=false)
        public String name;

        @Element(type=Type.TEXT, samenode=true)
        public String value;
    }
}

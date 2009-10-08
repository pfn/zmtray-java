package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraAccount")
public class AuthRequest {
    @Element
    public Account account;

    @Element(name="password", type=Type.TEXT)
    public String password;

    @Element(name="account")
    public static class Account {
        @Element(type=Type.ATTRIBUTE)
        public String by;

        @Element(type=Type.TEXT)
        public String name;
    }
}

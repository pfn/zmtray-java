package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraAccount")
public class AuthResponse {
    @Element(type=Type.TEXT)
    public String authToken;
}

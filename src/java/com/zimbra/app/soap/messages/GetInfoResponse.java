package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraAccount")
public class GetInfoResponse {

    @Element(type=Type.TEXT)
    public String version;

    @Element(type=Type.TEXT)
    public String name;
}

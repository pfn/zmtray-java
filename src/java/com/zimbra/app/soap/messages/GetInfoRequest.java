package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraAccount")
public class GetInfoRequest {
    @Element(type=Type.ATTRIBUTE)
    public String sections;
}

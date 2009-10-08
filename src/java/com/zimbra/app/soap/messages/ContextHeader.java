package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;

@Element(name="context", ns="urn:zimbra")
public class ContextHeader {
    @Element
    public String authToken;
}

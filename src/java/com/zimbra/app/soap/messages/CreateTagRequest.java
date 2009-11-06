package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraMail")
public class CreateTagRequest {
    @Element(name="tag")
    public Tag tag = new Tag();
    
    @Element(name="tag")
    public static class Tag {
        @Element(type=Type.ATTRIBUTE)
        public String name;
    }
}

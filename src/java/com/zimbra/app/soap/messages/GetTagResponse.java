package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

import java.util.ArrayList;

@Element(ns="urn:zimbraMail")
public class GetTagResponse {

    @Element(name="tag")
    public ArrayList<Tag> tags = new ArrayList<Tag>();

    @Element(name="tag")
    public static class Tag {
        @Element(type=Type.ATTRIBUTE, optional=false)
        public String name;

        @Element(type=Type.ATTRIBUTE, optional=false)
        public int id;
    }
}

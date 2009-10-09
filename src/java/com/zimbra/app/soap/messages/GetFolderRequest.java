package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraMail")
public class GetFolderRequest {
    @Element
    public Folder folder;

    @Element(name="folder")
    public static class Folder {
        @Element(type=Type.TEXT)
        public String text;

        @Element(type=Type.ATTRIBUTE, name="l")
        public String id;
    }
}

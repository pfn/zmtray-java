package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

import java.util.ArrayList;

@Element(ns="urn:zimbraMail")
public class GetFolderResponse {

    @Element(name="folder", optional=false)
    public ArrayList<Folder> folders = new ArrayList<Folder>();

    @Element(name="folder")
    public static class Folder {
        @Element(name="folder")
        public ArrayList<Folder> folders = new ArrayList<Folder>();

        @Element(type=Type.ATTRIBUTE)
        public String owner;

        @Element(type=Type.ATTRIBUTE)
        public String view;

        @Element(type=Type.ATTRIBUTE, optional=false)
        public String name;

        @Element(type=Type.ATTRIBUTE)
        public int id;
    }
}

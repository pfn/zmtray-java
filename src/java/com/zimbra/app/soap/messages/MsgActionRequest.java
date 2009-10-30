package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraMail")
public class MsgActionRequest {

    @Element
    public Action action = new Action();
    
    @Element(name="action")
    public static class Action {
        @Element(type=Type.ATTRIBUTE)
        public String id;
        
        /**
         * One of:
         * read, flag, update (tag), move, spam, trash
         */
        @Element(type=Type.ATTRIBUTE)
        public String op;
        
        @Element(type=Type.ATTRIBUTE, name="t")
        public String tags;
        
        /**
         * Folder ID
         */
        @Element(type=Type.ATTRIBUTE, name="l")
        public String folderId;
    }
}

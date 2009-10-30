package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraMail")
public class MsgActionResponse {

    @Element
    public Action action;
    
    @Element(name="action")
    public static class Action {
        @Element(type=Type.ATTRIBUTE)
        public String id;
        
        /**
         * One of:
         * read, flag, update, move, update (tag), spam, trash
         */
        @Element(type=Type.ATTRIBUTE)
        public String op;
    }
}

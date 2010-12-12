package com.zimbra.app.soap.messages;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.app.soap.Element;

import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraMail")
public class DismissCalendarItemAlarmRequest {
    @Element(name="appt")
    public List<Appointment> appointments = new ArrayList<Appointment>();
    
    @Element(name="appt")
    public static class Appointment {
        @Element(type=Type.ATTRIBUTE, optional=false)
        public long id;
        
        @Element(type=Type.ATTRIBUTE, optional=false)
        public long dismissedAt;
    }
}

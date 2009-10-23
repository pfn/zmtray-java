package com.zimbra.app.soap.messages;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.app.soap.Element;

import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraMail")
public class DismissCalendarItemAlarmResponse {
    @Element(name="appt")
    public List<Appointment> appointments = new ArrayList<Appointment>();
    
    public static class Appointment {
        @Element(name="calItemId", type=Type.ATTRIBUTE, optional=false)
        public int id;
    }
}

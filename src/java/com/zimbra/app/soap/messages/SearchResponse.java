package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

import java.util.ArrayList;

@Element(ns="urn:zimbraMail")
public class SearchResponse {

    @Element(name="m")
    public ArrayList<Message> messages = new ArrayList<Message>();

    @Element(name="appt")
    public ArrayList<Appointment> appointments = new ArrayList<Appointment>();

    @Element(name="m")
    public static class Message {
        @Element(type=Type.ATTRIBUTE, optional=false)
        public int id;

        @Element(name="fr", type=Type.TEXT)
        public String fragment;

        @Element(name="su", type=Type.TEXT)
        public String subject;


        @Element(name="e", optional=false)
        public Sender sender;

        @Element(name="e")
        public static class Sender {
            @Element(name="p", type=Type.ATTRIBUTE)
            public String fullName;

            @Element(name="d", type=Type.ATTRIBUTE)
            public String shortName;

            @Element(name="a", type=Type.ATTRIBUTE)
            public String emailAddress;
        }
    }

    @Element(name="appt")
    public static class Appointment {
        @Element(type=Type.ATTRIBUTE, optional=false)
        public int id;

        @Element(type=Type.ATTRIBUTE, optional=false)
        public String name;

        @Element(type=Type.ATTRIBUTE, optional=false, name="dur")
        public int duration;

        @Element
        public AlarmData alarmData;

        @Element(name="fr", type=Type.TEXT)
        public String fragment;

        @Element(name="or")
        public Organizer organizer;

        @Element(name="alarmData")
        public static class AlarmData {
            
            @Element(type=Type.ATTRIBUTE)
            public String name;

            @Element(name="nextAlarm", type=Type.ATTRIBUTE, optional=false)
            public long alarmTime;

            @Element(type=Type.ATTRIBUTE, name="loc")
            public String location;

            @Element(type=Type.ATTRIBUTE, name="alarmInstStart")
            public long eventTime;
        }

        @Element(name="or")
        public static class Organizer {
            @Element(type=Type.ATTRIBUTE)
            public String url;

            @Element(type=Type.ATTRIBUTE, name="a")
            public String emailAddress;

            @Element(type=Type.ATTRIBUTE, name="d")
            public String name;
        }
    }
}

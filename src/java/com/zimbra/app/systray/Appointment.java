package com.zimbra.app.systray;

import com.zimbra.app.soap.messages.SearchResponse;

public class Appointment {
    public Appointment(Account account, SearchResponse.Appointment appt) {
        this.account = account;
        id = appt.id;
        fragment = appt.fragment;
        alarmName = appt.alarmData.name;
        alarmTime = appt.alarmData.alarmTime;
        location = appt.alarmData.location;
        eventTime = appt.alarmData.eventTime;
        url = appt.organizer.url;
        emailAddress = appt.organizer.emailAddress;
        organizerName = appt.organizer.name;
        name = appt.name;
    }

    private Account account;
    public Account getAccount() { return account; }
    private int id;
    public int getId() { return id; }

    public String name;
    public String getName() { return name; }

    private String fragment;
    public String getFragment() { return fragment; }
    
    private String alarmName;
    public String getAlarmName() { return alarmName; }

    private long alarmTime;
    public long getAlarmTime() { return alarmTime; }

    private String location;
    public String getLocation() { return location; }

    private long eventTime;
    public long getEventTime() { return eventTime; }

    private String url;
    public String getOrganizerURL() { return url; }

    private String emailAddress;
    public String getOrganizerAddress() { return emailAddress; }

    private String organizerName;
    public String getOrganizerName() { return organizerName; }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        boolean equals = false;
        if (other instanceof Appointment) {
            equals = ((Appointment) other).id == id;
        }
        return equals;
    }
}

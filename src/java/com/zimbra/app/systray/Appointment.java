package com.zimbra.app.systray;

import com.zimbra.app.soap.messages.SearchResponse;

import java.awt.EventQueue;
import java.util.concurrent.TimeUnit;

public class Appointment {
    private long snoozeTime;
    private Alarm alarm;

    public Appointment(Account account, SearchResponse.Appointment appt) {
        this.account = account;
        id = appt.id;
        fragment = appt.fragment;
        alarmName = appt.alarmData.name;
        alarmTime = appt.alarmData.alarmTime;
        snoozeTime = alarmTime;
        location = appt.alarmData.location;
        eventTime = appt.alarmData.eventTime;
        url = appt.organizer.url;
        emailAddress = appt.organizer.emailAddress;
        organizerName = appt.organizer.name;
        name = appt.name;
        duration = appt.duration;
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

    // in milliseconds
    private int duration;
    public int getDuration() { return duration; }

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

    public long getSnoozeTime() {
        return snoozeTime;
    }

    public void setSnoozeTime(long time) {
        snoozeTime = time;
    }

    public void createAlarm(ZimbraTray zt) {
        alarm = new Alarm(zt);
    }

    public Alarm getAlarm() {
        return alarm;
    }

    public class Alarm implements Runnable {
        private ZimbraTray zt;
        public Alarm(ZimbraTray zt) {
            this.zt = zt;
            long now = System.currentTimeMillis();
            if (alarmTime <= now) {
                System.out.println("Alarm overdue, show now");
                zt.getExecutor().submit(this);
            } else {
                long delay = alarmTime - now;
                System.out.println("Alarm ok, schedule for later");
                zt.getExecutor().schedule(this, delay, TimeUnit.MILLISECONDS);
            }
        }

        public void run() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    System.out.println("Show appointment: " + getName());
                    AppointmentListView.showView(zt, Appointment.this);
                }
            });
        }
    }
}

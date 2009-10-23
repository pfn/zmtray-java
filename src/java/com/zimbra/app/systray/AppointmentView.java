package com.zimbra.app.systray;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.hanhuy.common.ui.ResourceBundleForm;

public class AppointmentView extends ResourceBundleForm {
    private Appointment appt;
    private JComponent component = new JPanel();
    
    private JLabel  locLabel    = new JLabel();
    private JLabel  name        = new JLabel();
    private JLabel  when        = new JLabel();
    private JLabel  location    = new JLabel();
    private JLabel  disposition = new JLabel();
    private JButton dismiss     = new JButton();

    public AppointmentView() {
        component.setLayout(createLayoutManager());
        layout();
    }
    
    public AppointmentView(Appointment a) {
        this();
        appt = a;
        setAppointment(a);
    }

    private void setAppointment(Appointment a) {
        name.setText("<html><b>" + a.getName());
        when.setText(format("eventTime", new Date(a.getEventTime()),
                new Date(a.getEventTime() + a.getDuration())));

        if (a.getLocation() != null && !"".equals(a.getLocation().trim())) {
            location.setText(a.getLocation());
            locLabel.setText(getString("locationLabel.text"));
        } else {
            locLabel.setText(
                    getString("locationLabel.text").replaceAll(".", " "));
            location.setText(" ");
        }
        
        setDisposition(a);
    }
    
    private void setDisposition(Appointment a) {
        String fmt;
        String time;
        
        disposition.setVerticalAlignment(JLabel.TOP);
        disposition.setHorizontalAlignment(JLabel.LEFT);
        long d = a.getEventTime() - System.currentTimeMillis();
        boolean now = false;
        boolean overdue = false;
        if (d < 0) {
            d *= -1;
            overdue = true;
        }
        int days    = (int) TimeUnit.DAYS.convert(d, TimeUnit.MILLISECONDS);
        int hours   = (int) TimeUnit.HOURS.convert(d, TimeUnit.MILLISECONDS);
        int minutes = (int) TimeUnit.MINUTES.convert(d, TimeUnit.MILLISECONDS);
        
        hours = hours % 24;
        minutes = minutes % 60;
        
        if (days > 0) {
            time = hours != 0 ? format("daysHours", format("days", days),
                    format("hours", hours)) : format("days", days);
        } else if (hours > 0) {
            time = minutes != 0 ? format("hoursMinutes", format("hours", hours),
                    format("minutes", minutes)) : format("hours", hours);
        } else {
            time = format("minutes", minutes + 1);
            if (overdue && minutes == 0) {
                overdue = false;
                now = true;
            }
        }
        if (overdue) {
            fmt = "overdueText";
        } else if (now) {
            fmt = "nowText";
        } else {
            fmt = "inText";
        }
        String text = format(fmt, time);
        
        disposition.setText(text);
    }
    
    private void layout() {
        component.add(name,        "nameText");
        component.add(when,        "whenText");
        component.add(location,    "locationText");
        component.add(locLabel,    "locationLabel");
        component.add(disposition, "dispositionText");
        component.add(dismiss,     "dismissButton");
        
        dismiss.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Action: " + appt.getName());
            }
        });
    }

    public Component getComponent() {
        return component;
    }
    
}

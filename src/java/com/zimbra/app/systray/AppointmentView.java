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
            setComponentVisible("locationText", true);
            location.setText(a.getLocation());
        } else {
            setComponentVisible("locationText", false);
        }
        
        setDisposition(a);
    }
    
    private void setDisposition(Appointment a) {
        String fmt;
        String time;
        
        disposition.setVerticalAlignment(JLabel.TOP);
        disposition.setHorizontalAlignment(JLabel.LEFT);
        long d = a.getEventTime() - System.currentTimeMillis();
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
            time = format("daysHoursMinutes", format("days", days),
                    format("hours", hours), format("minutes", minutes));
        } else if (hours > 0) {
            time = format("hoursMinutes",
                    format("hours", hours), format("minutes", minutes));
        } else {
            time = format("minutes", minutes);
        }
        if (overdue) {
            fmt = "overdueText";
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
    
    private void setComponentVisible(String name, boolean b) {
        Component[] comps = component.getComponents();
        for (Component c : comps) {
            if (name.equals(c.getName())) {
                c.setVisible(b);
                break;
            }
        }
        component.invalidate();
    }
}

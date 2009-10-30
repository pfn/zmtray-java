package com.zimbra.app.systray;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.hanhuy.common.ui.ResourceBundleForm;

public class AppointmentView extends ResourceBundleForm {
    private final AppointmentListView view;
    private Appointment appt;
    private JComponent component = new JPanel();
    
    private final JLabel  locLabel    = new JLabel();
    private final JLabel  name        = new JLabel();
    private final JLabel  when        = new JLabel();
    private final JLabel  location    = new JLabel();
    private final JLabel  disposition = new JLabel();
    private final JButton dismiss     = new JButton();
    
    private final Color locLabelForeground = locLabel.getForeground();

    public AppointmentView(AppointmentListView v) {
        view = v;
        component.setLayout(createLayoutManager());
        layout();
    }
    
    public AppointmentView(AppointmentListView v, Appointment a) {
        this(v);
        appt = a;
        setAppointment(a);
    }

    private void setAppointment(Appointment a) {
        name.setText("<html><b>" + a.getName());
        when.setText(format("eventTime", new Date(a.getEventTime()),
                new Date(a.getEventTime() + a.getDuration())));

        if (a.getLocation() != null && !"".equals(a.getLocation().trim())) {
            locLabel.setForeground(locLabelForeground);
            location.setForeground(locLabelForeground);
            location.setText(a.getLocation());
            locLabel.setText(getString("locationLabel.text"));
        } else {
            locLabel.setForeground(locLabel.getBackground());
            location.setForeground(location.getBackground());
            location.setText("");
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
        int days    = (int) TU.DAYS.convert(d, TU.MILLISECONDS);
        int hours   = (int) TU.HOURS.convert(d, TU.MILLISECONDS);
        int minutes = (int) TU.MINUTES.convert(d, TU.MILLISECONDS);
        
        hours = hours % 24;
        minutes = minutes % 60;
        
        if (days > 0) {
            time = hours != 0 ? format("daysHours", format("days", days),
                    format("hours", hours)) : format("days", days);
        } else if (hours > 0) {
            time = minutes != 0 ? format("hoursMinutes", format("hours", hours),
                    format("minutes", minutes + (overdue ? 0 : 1))) :
                        format("hours", hours);
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
                view.getZimbraTray().dismissAppointments(Arrays.asList(appt));
                view.removeAppointment(appt);
            }
        });
    }

    public Component getComponent() {
        return component;
    }
    
}

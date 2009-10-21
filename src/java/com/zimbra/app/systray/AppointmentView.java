package com.zimbra.app.systray;

import java.awt.Component;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.hanhuy.common.ui.ResourceBundleForm;

public class AppointmentView extends ResourceBundleForm {
    private JComponent component = new JPanel();
    
    private JLabel name     = new JLabel();
    private JLabel when     = new JLabel();
    private JLabel location = new JLabel();

    public AppointmentView() {
        component.setLayout(createLayoutManager());
        layout();
    }

    private void layout() {
        component.add(name,     "nameText");
        component.add(when,     "whenText");
        component.add(location, "locationText");
    }

    public Component getComponent() {
        return component;
    }
    
    public void setAppointment(Appointment a) {
        name.setText(a.getName());
        when.setText(format("eventTime", new Date(a.getEventTime())));

        if (a.getLocation() != null && !"".equals(a.getLocation().trim())) {
            setComponentVisible("locationText", true);
            location.setText(a.getLocation());
        } else {
            setComponentVisible("locationText", false);
        }
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

package com.zimbra.app.systray.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import com.zimbra.app.systray.Prefs;
import com.zimbra.app.systray.ZimbraTray;

import com.hanhuy.common.ui.ResourceBundleForm;


public class GeneralOptionsForm extends ResourceBundleForm {
    private JPanel panel = new JPanel();
    private JRadioButton mTopLeft     = new JRadioButton();
    private JRadioButton mTopRight    = new JRadioButton();
    private JRadioButton mCenter      = new JRadioButton();
    private JRadioButton mBottomLeft  = new JRadioButton();
    private JRadioButton mBottomRight = new JRadioButton();

    private JRadioButton cTopLeft     = new JRadioButton();
    private JRadioButton cTopRight    = new JRadioButton();
    private JRadioButton cCenter      = new JRadioButton();
    private JRadioButton cBottomLeft  = new JRadioButton();
    private JRadioButton cBottomRight = new JRadioButton();

    public GeneralOptionsForm(ZimbraTray zt) {
        layout();
    }

    private void layout() {
        panel.setLayout(createLayoutManager());
        Prefs prefs = Prefs.getPrefs();
        
        JRadioButton c = null;
        switch (prefs.getAppointmentAlertLocation()) {
        case TOP_LEFT:
            c = cTopLeft;
            break;
        case TOP_RIGHT:
            c = cTopRight;
            break;
        case CENTER:
            c = cCenter;
            break;
        case BOTTOM_RIGHT:
            c = cBottomRight;
            break;
        case BOTTOM_LEFT:
            c = cBottomLeft;
            break;
        }
        c.setSelected(true);
        
        switch (prefs.getMessageAlertLocation()) {
        case TOP_LEFT:
            c = mTopLeft;
            break;
        case TOP_RIGHT:
            c = mTopRight;
            break;
        case CENTER:
            c = mCenter;
            break;
        case BOTTOM_RIGHT:
            c = mBottomRight;
            break;
        case BOTTOM_LEFT:
            c = mBottomLeft;
            break;
        }
        c.setSelected(true);

        ButtonGroup g = new ButtonGroup();
        g.add(mTopLeft);
        g.add(mTopRight);
        g.add(mCenter);
        g.add(mBottomLeft);
        g.add(mBottomRight);

        panel.add(mTopLeft,     "mTopLeft");
        panel.add(mTopRight,    "mTopRight");
        panel.add(mCenter,      "mCenter");
        panel.add(mBottomLeft,  "mBottomLeft");
        panel.add(mBottomRight, "mBottomRight");

        g = new ButtonGroup();
        g.add(cTopLeft);
        g.add(cTopRight);
        g.add(cCenter);
        g.add(cBottomLeft);
        g.add(cBottomRight);

        panel.add(cTopLeft,     "cTopLeft");
        panel.add(cTopRight,    "cTopRight");
        panel.add(cCenter,      "cCenter");
        panel.add(cBottomLeft,  "cBottomLeft");
        panel.add(cBottomRight, "cBottomRight");
        
        ActionListener al = new AppointmentLocationActionListener();
        ActionListener ml = new MessageLocationActionListener();
        mTopLeft.setActionCommand("" + Prefs.ScreenLocation.TOP_LEFT.ordinal());
        mTopRight.setActionCommand(
                "" + Prefs.ScreenLocation.TOP_RIGHT.ordinal());
        mCenter.setActionCommand("" + Prefs.ScreenLocation.CENTER.ordinal());
        mBottomRight.setActionCommand(
                "" + Prefs.ScreenLocation.BOTTOM_RIGHT.ordinal());
        mBottomLeft.setActionCommand(
                "" + Prefs.ScreenLocation.BOTTOM_LEFT.ordinal());
        mTopLeft.addActionListener(ml);
        mTopRight.addActionListener(ml);
        mCenter.addActionListener(ml);
        mBottomRight.addActionListener(ml);
        mBottomLeft.addActionListener(ml);
        cTopLeft.setActionCommand("" + Prefs.ScreenLocation.TOP_LEFT.ordinal());
        cTopRight.setActionCommand(
                "" + Prefs.ScreenLocation.TOP_RIGHT.ordinal());
        cCenter.setActionCommand("" + Prefs.ScreenLocation.CENTER.ordinal());
        cBottomRight.setActionCommand(
                "" + Prefs.ScreenLocation.BOTTOM_RIGHT.ordinal());
        cBottomLeft.setActionCommand(
                "" + Prefs.ScreenLocation.BOTTOM_LEFT.ordinal());
        cTopLeft.addActionListener(al);
        cTopRight.addActionListener(al);
        cCenter.addActionListener(al);
        cBottomRight.addActionListener(al);
        cBottomLeft.addActionListener(al);
    }

    public Component getComponent() {
        return panel;
    }
    
    private class AppointmentLocationActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            int ord = Integer.parseInt(cmd);
            Prefs.getPrefs().setAppointmentAlertLocation(
                    Prefs.ScreenLocation.values()[ord]);
        }
    }
    private class MessageLocationActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            int ord = Integer.parseInt(cmd);
            Prefs.getPrefs().setMessageAlertLocation(
                    Prefs.ScreenLocation.values()[ord]);
        }
    }
}

package com.zimbra.app.systray.options;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

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
    }

    public Component getComponent() {
        return panel;
    }
}

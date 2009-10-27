package com.zimbra.app.systray.options;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JButton;

import com.zimbra.app.systray.ZimbraTray;

import com.hanhuy.common.ui.ResourceBundleForm;


public class AdvancedOptionsForm extends ResourceBundleForm {

    private JPanel panel = new JPanel();

    private JCheckBox soapDebug = new JCheckBox();
    private JButton showConsoleButton = new JButton();

    public AdvancedOptionsForm(ZimbraTray zt) {
        layout();
    }

    private void layout() {
        panel.setLayout(createLayoutManager());
        panel.add(soapDebug,         "soapDebug");
        panel.add(showConsoleButton, "showConsoleButton");
    }

    public Component getComponent() {
        return panel;
    }
}

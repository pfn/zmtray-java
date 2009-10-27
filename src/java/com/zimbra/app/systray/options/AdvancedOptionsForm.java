package com.zimbra.app.systray.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JButton;

import com.zimbra.app.systray.ZimbraTray;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.ConsoleViewer;


public class AdvancedOptionsForm extends ResourceBundleForm {

    private ZimbraTray zt;
    private JPanel panel = new JPanel();

    private JCheckBox soapDebug = new JCheckBox();
    private JButton showConsoleButton = new JButton();

    public AdvancedOptionsForm(ZimbraTray zt) {
        this.zt = zt;
        layout();
    }

    private void layout() {
        panel.setLayout(createLayoutManager());
        panel.add(soapDebug,         "soapDebug");
        panel.add(showConsoleButton, "showConsoleButton");

        showConsoleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ConsoleViewer(zt.HIDDEN_PARENT);
            }
        });
    }

    public Component getComponent() {
        return panel;
    }
}

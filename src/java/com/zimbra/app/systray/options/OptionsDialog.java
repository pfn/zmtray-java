package com.zimbra.app.systray.options;

import com.zimbra.app.systray.ZimbraTray;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;

import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;

public class OptionsDialog extends ResourceBundleForm {
    private JTabbedPane tabs;
    public OptionsDialog(ZimbraTray zt) {
        tabs = new JTabbedPane();
        tabs.addTab(getString("generalTab"), new JLabel("g"));
        tabs.addTab(getString("soundsTab"), new JLabel("s"));
        tabs.addTab(getString("newAccountTab"),
                new NewAccountForm(zt).getComponent());
        tabs.addTab(getString("accountsTab"), new JLabel("accts"));
        tabs.addTab(getString("advancedTab"), new JLabel("adv"));
        JDialog d = new JDialog(zt.HIDDEN_PARENT, getString("title"));
        d.add(tabs);
        d.pack();
        Util.centerWindow(d);
        d.setVisible(true);
    }
}

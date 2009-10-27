package com.zimbra.app.systray.options;

import com.zimbra.app.systray.ZimbraTray;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;

import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;

public class OptionsDialog extends ResourceBundleForm {
    private JTabbedPane tabs;
    // TODO stop using constructor to show options dialog
    public OptionsDialog(ZimbraTray zt) {
        tabs = new JTabbedPane();
        tabs.addTab(getString("generalTab"), 
                new GeneralOptionsForm(zt).getComponent());
        tabs.addTab(getString("soundsTab"), 
                new SoundsOptionsForm(zt).getComponent());
        tabs.addTab(getString("newAccountTab"),
                new NewAccountForm(zt).getComponent());
        tabs.addTab(getString("accountsTab"),
                new AccountsForm(zt).getComponent());
        tabs.addTab(getString("advancedTab"), 
                new AdvancedOptionsForm(zt).getComponent());
        JDialog d = new JDialog(zt.HIDDEN_PARENT, getString("title"));
        d.add(tabs);
        d.pack();
        Util.centerWindow(d);
        d.setVisible(true);
    }
}
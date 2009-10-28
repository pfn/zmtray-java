package com.zimbra.app.systray.options;

import javax.swing.JDialog;
import javax.swing.JTabbedPane;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;
import com.zimbra.app.systray.ZimbraTray;

public class OptionsDialog extends ResourceBundleForm {
    private JTabbedPane tabs;
    
    private JDialog dlg;
    
    private static OptionsDialog INSTANCE;
    private OptionsDialog(ZimbraTray zt) {
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
        dlg = new JDialog(zt.HIDDEN_PARENT, getString("title"));
        dlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dlg.add(tabs);
        dlg.pack();
    }
    
    public static void showForm(ZimbraTray zt) {
        if (INSTANCE == null)
            INSTANCE = new OptionsDialog(zt);
        Util.centerWindow(INSTANCE.dlg);
        INSTANCE.dlg.setVisible(true);
        INSTANCE.dlg.toFront();
    }

    public static void showNewAccountForm(ZimbraTray zt) {
        if (INSTANCE == null)
            INSTANCE = new OptionsDialog(zt);
        INSTANCE.tabs.setSelectedIndex(
                INSTANCE.tabs.indexOfTab(INSTANCE.getString("newAccountTab")));
        Util.centerWindow(INSTANCE.dlg);
        INSTANCE.dlg.setModal(true);
        INSTANCE.dlg.setVisible(true);
        INSTANCE.dlg.setModal(false);
    }
}

package com.zimbra.app.systray.options;

import com.zimbra.app.systray.Prefs;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.zimbra.app.systray.ZimbraTray;

import com.hanhuy.common.ui.ResourceBundleForm;


public class AccountsForm extends ResourceBundleForm {

    private ZimbraTray zt;
    private JPanel panel           = new JPanel();
    private DefaultListModel model = new DefaultListModel();
    private JList  list            = new JList(model);
    private JButton delete         = new JButton();
    private JButton edit           = new JButton();
    private JScrollPane pane       = new JScrollPane(list,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private static AccountsForm INSTANCE;

    public AccountsForm(ZimbraTray zt) {
        INSTANCE = this;
        this.zt = zt;
        reset();
        layout();
    }

    private void layout() {
        panel.setLayout(createLayoutManager());

        list.setVisibleRowCount(10);
        panel.add(pane, "accountList");
        panel.add(delete, "deleteButton");
        panel.add(edit, "editButton");
    }

    public static void reset() {
        INSTANCE.model.clear();

        List<String> names = Prefs.getPrefs().getAccountNames();
        for (String name : names) {
            INSTANCE.model.addElement(name);
        }
    }

    public Component getComponent() {
        return panel;
    }
}

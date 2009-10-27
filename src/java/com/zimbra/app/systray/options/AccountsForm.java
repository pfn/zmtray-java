package com.zimbra.app.systray.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JOptionPane;

import com.zimbra.app.systray.ZimbraTray;
import com.zimbra.app.systray.Prefs;
import com.zimbra.app.systray.Account;

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

        panel.add(pane,   "accountList");
        panel.add(delete, "deleteButton");
        panel.add(edit,   "editButton");
        
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        delete.setEnabled(false);
        edit.setEnabled(false);
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                boolean selected = list.getSelectedIndex() != -1;
                delete.setEnabled(selected);
                edit.setEnabled(selected);
            }
        });
        edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO implement account edit
            }
        });
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int r = JOptionPane.showConfirmDialog(panel,
                        getString("deleteAccountConfirm"),
                        getString("deleteAccountTitle"),
                        JOptionPane.WARNING_MESSAGE);
                if (r == JOptionPane.YES_OPTION) {
                    String name = (String) list.getSelectedValue();
                    model.removeElement(name);
                    Account a = Prefs.getPrefs().getAccount(name);
                    zt.removeAccount(a);
                }
            }
        });
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

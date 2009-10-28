package com.zimbra.app.systray.options;

import java.awt.CardLayout;
import java.awt.LayoutManager;
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
import com.zimbra.app.systray.AccountHandler;

import com.hanhuy.common.ui.ResourceBundleForm;


public class AccountsForm extends ResourceBundleForm {

    final static String ACCOUNT_LIST_CARD = "accountListCard";
    final static String ACCOUNT_EDIT_CARD = "accountEditCard";

    private AccountEditForm aef;
    private ZimbraTray zt;
    private JPanel panel           = new JPanel();
    private CardLayout cards       = new CardLayout();

    // account list components
    private JPanel  accountListCard = new JPanel();
    private DefaultListModel model  = new DefaultListModel();
    private JList   accountList     = new JList(model);
    private JButton delete          = new JButton();
    private JButton edit            = new JButton();

    private static AccountsForm INSTANCE;

    public AccountsForm(ZimbraTray zt) {
        INSTANCE = this;
        this.zt = zt;
        reset();
        layout();
        aef = new AccountEditForm(zt, this);
        panel.setLayout(cards);
        panel.add(accountListCard, ACCOUNT_LIST_CARD);
        panel.add(aef.getComponent(), ACCOUNT_EDIT_CARD);
        show(ACCOUNT_LIST_CARD);
    }

    private void layout() {
        accountListCard.setLayout(createLayoutManager());

        JScrollPane accountListPane = new JScrollPane(accountList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        accountListCard.add(accountListPane, "accountList");
        accountListCard.add(delete, "deleteButton");
        accountListCard.add(edit,   "editButton");
        
        accountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        delete.setEnabled(false);
        edit.setEnabled(false);
        accountList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                boolean selected = accountList.getSelectedIndex() != -1;
                delete.setEnabled(selected);
                edit.setEnabled(selected);
            }
        });
        edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO implement account edit
                String name = (String) accountList.getSelectedValue();
                aef.setAccount(Prefs.getPrefs().getAccount(name));
                show(ACCOUNT_EDIT_CARD);
            }
        });
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int r = JOptionPane.showConfirmDialog(accountListCard,
                        getString("deleteAccountConfirm"),
                        getString("deleteAccountTitle"),
                        JOptionPane.WARNING_MESSAGE);
                if (r == JOptionPane.YES_OPTION) {
                    String name = (String) accountList.getSelectedValue();
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

    void show(String name) {
        cards.show(panel, name);
    }
    public Component getComponent() {
        return panel;
    }
}

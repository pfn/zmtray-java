package com.zimbra.app.systray.options;

import java.awt.CardLayout;
import java.awt.LayoutManager;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
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

public class AccountEditForm extends ResourceBundleForm {

    private ZimbraTray zt;
    private AccountsForm af;
    private JPanel panel           = new JPanel();

    // account edit components
    private JTextField     name         = new JTextField();
    private JTextField     server       = new JTextField();
    private JTextField     user         = new JTextField();
    private JPasswordField pass         = new JPasswordField();
    private JCheckBox      http         = new JCheckBox();
    private JCheckBox      disabled     = new JCheckBox();
    private JList          folderList   = new JList();
    private JList          calendarList = new JList();
    private JButton        back         = new JButton();
    private JButton        save         = new JButton();


    public AccountEditForm(ZimbraTray zt, AccountsForm af) {
        this.zt = zt;
        this.af = af;
        layout();
    }

    private void layout() {
        panel.setLayout(createLayoutManager());
        folderList.setVisibleRowCount(5);
        calendarList.setVisibleRowCount(5);
        JScrollPane folderPane = new JScrollPane(folderList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane calendarPane = new JScrollPane(calendarList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(disabled,     "disabledField");
        panel.add(name,         "nameField");
        panel.add(server,       "serverField");
        panel.add(user,         "userField");
        panel.add(pass,         "passField");
        panel.add(http,         "httpField");
        //panel.add(folderPane,   "folderList");
        //panel.add(calendarPane, "calendarList");
        panel.add(back,         "backButton");
        panel.add(save,         "saveButton");

        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                af.show(AccountsForm.ACCOUNT_LIST_CARD);
            }
        });
    }

    void setAccount(Account account) {
         name.setText(account.getAccountName());
         server.setText(account.getServer());
         user.setText(account.getLogin());
         pass.setText(account.getPassword());
         http.setSelected(!account.isSSL());
         boolean enabled = account.isEnabled();
         disabled.setSelected(!enabled);
         if (enabled) {
             AccountHandler ah = zt.getAccountHandlerBy(account);
             DefaultListModel folderModel = new DefaultListModel();
             for (String folder : ah.getAvailableMailFolders()) {
                 folderModel.addElement(folder);
             }
             folderList.setModel(folderModel);
             DefaultListModel calendarModel = new DefaultListModel();
             for (String calendar : ah.getAvailableCalendars()) {
                 calendarModel.addElement(calendar);
             }
             calendarList.setModel(calendarModel);
         }
    }

    public Component getComponent() {
        return panel;
    }
}

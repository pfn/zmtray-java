package com.zimbra.app.systray.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.zimbra.app.systray.Account;
import com.zimbra.app.systray.AccountHandler;
import com.zimbra.app.systray.Prefs;
import com.zimbra.app.systray.ZimbraTray;

public class AccountEditForm extends ResourceBundleForm {

    private ZimbraTray zt;
    private AccountsForm af;
    private JPanel panel           = new JPanel();
    private Account account;

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
        panel.add(folderPane,   "folderList");
        panel.add(calendarPane, "calendarList");
        panel.add(back,         "backButton");
        panel.add(save,         "saveButton");

        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                af.show(AccountsForm.ACCOUNT_LIST_CARD);
            }
        });
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (verifySettings()) {
                    Account other = Prefs.getPrefs().getAccount(name.getText());
                    if (other != null &&
                            !account.getId().equals(other.getId())) {
                        JOptionPane.showMessageDialog(panel,
                                getString("accountExistsError"),
                                getString("errorString"),
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    account.setAccountName(name.getText());
                    account.setServer(server.getText());
                    account.setLogin(user.getText());
                    account.setPassword(new String(pass.getPassword()));
                    account.setSSL(!http.isSelected());
                    account.setEnabled(!disabled.isSelected());
                    Object[] folders = folderList.getSelectedValues();
                    Object[] calendars = calendarList.getSelectedValues();
                    ArrayList<String> folderNames = new ArrayList<String>();
                    ArrayList<String> calendarNames = new ArrayList<String>();
                    for (Object o : folders)
                        folderNames.add(o.toString());
                    for (Object o : calendars)
                        calendarNames.add(o.toString());
                    account.setSubscribedCalendarNames(calendarNames);
                    account.setSubscribedMailFolders(folderNames);

                    AccountsForm.reset();
                    zt.resetAccount(account);

                    af.show(AccountsForm.ACCOUNT_LIST_CARD);
                }
            }
        });
    }

    void setAccount(Account account) {
        this.account = account;
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
            List<String> subscribedFolders =
                    account.getSubscribedMailFolders();
            int index = 0;
            ArrayList<Integer> indices = new ArrayList<Integer>();
            for (String folder : ah.getAvailableMailFolders()) {
                folderModel.addElement(folder);
                if (subscribedFolders.contains(folder))
                    indices.add(index);
                index++;
            }
            folderList.setModel(folderModel);
            int[] indexAry = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                indexAry[i] = indices.get(i);
            }
            folderList.setSelectedIndices(indexAry);
            DefaultListModel calendarModel = new DefaultListModel();
            List<String> subscribedCalendars =
                    account.getSubscribedCalendarNames();

            indices.clear();
            index = 0;
            for (String calendar : ah.getAvailableCalendars()) {
                calendarModel.addElement(calendar);
                if (subscribedCalendars.contains(calendar))
                    indices.add(index);
                index++;
            }
            indexAry = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                indexAry[i] = indices.get(i);
            }
            calendarList.setModel(calendarModel);
            calendarList.setSelectedIndices(indexAry);
            setComponentsVisible(new String[] {
                    "folderList", "calendarList",
                    "$label.folder", "$label.calendar" }, true);
        } else {
            setComponentsVisible(new String[] {
                    "folderList", "calendarList",
                    "$label.folder", "$label.calendar" }, false);
        }
    }

    public Component getComponent() {
        return panel;
    }

    private void setComponentsVisible(String[] names, boolean b) {
        HashMap<String,Component> cmap = new HashMap<String,Component>();
        Component[] comps = panel.getComponents();
        for (Component c : comps)
            cmap.put(c.getName(), c);
        for (String name : names) {
            Component c = cmap.get(name);
            if (c != null)
                c.setVisible(b);
        }
        panel.invalidate();
    }

    private boolean verifySettings() {
        String accountName = name.getText();
        String serverName = server.getText();
        String username = user.getText();
        String password = new String(pass.getPassword());
        int[] selectedFolders = folderList.getSelectedIndices();
        int[] selectedCalendars = calendarList.getSelectedIndices();

        String error = null;
        if (accountName == null || accountName.trim().equals(""))
            error = getString("accountNameEmptyError");
        if (serverName == null || serverName.trim().equals(""))
            error = getString("serverNameEmptyError");
        if (username == null || username.trim().equals(""))
            error = getString("usernameEmptyError");
        if (password == null || password.trim().equals(""))
            error = getString("passwordEmptyError");
        if (selectedFolders.length == 0)
            error = getString("noFolderSelectedError");
        if (selectedCalendars.length == 0)
            error = getString("noCalendarSelectedError");
        if (error != null) {
            JOptionPane.showMessageDialog(panel,
                    error, getString("errorString"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
}

package com.zimbra.app.systray;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JPasswordField;

public class NewAccountForm extends ResourceBundleForm {

    private JDialog dialog;
    public NewAccountForm() {
        dialog = new JDialog((JFrame) null, getString("title"), true);
        dialog.setAlwaysOnTop(true);

        layoutDialog();
    }

    public void show() {
        dialog.pack();
        Util.centerWindow(dialog);
        dialog.setVisible(true);
    }

    private void layoutDialog() {
        dialog.setLayout(createLayoutManager());

        JTextField user = new JTextField();
        JTextField pass = new JPasswordField();

        dialog.add(user, "userField");
        dialog.add(pass, "passField");
    }

}

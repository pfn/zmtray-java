package com.zimbra.app.systray;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;

import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JCheckBox;

public class NewAccountForm extends ResourceBundleForm {

    private JDialog dialog;
    public NewAccountForm(ZimbraTray zt) {
        dialog = new JDialog(zt.HIDDEN_PARENT, getString("title"), true);
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

        final JTextField server = new JTextField();
        final JTextField name   = new JTextField();
        final JTextField user   = new JTextField();
        final JTextField pass   = new JPasswordField();
        final JCheckBox  http   = new JCheckBox();

        dialog.add(name,   "nameField");
        dialog.add(server, "serverField");
        dialog.add(user,   "userField");
        dialog.add(pass,   "passField");
        dialog.add(http,   "httpField");

        final boolean[] nameSet = new boolean[1];

        final Runnable nameSetCheck = new Runnable() {
            public void run() {
                nameSet[0] = name.getText().length() > 0;
            }
        };
        final Runnable nameSetter = new Runnable() {
            public void run() {
                if (!nameSet[0])
                    name.setText(server.getText() + " - " + user.getText());
            }
        };
        name.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                EventQueue.invokeLater(nameSetCheck);
            }
        });

        user.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                EventQueue.invokeLater(nameSetter);
            }
        });
        server.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                EventQueue.invokeLater(nameSetter);
            }
        });
    }
}

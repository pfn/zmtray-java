package com.zimbra.app.systray.options;

import java.io.File;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.zimbra.app.systray.ZimbraTray;

import com.hanhuy.common.ui.ResourceBundleForm;


public class SoundsOptionsForm extends ResourceBundleForm {

    private final static String APPT_ACTION = "appointment";
    private final static String MSG_ACTION = "message";

    private JPanel panel = new JPanel();

    private JTextField messageSoundText     = new JTextField();
    private JTextField appointmentSoundText = new JTextField();
    private JButton messageSoundBrowse      = new JButton();
    private JButton appointmentSoundBrowse  = new JButton();
    private JButton messageSoundPlay        = new JButton();
    private JButton appointmentSoundPlay    = new JButton();
    private JButton messageSoundStop        = new JButton();
    private JButton appointmentSoundStop    = new JButton();
    public SoundsOptionsForm(ZimbraTray zt) {
        layout();
    }

    private void layout() {
        panel.setLayout(createLayoutManager());

        ImageIcon playIcon = (ImageIcon) getIcon("playIcon");
        ImageIcon stopIcon = (ImageIcon) getIcon("stopIcon");
        messageSoundPlay.setIcon(playIcon);
        messageSoundStop.setIcon(stopIcon);
        appointmentSoundPlay.setIcon(playIcon);
        appointmentSoundStop.setIcon(stopIcon);

        panel.add(messageSoundText,   "messageSoundText");
        panel.add(messageSoundBrowse, "messageSoundBrowse");
        panel.add(messageSoundPlay,   "messageSoundPlay");
        panel.add(messageSoundStop,   "messageSoundStop");

        panel.add(appointmentSoundText,   "appointmentSoundText");
        panel.add(appointmentSoundBrowse, "appointmentSoundBrowse");
        panel.add(appointmentSoundPlay,   "appointmentSoundPlay");
        panel.add(appointmentSoundStop,   "appointmentSoundStop");

        ActionListener browseListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                FileNameExtensionFilter f = new FileNameExtensionFilter(
                        getString("soundFiles"),
                        "wav", "au", "aiff", "aifc", "snd");
                JFileChooser chooser;
                if (System.getenv("windir") != null) {
                    chooser = new JFileChooser(System.getenv("windir") +
                           File.separator + "Media");
                } else
                    chooser = new JFileChooser();
                chooser.setFileFilter(f);
                int r = chooser.showOpenDialog(panel);
                if (r == JFileChooser.APPROVE_OPTION) {
                    if (APPT_ACTION.equals(cmd)) {
                        appointmentSoundText.setText(
                                chooser.getSelectedFile().getPath());
                    } else if (MSG_ACTION.equals(cmd)) {
                        messageSoundText.setText(
                                chooser.getSelectedFile().getPath());
                    }
                }
            }
        };
        messageSoundBrowse.setActionCommand(MSG_ACTION);
        messageSoundBrowse.addActionListener(browseListener);
        appointmentSoundBrowse.setActionCommand(APPT_ACTION);
        appointmentSoundBrowse.addActionListener(browseListener);
    }

    public Component getComponent() {
        return panel;
    }
}

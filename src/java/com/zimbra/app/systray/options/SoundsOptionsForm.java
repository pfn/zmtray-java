package com.zimbra.app.systray.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.zimbra.app.systray.Prefs;
import com.zimbra.app.systray.ZimbraTray;


public class SoundsOptionsForm extends ResourceBundleForm {

    private volatile Clip audioClip;
    private final static String APPT_ACTION = "appointment";
    private final static String MSG_ACTION  = "message";

    private JPanel panel = new JPanel();

    private JTextField messageSoundText     = new JTextField();
    private JTextField appointmentSoundText = new JTextField();
    private JButton messageSoundBrowse      = new JButton();
    private JButton appointmentSoundBrowse  = new JButton();
    private JButton messageSoundPlay        = new JButton();
    private JButton appointmentSoundPlay    = new JButton();
    private JButton messageSoundStop        = new JButton();
    private JButton appointmentSoundStop    = new JButton();

    private JCheckBox disableSounds         = new JCheckBox();

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

        panel.add(disableSounds, "disableSounds");

        ActionListener browseListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                FileFilter f = new FileFilter() {

                    @Override
                    public boolean accept(File e) {
                        int idx = e.getName().lastIndexOf(".");
                        if (idx == -1)
                            return false;
                        String ext = e.getName().substring(
                                idx + 1).toLowerCase();
                        return Arrays.asList("wav", "au", "aiff", "aifc", "snd")
                                .contains(ext);
                    }

                    @Override
                    public String getDescription() {
                        return getString("soundFiles");
                    }
                };
                        //getString("soundFiles"),
                        //"wav", "au", "aiff", "aifc", "snd");
                JFileChooser chooser;
                if (System.getenv("windir") != null) {
                    chooser = new JFileChooser(System.getenv("windir") +
                           File.separator + "Media");
                } else
                    chooser = new JFileChooser();
                chooser.setFileFilter(f);
                int r = chooser.showOpenDialog(panel);
                if (r == JFileChooser.APPROVE_OPTION) {
                    String filename = chooser.getSelectedFile().getPath();
                    if (APPT_ACTION.equals(cmd)) {
                        appointmentSoundText.setText(filename);
                        Prefs.getPrefs().setAppointmentSound(filename);
                    } else if (MSG_ACTION.equals(cmd)) {
                        messageSoundText.setText(filename);
                        Prefs.getPrefs().setMessageSound(filename);
                    }
                }
            }
        };
        ActionListener playListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                JTextField input;
                if (APPT_ACTION.equals(cmd)) {
                    input = appointmentSoundText;
                } else if (MSG_ACTION.equals(cmd)) {
                    input = messageSoundText;
                } else {
                    return;
                }
                String file = input.getText();
                playClip(file);
            }
        };
        ActionListener stopListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopClip();
            }
        };
        messageSoundPlay.setActionCommand(MSG_ACTION);
        messageSoundPlay.addActionListener(playListener);
        appointmentSoundPlay.setActionCommand(APPT_ACTION);
        appointmentSoundPlay.addActionListener(playListener);
        messageSoundStop.addActionListener(stopListener);
        appointmentSoundStop.addActionListener(stopListener);
        messageSoundBrowse.setActionCommand(MSG_ACTION);
        messageSoundBrowse.addActionListener(browseListener);
        appointmentSoundBrowse.setActionCommand(APPT_ACTION);
        appointmentSoundBrowse.addActionListener(browseListener);
        
        String apptSound = Prefs.getPrefs().getAppointmentSound();
        String msgSound = Prefs.getPrefs().getMessageSound();
        if (apptSound != null && !"".equals(apptSound.trim()))
            appointmentSoundText.setText(apptSound);
        if (msgSound != null && !"".equals(msgSound.trim()))
            messageSoundText.setText(msgSound);
        
        disableSounds.setSelected(Prefs.getPrefs().isSoundDisabled());
        disableSounds.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Prefs.getPrefs().setSoundDisabled(disableSounds.isSelected());
            }
        });
    }

    private void stopClip() {
        if (audioClip != null) {
            if (audioClip.isRunning())
                audioClip.stop();
            audioClip.close();
            audioClip = null;
        }
    }

    private void playClip(String name) {
        if (name == null || "".equals(name.trim()))
            return;
        if (audioClip != null)
            stopClip();

        FileInputStream fin = null;
        try {
            File f = new File(name);
            fin = new FileInputStream(f);
            AudioInputStream ain = AudioSystem.getAudioInputStream(fin);
            audioClip = AudioSystem.getClip();
            audioClip.open(ain);
            audioClip.start();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        catch (UnsupportedAudioFileException e) {
            throw new IllegalStateException(e);
        }
        catch (LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
        finally {
            try {
                if (fin != null)
                    fin.close();
            } catch (IOException e) { }
        }
    }

    public Component getComponent() {
        return panel;
    }
}

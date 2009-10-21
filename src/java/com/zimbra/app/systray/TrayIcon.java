package com.zimbra.app.systray;

import org.jdesktop.swinghelper.tray.JXTrayIcon;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import javax.swing.JOptionPane;
import javax.swing.JDialog;

public class TrayIcon extends JXTrayIcon {
    public TrayIcon(Image image, final ZimbraTray zmtray) {
        super(image);
        addMouseListener(new MouseAdapter() {
            JDialog dlg = null;
            boolean popupTrigger = false;
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!popupTrigger && e.getClickCount() == 1)
                    ; //noop temporarily
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!popupTrigger)
                    popupTrigger = e.isPopupTrigger();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                popupTrigger = e.isPopupTrigger();
            }
        });
    }
}

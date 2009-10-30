package com.zimbra.app.systray;

import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.jdesktop.swinghelper.tray.JXTrayIcon;

public class TrayIcon extends JXTrayIcon {
    public TrayIcon(Image image, final ZimbraTray zmtray) {
        super(image);
        addMouseListener(new MouseAdapter() {
            boolean popupTrigger = false;
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!popupTrigger && e.getClickCount() == 1)
                    zmtray.showNewMessages(true);
                if (!popupTrigger && e.getClickCount() == 2)
                    zmtray.pollNow();
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

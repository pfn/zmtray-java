package com.zimbra.app.systray;

import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.jdesktop.swinghelper.tray.JXTrayIcon;

public class TrayIconSupport {
    Object trayicon;
    public TrayIconSupport(Image image, final ZimbraTray zmtray) {
        trayicon = new _TrayIcon(image, zmtray);
    }
    
    public java.awt.TrayIcon getTrayIcon() {
        return (java.awt.TrayIcon) trayicon;
    }
    
    public void setImage(Image image) {
        ((_TrayIcon) trayicon).setImage(image);
    }
    
    public void setImageAutoSize(boolean b) {
        ((_TrayIcon) trayicon).setImageAutoSize(b);
    }
    
    public void setJPopupMenu(JPopupMenu m) {
        ((_TrayIcon) trayicon).setJPopupMenu(m);
    }
    
    public void setToolTip(String text) {
        ((_TrayIcon) trayicon).setToolTip(text);
    }
    
    /**
     * Workaround for java1.5 vs 1.6 compat
     * No systray is supported on 1.5, just stub it.
     */
    private static class _TrayIcon extends JXTrayIcon {
        public _TrayIcon(Image image, final ZimbraTray zmtray) {
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
}

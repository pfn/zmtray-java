package com.zimbra.app.systray;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.PopupMenu;
import java.awt.MenuItem;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

public class ZimbraTray implements Runnable {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        EventQueue.invokeLater(new ZimbraTray());
    }

    public void run() {
        ImageIcon icon = new ImageIcon(
                getClass().getResource("resources/icons/email.png"));
        SystemTray tray = SystemTray.getSystemTray();
        PopupMenu menu = new PopupMenu();
        MenuItem item = new MenuItem("Test Menu Item");
        menu.add(item);
        TrayIcon trayicon = new TrayIcon(icon.getImage(), "Test", menu);
        try {
            tray.add(trayicon);
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
    }
}

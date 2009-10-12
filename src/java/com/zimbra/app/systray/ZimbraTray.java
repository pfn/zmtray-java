package com.zimbra.app.systray;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

public class ZimbraTray implements Runnable {
    private boolean hasSystemTray = false;
    
    public static void main(String[] args) throws Exception {
        checkIfRunning();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        EventQueue.invokeLater(new ZimbraTray());
    }

    public void run() {
        setupSystemTray();
    }
    
    private static void checkIfRunning() {
        Prefs prefs = Prefs.getPrefs();
        InetAddress localhost;
        try {
            localhost = InetAddress.getByName(null);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        try {
            ServerSocket socket = new ServerSocket(0, 0, localhost);
            int port = socket.getLocalPort();
            prefs.setPort(port);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setupSystemTray() {
        SystemTray tray;
        
        try {
            tray = SystemTray.getSystemTray();
        }
        catch (NoClassDefFoundError e) {
            return;
        }
        hasSystemTray = true;
        
        ImageIcon icon = new ImageIcon(
                getClass().getResource("resources/icons/email.png"));
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

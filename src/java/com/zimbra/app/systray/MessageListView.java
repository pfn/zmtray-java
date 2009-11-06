package com.zimbra.app.systray;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.sun.awt.AWTUtilities;
import com.zimbra.app.systray.AccountHandler.MessageAction;

public class MessageListView extends ResourceBundleForm
implements ListCellRenderer {
    private ZimbraTray zt;
    private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private MessageView view = new MessageView();
    private Future<?> autoCloseFuture;
    
    private final static MessageListView INSTANCE = new MessageListView();
    private JPopupMenu messageMenu;
    private JPopupMenu nothingMenu;
    
    private final JList list = new JList();
    
    private boolean performingAction = false;
    private SlidingDialog dlg;
    private Color background = Color.white;
    private JScrollPane pane = new JScrollPane(
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private Action hideAction = new Action(getString("hideAction"),
                getInt("hideActionMnemonic"),
                getString("hideActionAccelerator"), new Runnable() {
        public void run() {
            dlg.setVisible(false);
        }
    });
    
    private Action dismissAction = new Action(getString("dismissItemAction"),
            getInt("dismissItemMnemonic"),
            getString("dismissItemAccelerator"), new Runnable() {
        @Override
        public void run() {
            Message m = (Message) list.getSelectedValue();
            dismissMessageAlert(m);
        }
    });
    
    private Action openAction = new Action(getString("openItemAction"),
            getInt("openItemMnemonic"),
            getString("openItemAccelerator"), new Runnable() {
        @Override
        public void run() {
            Message m = (Message) list.getSelectedValue();
            Account a = m.getAccount();
            zt.openClient(a, m);
        }
    });
    
    private Action readAction = new Action(getString("markItemReadAction"),
            getInt("markItemReadMnemonic"),
            getString("markItemReadAccelerator"), new Runnable() {
        @Override
        public void run() {
            Message m = (Message) list.getSelectedValue();
            dismissMessageAlert(m);
            doMessageAction(m, MessageAction.READ, null);
        }
    });
    
    private Action flagAction = new Action(getString("flagItemAction"),
            getInt("flagItemMnemonic"),
            getString("flagItemAccelerator"), new Runnable() {
        @Override
        public void run() {
            Message m = (Message) list.getSelectedValue();
            dismissMessageAlert(m);
            doMessageAction(m, MessageAction.FLAG, null);
        }
    });
    
    private Action tagAction = new Action(getString("tagItemAction"),
            getInt("tagItemMnemonic"), getString("tagItemAccelerator"),
            new Runnable() {
        @Override
        public void run() {
            Message m = (Message) list.getSelectedValue();
            AccountHandler ah = zt.getAccountHandlerBy(m.getAccount());
            String tags = JOptionPane.showInputDialog(dlg,
                    getString("tagText"), getString("tagTitle"),
                    JOptionPane.QUESTION_MESSAGE);
            if (tags == null || "".equals(tags.trim()))
                return;
            String[] tagList = tags.split(",");
            for (int i = 0; i < tagList.length; i++)
                tagList[i] = tagList[i].trim();
            ah.tagMessage(m, tagList);
            dismissMessageAlert(m);
        }
    });
    
    private Action moveAction = new Action(getString("moveItemAction"),
            getInt("moveItemMnemonic"), getString("moveItemAccelerator"),
            new Runnable() {
        @Override
        public void run() {
            Message m = (Message) list.getSelectedValue();
            AccountHandler ah = zt.getAccountHandlerBy(m.getAccount());
            List<String> folders = ah.getAvailableMailFolders();
            JList folderList = new JList(folders.toArray());
            folderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane p = new JScrollPane(folderList,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            int r = JOptionPane.showConfirmDialog(dlg,
                    new Object[] { getString("moveItemPrompt"), p },
                    getString("moveItemTitle"), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            Object value = folderList.getSelectedValue();
            if (value == null || r != JOptionPane.OK_OPTION)
                return;
            ah.moveMessage(m, (String) value);
            dismissMessageAlert(m);
        }
    });
    
    private Action junkAction = new Action(getString("junkItemAction"),
            getInt("junkItemMnemonic"), getString("junkItemAccelerator"),
            new Runnable() {
        @Override
        public void run() {
            Message m = (Message) list.getSelectedValue();
            dismissMessageAlert(m);
            doMessageAction(m, MessageAction.SPAM, null);
        }
    });
    
    private Action deleteAction = new Action(getString("deleteItemAction"),
            getInt("deleteItemMnemonic"),
            getString("deleteItemAccelerator"), new Runnable() {
        @Override
        public void run() {
            Message m = (Message) list.getSelectedValue();
            dismissMessageAlert(m);
            doMessageAction(m, MessageAction.TRASH, null);
        }
    });
    
    public Component getListCellRendererComponent(JList list, Object value,
            int idx, boolean selected, boolean focused) {
        Component c;
        if (value instanceof Message) {
            view.setMessage((Message) value);
            c = view.getComponent();
            c.setBackground(background);
            JComponent jc = (JComponent) c;
            EtchedBorder border = new EtchedBorder(EtchedBorder.LOWERED);
            Insets i = border.getBorderInsets(jc);
            if (selected) {
                jc.setBorder(border);
            } else {
                jc.setBorder(new EmptyBorder(i));
            }
            // TODO set message fragment in tooltip instead?
            //jc.setToolTipText(((Message)value).getFragment());
        } else {
            c = defaultRenderer.getListCellRendererComponent(
                list, "<html><b><i>" + value, idx, false, false);
            c.setBackground(background);
        }
        return c;
    }

    private MessageListView() {
        list.setCellRenderer(this);
        list.addMouseListener(new ListMouseListener());

        mapAction(hideAction);
        mapAction(dismissAction);
        mapAction(openAction);
        mapAction(readAction);
        mapAction(flagAction);
        mapAction(tagAction);
        mapAction(moveAction);
        mapAction(junkAction);
        mapAction(deleteAction);
        
        dismissAction.setEnabled(false);
        openAction.setEnabled(false);
        readAction.setEnabled(false);
        flagAction.setEnabled(false);
        tagAction.setEnabled(false);
        moveAction.setEnabled(false);
        junkAction.setEnabled(false);
        deleteAction.setEnabled(false);
        
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                cancelAutoClose();
                Object value = list.getSelectedValue();
                boolean enabled = value instanceof Message;
                dismissAction.setEnabled(enabled);
                openAction.setEnabled(enabled);
                readAction.setEnabled(enabled);
                flagAction.setEnabled(enabled);
                tagAction.setEnabled(enabled);
                moveAction.setEnabled(enabled);
                junkAction.setEnabled(enabled);
                deleteAction.setEnabled(enabled);
            }
        });
        initMenu();
    }

    private void initMenu() {
        JMenuItem item;
        item = new JMenuItem(hideAction);
        nothingMenu = new JPopupMenu();
        nothingMenu.add(item);

        messageMenu = new JPopupMenu();
        
        item = new JMenuItem(dismissAction);
        messageMenu.add(item);
        
        item = new JMenuItem(openAction);
        messageMenu.add(item);
        
        messageMenu.addSeparator();
        
        item = new JMenuItem(readAction);
        messageMenu.add(item);
        item = new JMenuItem(flagAction);
        messageMenu.add(item);
        item = new JMenuItem(tagAction);
        messageMenu.add(item);
        item = new JMenuItem(moveAction);
        messageMenu.add(item);
        
        messageMenu.addSeparator();
        
        item = new JMenuItem(junkAction);
        messageMenu.add(item);
        
        item = new JMenuItem(deleteAction);
        messageMenu.add(item);
        
        messageMenu.addSeparator();
        
        item = new JMenuItem(hideAction);
        messageMenu.add(item);
    }
    
    private static void setWindowTranslucent() {
        try {
            Class.forName("com.sun.awt.AWTUtilities");
            if (AWTUtilities.isTranslucencySupported(
                    AWTUtilities.Translucency.TRANSLUCENT))
                AWTUtilities.setWindowOpacity(INSTANCE.dlg, 0.90f);
        }
        catch (ClassNotFoundException e) { } // ignore
    }

    public static void hideView() {
        if (INSTANCE.dlg != null && INSTANCE.dlg.isVisible()) {
            INSTANCE.dlg.setVisible(false);
        }
    }

    private void useScrollPane() {
        pane.setViewportView(list);
        dlg.setComponent(pane);
        int width = list.getPreferredSize().width;
        pane.setSize(new Dimension(width, getInt("preferredHeight")));
        pane.setPreferredSize(new Dimension(width, getInt("preferredHeight")));
        pane.setMinimumSize(new Dimension(width, getInt("preferredHeight")));
    }
    private void useList() {
        dlg.setComponent(list);
    }
    public static synchronized void showView(
            final ZimbraTray zt, List<?> items) {
        INSTANCE.view.resetPreferredWidth();
        INSTANCE.zt = zt;
        
        SlidingDialog dlg = INSTANCE.dlg;
        if (dlg == null) {
            dlg = new SlidingDialog(zt.HIDDEN_PARENT);
            INSTANCE.dlg = dlg;
            setWindowTranslucent();
            dlg.setAlwaysOnTop(true);
            dlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            dlg.setUndecorated(true);
            EtchedBorder b1 = new EtchedBorder(EtchedBorder.RAISED);
            MatteBorder b2 = new MatteBorder(new Insets(3,3,3,3), Color.blue);
            ((JComponent) dlg.getContentPane()).setBorder(
                    new CompoundBorder(b1, b2));
            
            dlg.addWindowFocusListener(new WindowFocusListener() {
                @Override public void windowGainedFocus(WindowEvent e) {
                } // ignore

                @Override
                public void windowLostFocus(WindowEvent e) {
                    if (!INSTANCE.performingAction) {
                        INSTANCE.setupAutoClose();
                    }
                }

            });
            
        }
        
        INSTANCE._showView(items);
    }
    
    private void cancelAutoClose() {
        if (autoCloseFuture != null) {
            autoCloseFuture.cancel(true);
        }
    }
    private void setupAutoClose() {
        cancelAutoClose();
        int seconds = Prefs.getPrefs().getAutoHideTime();
        if (seconds != -1) {
            autoCloseFuture = zt.getExecutor().schedule(new AutoCloser(),
                        seconds, TimeUnit.SECONDS);
        }
        
    }
    
    private class AutoCloser implements Runnable {
        @Override
        public void run() {
            if (dlg.isVisible())
                dlg.setVisible(false);
            autoCloseFuture = null;
        }
    }
    
    private void _showView(List<?> items) {
        DefaultListModel model = new DefaultListModel();
        for (Object item : items) {
            model.addElement(item);
        }
        list.setModel(model);
        
        dlg.pack();
        int msgCount = 0;
        Enumeration<?> e = model.elements();
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            if (o instanceof Message)
                msgCount++;
        }
        Dimension d = list.getPreferredSize();
        if (d.height > getInt("preferredHeight")) {
            useScrollPane();
        } else {
            useList();
            dlg.pack();
        }
        if (!dlg.isVisible()) {
            setupAutoClose();
            dlg.setScreenLocation(Prefs.getPrefs().getMessageAlertLocation());
            dlg.setVisible(true);
        }
    }

    public static void refreshView(List<?> items) {
        JDialog dlg = INSTANCE.dlg;
        if (dlg == null || !dlg.isVisible())
            return;
        INSTANCE._showView(items);
    }

    private class ListMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(final MouseEvent e) {
            int index = list.locationToIndex(e.getPoint());
            Rectangle r = list.getCellBounds(index, index);
            if (r.contains(e.getPoint())) {
                list.setSelectedIndex(index);
                maybeShowPopup(e);
            }
        }
        public void mouseReleased(final MouseEvent e) {
            int index = list.locationToIndex(e.getPoint());
            Rectangle r = list.getCellBounds(index, index);
            if (r.contains(e.getPoint())) {
                list.setSelectedIndex(index);
                maybeShowPopup(e);
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1)
                return;
            int index = list.locationToIndex(e.getPoint());
            Rectangle r = list.getCellBounds(index, index);
            if (r.contains(e.getPoint())) {
                list.setSelectedIndex(index);
                if (Prefs.getPrefs().getClickToDismiss() &&
                        dismissAction.isEnabled())
                    dismissAction.actionPerformed(null);
            }
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                Object item = list.getSelectedValue();
                JPopupMenu popup;
                if (item instanceof Message)
                    popup = messageMenu;
                else
                    popup = nothingMenu;
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    
    private void dismissMessageAlert(Message m) {
        zt.dismissMessage(m);
    }
    
    private void doMessageAction(final Message m, final MessageAction a,
            final String args) {
        final AccountHandler h = zt.getAccountHandlerBy(m.getAccount());
        zt.getExecutor().submit(new Runnable() {
            public void run() {
                h.doMessageAction(m, a, args);
            }
        });
    }

    class Action extends AbstractAction {
        private Runnable r;
        Action(String name, int mnemonic, String accelerator, Runnable r) {
            putValue(NAME, name);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelerator));
            this.r = r;
        }

        public void actionPerformed(ActionEvent e) {
            performingAction = true;
            int idx = list.getSelectedIndex();
            try {
                r.run();
            }
            finally {
                performingAction = false;
                int s = list.getModel().getSize();
                if (s > 0) {
                    list.setSelectedIndex(s > idx ? idx : s - 1);
                    list.requestFocusInWindow();
                }
            }
        }
    }
    
    private void mapAction(Action action) {
        ActionMap am = list.getActionMap();
        InputMap  im = list.getInputMap();
        am.put(action.getValue(Action.NAME), action);
        im.put((KeyStroke) action.getValue(Action.ACCELERATOR_KEY),
                action.getValue(Action.NAME));
    }
}

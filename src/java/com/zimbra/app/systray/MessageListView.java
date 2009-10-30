package com.zimbra.app.systray;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;
import com.zimbra.app.systray.AccountHandler.MessageAction;

public class MessageListView extends ResourceBundleForm
implements ListCellRenderer {
    private ZimbraTray zt;
    private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private MessageView view = new MessageView();
    
    private final static MessageListView INSTANCE = new MessageListView();
    private final static int SCROLLPANE_THRESHOLD = 5;
    private JPopupMenu messageMenu;
    private JPopupMenu nothingMenu;
    
    private final JList list = new JList();
    
    private JDialog dlg;
    private Color background = Color.white;
    private JScrollPane pane = new JScrollPane(
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    
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

        initMenu();
    }

    // TODO use swing action framework, accelerators, mnemonics, etc.
    private void initMenu() {
        ActionListener hideListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hideView();
            }
        };
        JMenuItem item;
        item = new JMenuItem(getString("hideAlertItem"));
        item.addActionListener(hideListener);
        nothingMenu = new JPopupMenu();
        nothingMenu.add(item);

        messageMenu = new JPopupMenu();
        
        item = new JMenuItem(getString("dismissAlertItem"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Message m = (Message) list.getSelectedValue();
                dismissMessageAlert(m);
            }
        });
        messageMenu.add(item);
        
        item = new JMenuItem(getString("openItem"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Message m = (Message) list.getSelectedValue();
                Account a = m.getAccount();
                zt.openClient(a, m);
            }
        });
        messageMenu.add(item);
        
        messageMenu.addSeparator();
        
        item = new JMenuItem(getString("markItemRead"));
        messageMenu.add(item);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Message m = (Message) list.getSelectedValue();
                dismissMessageAlert(m);
                doMessageAction(m, MessageAction.READ, null);
            }
        });
        item = new JMenuItem(getString("markItemFlag"));
        messageMenu.add(item);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Message m = (Message) list.getSelectedValue();
                dismissMessageAlert(m);
                doMessageAction(m, MessageAction.FLAG, null);
            }
        });
        item = new JMenuItem(getString("tagItem"));
        messageMenu.add(item);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Message m = (Message) list.getSelectedValue();
                /*
                String tags = JOptionPane.showInputDialog(dlg,
                        getString("tagText"), getString("tagTitle"),
                        JOptionPane.QUESTION_MESSAGE);
                if (tags == null || "".equals(tags.trim()))
                    return;
                 */
                // TODO implement tagItem
                //doMessageAction(m, MessageAction.UPDATE, tags);
                JOptionPane.showMessageDialog(dlg,
                        "Tagging messages is not implemented, yet",
                        "Not yet implemented", JOptionPane.INFORMATION_MESSAGE);
                //dismissMessageAlert(m);
            }
        });
        item = new JMenuItem(getString("moveItem"));
        messageMenu.add(item);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Message m = (Message) list.getSelectedValue();
                JOptionPane.showMessageDialog(dlg,
                        "Moving messages is not implemented, yet",
                        "Not yet implemented", JOptionPane.INFORMATION_MESSAGE);
                //dismissMessageAlert(m);
                // TODO implement moveItem
            }
        });
        
        messageMenu.addSeparator();
        
        item = new JMenuItem(getString("markItemJunk"));
        messageMenu.add(item);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Message m = (Message) list.getSelectedValue();
                dismissMessageAlert(m);
                doMessageAction(m, MessageAction.SPAM, null);
            }
        });
        
        item = new JMenuItem(getString("deleteItem"));
        messageMenu.add(item);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Message m = (Message) list.getSelectedValue();
                dismissMessageAlert(m);
                doMessageAction(m, MessageAction.TRASH, null);
            }
        });
        
        messageMenu.addSeparator();
        
        item = new JMenuItem(getString("hideAlertItem"));
        item.addActionListener(hideListener);
        messageMenu.add(item);
    }
    
    private static void setWindowTranslucent() {
        try {
            Class.forName("com.sun.awt.AWTUtilities");
            com.sun.awt.AWTUtilities.setWindowOpacity(INSTANCE.dlg, 0.90f);
        }
        catch (ClassNotFoundException e) { } // ignore
    }

    public static void hideView() {
        if (INSTANCE.dlg != null && INSTANCE.dlg.isVisible()) {
            INSTANCE.dlg.setVisible(false);
        }
    }

    private int useScrollPane() {
        dlg.getContentPane().removeAll();
        pane.setViewportView(list);
        dlg.add(pane);
        return pane.getVerticalScrollBar().getPreferredSize().width;
    }
    private void useList() {
        dlg.getContentPane().removeAll();
        dlg.add(list);
    }
    // TODO add animation
    public static synchronized void showView(ZimbraTray zt, List<?> items) {
        INSTANCE.view.resetPreferredWidth();
        INSTANCE.zt = zt;
        
        JDialog dlg = INSTANCE.dlg;
        if (dlg == null) {
            dlg = new JDialog(zt.HIDDEN_PARENT);
            INSTANCE.dlg = dlg;
            setWindowTranslucent();
            dlg.setAlwaysOnTop(true);
            INSTANCE.useList();
            dlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            dlg.setUndecorated(true);
            EtchedBorder b1 = new EtchedBorder(EtchedBorder.RAISED);
            MatteBorder b2 = new MatteBorder(new Insets(3,3,3,3), Color.blue);
            ((JComponent) dlg.getContentPane()).setBorder(
            new CompoundBorder(b1, b2));
        }
        
        _showView(items);
    }
    
    private static void _showView(List<?> items) {
        DefaultListModel model = new DefaultListModel();
        for (Object item : items) {
            model.addElement(item);
        }
        INSTANCE.list.setModel(model);
        
        final JDialog dlg = INSTANCE.dlg;
        dlg.addComponentListener(new ComponentAdapter() {
            Dimension d = null;
            public void componentResized(ComponentEvent e) {
                if (d == null || !d.equals(dlg.getSize())) {
                    setWindowLocation(dlg);
                }
                d = dlg.getSize();
            }
        });
        dlg.pack();
        int msgCount = 0;
        Enumeration<?> e = model.elements();
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            if (o instanceof Message)
                msgCount++;
        }
        if (msgCount > SCROLLPANE_THRESHOLD) {
            int width = dlg.getSize().width + INSTANCE.useScrollPane();
            Component c = INSTANCE.getListCellRendererComponent(null,
                    model.elementAt(1), 0, false, false);
            dlg.setSize(new Dimension(width, SCROLLPANE_THRESHOLD *
                    c.getPreferredSize().height));
        } else {
            INSTANCE.useList();
            dlg.pack();
        }
        setWindowLocation(dlg);
        if (!dlg.isVisible())
            dlg.setVisible(true);
        dlg.toFront();
    }

    public static void refreshView(List<?> items) {
        JDialog dlg = INSTANCE.dlg;
        if (dlg == null || !dlg.isVisible())
            return;
        _showView(items);
    }

    private static void setWindowLocation(JDialog dlg) {
        Dimension size = dlg.getSize();
        Rectangle r = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        Prefs.ScreenLocation l = Prefs.getPrefs().getMessageAlertLocation();
        int x = 0, y = 0;
        switch (l) {
        case TOP_LEFT:
            x = r.x;
            y = r.y;
            break;
        case TOP_RIGHT:
            x = r.width - size.width + r.x;
            y = r.y;
            break;
        case BOTTOM_RIGHT:
            x = r.width - size.width + r.x;
            y = r.height - size.height + r.y;
            break;
        case BOTTOM_LEFT:
            x = r.x;
            y = r.height - size.height + r.y;
            break;
        }
        if (Prefs.ScreenLocation.CENTER == l) {
            Util.centerWindow(dlg);
        } else {
            dlg.setLocation(x, y);
        }
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
}

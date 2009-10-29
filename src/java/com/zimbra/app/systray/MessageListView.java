package com.zimbra.app.systray;

import java.awt.EventQueue;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.List;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.JButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import com.hanhuy.common.ui.Util;
import com.hanhuy.common.ui.ResourceBundleForm;

public class MessageListView extends ResourceBundleForm
implements ListCellRenderer {
    private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private MessageView view = new MessageView();
    
    private final static MessageListView INSTANCE = new MessageListView();
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
        messageMenu.add(item);
        messageMenu.addSeparator();
        item = new JMenuItem(getString("markItemRead"));
        messageMenu.add(item);
        item = new JMenuItem(getString("markItemJunk"));
        messageMenu.add(item);
        item = new JMenuItem(getString("tagItem"));
        messageMenu.add(item);
        item = new JMenuItem(getString("moveItem"));
        messageMenu.add(item);
        
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
    // TODO allow dismissing new messages by clicking on them
    // TODO add animation
    // TODO allow mark-read, file-into-folder, tag-message, junk or delete
    public static synchronized void showView(ZimbraTray zt, List<?> items) {
        INSTANCE.view.resetPreferredWidth();
        
        DefaultListModel model = new DefaultListModel();
        for (Object item : items) {
            model.addElement(item);
        }
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
        
        INSTANCE.list.setModel(model);
        
        final JDialog fdlg = dlg;
        dlg.addComponentListener(new ComponentAdapter() {
            Dimension d = null;
            public void componentResized(ComponentEvent e) {
                if (d == null || !d.equals(fdlg.getSize())) {
                    setWindowLocation(fdlg);
                }
                d = fdlg.getSize();
            }
        });
        dlg.pack();
        if (items.size() > 5) {
            int width = dlg.getSize().width + INSTANCE.useScrollPane();
            Component c = INSTANCE.getListCellRendererComponent(null,
                    items.get(1), 0, false, false);
            dlg.setSize(new Dimension(width, 5 *
                    c.getPreferredSize().height));
        } else {
            INSTANCE.useList();
        }
        setWindowLocation(dlg);
        if (!dlg.isVisible())
            dlg.setVisible(true);
        dlg.toFront();
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
}

package com.zimbra.app.systray;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.List;

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

//import com.sun.awt.AWTUtilities;

public class MessageListView implements ListCellRenderer {
    private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private MessageView view = new MessageView();
    
    private final static MessageListView INSTANCE = new MessageListView();
    
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
            if (selected && focused) {
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
    // TODO honor screen location pref
    // TODO add animation
    // TODO allow mark-read, file-into-folder or tag-message
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
        }
        
        INSTANCE.list.setModel(model);
        
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
        if (!dlg.isVisible())
            dlg.setVisible(true);
        dlg.toFront();
    }
}

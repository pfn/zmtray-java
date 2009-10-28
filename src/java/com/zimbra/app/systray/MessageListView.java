package com.zimbra.app.systray;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

//import com.sun.awt.AWTUtilities;

public class MessageListView implements ListCellRenderer {
    private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private MessageView view = new MessageView();
    
    private final static MessageListView INSTANCE = new MessageListView();
    
    private final JList list;
    
    private JDialog dlg;
    private Color background = Color.white;
    
    public Component getListCellRendererComponent(JList list, Object value,
            int idx, boolean selected, boolean focused) {
        Component c;
        if (value instanceof Message) {
            view.setMessage((Message) value);
            c = view.getComponent();
            c.setBackground(background);
        } else {
            c = defaultRenderer.getListCellRendererComponent(
                list, "<html><b><i>" + value, idx, false, false);
            c.setBackground(background);
        }
        return c;
    }

    private MessageListView() {
        list = new JList();
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

    // TODO allow dismissing new messages by clicking on them
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
            dlg.add(INSTANCE.list);
            dlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        }
        
        INSTANCE.list.setModel(model);
        
        dlg.pack();
        if (!dlg.isVisible())
            dlg.setVisible(true);
        dlg.toFront();
    }
}

package com.zimbra.app.systray;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class MessageListView implements ListCellRenderer {
    private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private MessageView view = new MessageView();
    
    private final static MessageListView INSTANCE = new MessageListView();
    
    private final JList list;
    
    private JDialog dlg;
    private Color background;
    
    public Component getListCellRendererComponent(JList list, Object value,
            int idx, boolean selected, boolean focused) {
        Component c;
        if (value instanceof Message) {
            view.setMessage((Message) value);
            c = view.getComponent();
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
        
        background = view.getComponent().getBackground();
        
    }

    public static void showView(ZimbraTray zt, List<?> items) {
        INSTANCE.view.resetPreferredWidth();
        
        DefaultListModel model = new DefaultListModel();
        for (Object item : items) {
            model.addElement(item);
        }
        JDialog dlg = INSTANCE.dlg;
        if (dlg == null) {
            dlg = new JDialog(zt.HIDDEN_PARENT);
            dlg.add(INSTANCE.list);
            INSTANCE.dlg = dlg;
        }
        
        INSTANCE.list.setModel(model);
        
        dlg.pack();
        dlg.setVisible(true);
        dlg.toFront();
        dlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    }
}

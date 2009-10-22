package com.zimbra.app.systray;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class AppointmentListView implements ListCellRenderer {
    private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private AppointmentView view = new AppointmentView();
    
    private final static AppointmentListView INSTANCE =
            new AppointmentListView();
    
    private final JList list;
    
    private JDialog dlg;
    private Color background;
    private final DefaultListModel model;
    
    public Component getListCellRendererComponent(JList list, Object value,
            int idx, boolean selected, boolean focused) {
        view.setAppointment((Appointment) value);
        return view.getComponent();
    }

    private AppointmentListView() {
        model = new DefaultListModel();
        list  = new JList(model);
        list.setCellRenderer(this);
        
        background = view.getComponent().getBackground();
    }

    public static void hideView() {
        if (INSTANCE.dlg != null && INSTANCE.dlg.isVisible()) {
            INSTANCE.dlg.setVisible(false);
        }
    }

    public static void showView(ZimbraTray zt, Appointment appt) {
        
        INSTANCE.model.addElement(appt);
        JDialog dlg = INSTANCE.dlg;
        if (dlg == null) {
            dlg = new JDialog(zt.HIDDEN_PARENT);
            dlg.setAlwaysOnTop(true);
            dlg.add(INSTANCE.list);
            INSTANCE.dlg = dlg;
        }
        
        dlg.pack();
        dlg.setVisible(true);
        dlg.toFront();
        dlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    }
}

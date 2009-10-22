package com.zimbra.app.systray;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;
import com.sun.awt.AWTUtilities;

public class AppointmentListView extends ResourceBundleForm
implements TableCellRenderer {
    private boolean rowHeightSet;
    private JPanel panel = new JPanel();
    private final HashMap<Appointment,AppointmentView> appointmentViewCache =
            new HashMap<Appointment, AppointmentView>();
    
    private int lastRowCount = 0;
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int col) {
        Appointment a = (Appointment) value;
        AppointmentView view = getAppointmentView(a);

        if (a.isDismissed()) {
            removeAppointment(row);
        }
        
        if (!rowHeightSet) {
            table.setRowHeight(view.getComponent().getPreferredSize().height
                    + table.getRowMargin());
            rowHeightSet = true;
        }

        int rowCount = model.getRowCount();
        if (rowCount != lastRowCount) {
            dlg.pack();
            Util.centerWindow(dlg);
        }
        lastRowCount = rowCount;
        return view.getComponent();
    }
    
    private void removeAppointment(final int row) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                model.removeRow(row);
                showView(null, null);
            }
        });
    }
    
    private AppointmentView getAppointmentView(Appointment a) {
        if (!appointmentViewCache.containsKey(a)) {
            appointmentViewCache.put(a, new AppointmentView(a));
        }
        return appointmentViewCache.get(a);
    }

    private final static AppointmentListView INSTANCE =
            new AppointmentListView();
    
    private final JTable table;
    
    private JDialog dlg;
    private final DefaultTableModel model;
    
    private AppointmentListView() {
        model = new DefaultTableModel();
        table = new JTable(model);
        table.setDefaultRenderer(Appointment.class, this);
        
        model.addColumn("Appointment Reminders");
        TableColumn c = table.getColumn("Appointment Reminders");
        c.setCellRenderer(this);
        c.setCellEditor(new TableCellEditor());
        c.setPreferredWidth(getInt("preferredWidth"));
        
        layout();
    }

    private void layout() {
        JButton snooze = new JButton();
        JButton dismiss = new JButton();
        
        panel.setLayout(createLayoutManager());
        panel.add(snooze, "snoozeButton");
        panel.add(dismiss, "dismissButton");
    }
    public static void hideView() {
        if (INSTANCE.dlg != null && INSTANCE.dlg.isVisible()) {
            INSTANCE.dlg.setVisible(false);
        }
    }

    private static void setWindowTranslucent() {
        try {
            Class.forName("com.sun.awt.AWTUtilities");
            AWTUtilities.setWindowOpacity(INSTANCE.dlg, 0.85f);
        }
        catch (ClassNotFoundException e) { } // ignore
    }
    public static void showView(ZimbraTray zt, Appointment appt) {
        
        if (appt != null)
            INSTANCE.model.addRow(new Object[] { appt });
        
        JDialog dlg = INSTANCE.dlg;
        if (dlg == null) {
            dlg = new JDialog(zt.HIDDEN_PARENT, INSTANCE.getString("title"));
            dlg.setAlwaysOnTop(true);
            dlg.add(INSTANCE.table);
            dlg.add(INSTANCE.panel, BorderLayout.SOUTH);
            //dlg.setUndecorated(true);
            INSTANCE.dlg = dlg;
            JComponent c = (JComponent) dlg.getContentPane();
            c.setBorder(new LineBorder(Color.black));
            setWindowTranslucent();
            dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        }
        int rows = INSTANCE.model.getRowCount();
        Dimension d = INSTANCE.table.getPreferredSize();
        d.height = rows * (INSTANCE.table.getRowHeight()
                + INSTANCE.table.getRowMargin());
        
        dlg.pack();
        if (!dlg.isVisible() && INSTANCE.model.getRowCount() > 0) {
            Util.centerWindow(dlg);
            dlg.setVisible(true);
            dlg.toFront();
        }
        if (INSTANCE.model.getRowCount() == 0)
            dlg.setVisible(false);
    }
    
    private class TableCellEditor extends DefaultCellEditor {
        private Object value;
        TableCellEditor() {
            super(new JTextField());
            setClickCountToStart(1);
        }

        @Override
        public Object getCellEditorValue() {
            return value;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean selected, int row, int col) {
            this.value = value;
            Appointment a = (Appointment) value;
            AppointmentView view = new AppointmentView(a);
            if (a.isDismissed()) {
                removeAppointment(row);
            }
        
            appointmentViewCache.put(a, view);
            return view.getComponent();
        }
    }
}

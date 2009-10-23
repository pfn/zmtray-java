package com.zimbra.app.systray;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.hanhuy.common.ui.Util;
import com.sun.awt.AWTUtilities;

public class AppointmentListView extends ResourceBundleForm
implements TableCellRenderer {
    private ZimbraTray zmtray;
    private final static int[] SNOOZE_TIMES = {
        1  * 60 * 1000,
        5  * 60 * 1000,
        10 * 60 * 1000,
        15 * 60 * 1000,
        30 * 60 * 1000,
        45 * 60 * 1000,
        1  * 60 * 60 * 1000,
        2  * 60 * 60 * 1000,
        3  * 60 * 60 * 1000,
        4  * 60 * 60 * 1000,
        6  * 60 * 60 * 1000,
        8  * 60 * 60 * 1000,
        10 * 60 * 60 * 1000,
        12 * 60 * 60 * 1000,
        24 * 60 * 60 * 1000,
    };
    
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
        }
        lastRowCount = rowCount;
        return view.getComponent();
    }
    
    public void removeAppointment(Appointment a) {
        int count = model.getRowCount();
        boolean found = false;
        int i;
        for (i = 0; i < count && !found; i++) {
            found = a.equals(model.getValueAt(i, 0));
        }
        
        if (found) {
            System.out.println(a.getName() + ": found at: " + (i - 1));
            removeAppointment(i - 1);
        } else {
            System.out.println(a.getName() + ": not found in view");
        }
    }
    private void removeAppointment(final int row) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                cellEditor.cancelCellEditing();
                model.removeRow(row);
                showView(null, null);
            }
        });
    }
    
    private AppointmentView getAppointmentView(Appointment a) {
        if (!appointmentViewCache.containsKey(a)) {
            appointmentViewCache.put(a, new AppointmentView(this, a));
        }
        return appointmentViewCache.get(a);
    }

    private final static AppointmentListView INSTANCE =
            new AppointmentListView();
    
    private final JTable table;
    
    private JDialog dlg;
    private final DefaultTableModel model;
    private final TableCellEditor cellEditor;
    
    private AppointmentListView() {
        model = new DefaultTableModel();
        table = new JTable(model);
        table.setDefaultRenderer(Appointment.class, this);
        
        model.addColumn("Appointment Reminders");
        TableColumn c = table.getColumn("Appointment Reminders");
        c.setCellRenderer(this);
        cellEditor = new TableCellEditor();
        c.setCellEditor(cellEditor);
        c.setPreferredWidth(getInt("preferredWidth"));
        
        layout();
    }

    private String[] generateSnoozeStrings() {
        ArrayList<String> strings = new ArrayList<String>();
        
        for (int time : SNOOZE_TIMES) {
            int hours   = (int) TimeUnit.HOURS.convert(
                    time, TimeUnit.MILLISECONDS);
            int minutes = (int) TimeUnit.MINUTES.convert(
                    time, TimeUnit.MILLISECONDS);
            if (hours == 0) {
                strings.add(format("minutes", minutes));
            } else {
                strings.add(format("hours", hours));
            }
        }
        
        return strings.toArray(new String[strings.size()]);
    }
    
    private void layout() {
        JButton snooze = new JButton();
        JButton dismiss = new JButton();
        final JComboBox snoozeTime = new JComboBox(generateSnoozeStrings());
        
        panel.setLayout(createLayoutManager());
        panel.add(snooze,     "snoozeButton");
        panel.add(snoozeTime, "snoozeTimes");
        panel.add(dismiss,    "dismissButton");
        
        snooze.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = snoozeTime.getSelectedIndex();
                int snooze = SNOOZE_TIMES[idx];
                int rows = model.getRowCount();
                for (int i = 0; i < rows; i++) {
                    Appointment a = (Appointment) model.getValueAt(i, 0);
                    a.snoozeAlarm(snooze);
                }
                model.setRowCount(0);
                snoozeTime.requestFocusInWindow();
                hideView();
            }
        });
        dismiss.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<Appointment> appointments =
                        new ArrayList<Appointment>();
                int rows = model.getRowCount();
                for (int i = 0; i < rows; i++) {
                    appointments.add((Appointment) model.getValueAt(i, 0));
                }
                getZimbraTray().dismissAppointments(appointments);
                model.setRowCount(0);
                hideView();
            }
        });
    }
    private void hideView() {
        if (dlg != null && dlg.isVisible()) {
            dlg.setVisible(false);
        }
    }

    private static void setWindowTranslucent() {
        try {
            Class.forName("com.sun.awt.AWTUtilities");
            AWTUtilities.setWindowOpacity(INSTANCE.dlg, 0.90f);
        }
        catch (ClassNotFoundException e) { } // ignore
    }
    public static void showView(ZimbraTray zt, Appointment appt) {
        
        if (zt != null)
            INSTANCE.zmtray = zt;

        INSTANCE.appointmentViewCache.clear();
        if (appt != null)
            INSTANCE.model.addRow(new Object[] { appt });
        
        JDialog dlg = INSTANCE.dlg;
        if (dlg == null) {
            dlg = new JDialog(zt.HIDDEN_PARENT, INSTANCE.getString("title"));
            dlg.setAlwaysOnTop(true);
            dlg.add(INSTANCE.table);
            dlg.add(INSTANCE.panel, BorderLayout.SOUTH);
            INSTANCE.dlg = dlg;
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
        }
        dlg.toFront();
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
            AppointmentView view = new AppointmentView(
                    AppointmentListView.this, a);
            if (a.isDismissed()) {
                removeAppointment(row);
            }
        
            appointmentViewCache.put(a, view);
            return view.getComponent();
        }
    }
    
    ZimbraTray getZimbraTray() {
        return zmtray;
    }
}

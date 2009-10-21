package com.zimbra.app.systray;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.hanhuy.common.ui.ResourceBundleForm;

public class MessageView extends ResourceBundleForm {
    private JComponent component = new JPanel();
    
    private JLabel from     = new JLabel();
    private JLabel subject  = new JLabel();
    private JLabel fragment = new JLabel();
    
    public MessageView() {
        component.setLayout(createLayoutManager());
        layout();
    }
    public MessageView(Message m) {
        this();
        setMessage(m);
    }

    private void layout() {
        component.add(from,     "fromText");
        component.add(subject,  "subjectText");
        component.add(fragment, "fragmentText");
    }

    public Component getComponent() {
        return component;
    }
    
    public void setMessage(Message m) {
        if (m.getSubject() == null || "".equals(m.getSubject().trim())) {
            setComponentsVisible(new String[] { "subjectText" }, false);
        } else {
            setComponentsVisible(new String[] { "subjectText" }, true);
            subject.setText(m.getSubject());
        }
        if (m.getFragment() == null || "".equals(m.getFragment().trim())) {
            setComponentsVisible(new String[] { "fragmentText" }, false);
        } else {
            setComponentsVisible(new String[] { "fragmentText" }, true);
            fragment.setText("<html>" + m.getFragment());
            wrapLabel(fragment);
        }
        String name = m.getSenderName();
        if (name == null || "".equals(name.trim())) {
            name = format("fromFormat2", m.getSenderAddress());
        } else {
            name = format("fromFormat1", name, m.getSenderAddress());
        }
        from.setText(name);
    }
    
    private void setComponentsVisible(String[] names, boolean b) {
        HashMap<String,Component> cmap = new HashMap<String,Component>();
        Component[] comps = component.getComponents();
        for (Component c : comps)
            cmap.put(c.getName(), c);
        for (String name : names) {
            Component c = cmap.get(name);
            if (c != null)
                c.setVisible(b);
        }
        component.invalidate();
    }
    
    private void wrapLabel(JLabel l) {
        int width = getInt("preferredWidth");
        Font xx = l.getFont();  
        int fontHeight = l.getFontMetrics(xx).getHeight();  
        int stringWidth = l.getFontMetrics(xx).stringWidth(l.getText());
        int linesCount = (int) Math.floor(stringWidth / width);  
        linesCount = Math.max(1, linesCount + 2);  
        l.setPreferredSize(new Dimension(width,
                (fontHeight+2)*linesCount));   
    }
}

package com.zimbra.app.systray;

import java.awt.Color;
import java.awt.Insets;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.border.*;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.Timer;

public class SlidingDialog extends JDialog {
    private final static int ANIMATE_FRAME_TIME = 1000 / 60; // ~60fps

    private Prefs.ScreenLocation location = Prefs.ScreenLocation.BOTTOM_RIGHT;

    private final Rectangle r;
    private long start;

    // TODO turn this into a Pref
    private boolean _animate = true;

    private JComponent c;
    private Dimension size;
    private SlidingCanvas canvas;

    private Timer timer;

    public SlidingDialog(Frame parent) {
        super(parent);
        r = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
    }

    public void setComponent(JComponent c) {
        this.c = c;

        add(c);
        pack();
        size = getSize();
        getContentPane().removeAll();
        canvas = new SlidingCanvas(c);
        add(canvas);
        pack();
    }

    public void setScreenLocation(Prefs.ScreenLocation location) {
        this.location = location;
    }

    private class Animation implements ActionListener {
        private final boolean show;
        private final long animateTime;
        Animation(boolean show) {
            this.show = show;
            animateTime = size.height * 4;
        }
        Point finalLocation = calculateWindowLocation();
        int y = r.y + r.height;
        public void actionPerformed(ActionEvent e) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > animateTime) {
                if (show) {
                    getContentPane().removeAll();
                    getContentPane().add(c);
                    pack();
                    setLocation(calculateWindowLocation());
                    repaint();
                } else {
                    SlidingDialog.super.setVisible(false);
                }
                setFocusableWindowState(true);
                timer.stop();
                timer = null;
            } else {
                double done = (double) elapsed / animateTime;
                int _pos = (int) (size.height * done);
                int position = show ? _pos : size.height - _pos;
                position = position == 0 ? 1 : position;

                canvas.setPosition(position);
                int yPos = -1;
                switch (location) {
                case TOP_RIGHT:
                case TOP_LEFT:
                    yPos = r.y - (size.height - position);
                    break;
                case BOTTOM_RIGHT:
                case BOTTOM_LEFT:
                    yPos = y - position;
                    break;
                }
                setLocation(finalLocation.x, yPos);
                pack();
                repaint();
                if (!isVisible())
                    SlidingDialog.super.setVisible(true);
            }
        }
    }

    @Override
    public void setVisible(boolean b) {
        setFocusableWindowState(false);
        if (_animate && location != Prefs.ScreenLocation.CENTER) {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            start = System.currentTimeMillis();
            timer = new Timer(ANIMATE_FRAME_TIME, new Animation(b));
            timer.start();
        } else {
            if (b) {
                add(c);
                pack();
                setLocation(calculateWindowLocation());
            }
            super.setVisible(b);
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    setFocusableWindowState(true);
                }
            });
        }
    }

    private class SlidingCanvas extends JComponent {
        private BufferedImage image;
        private Dimension size = new Dimension();
        private Dimension originalSize;
    
        public SlidingCanvas(JComponent c) {
            size.width = c.getWidth();
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsConfiguration gc =
                    ge.getDefaultScreenDevice().getDefaultConfiguration();
            image = gc.createCompatibleImage(c.getWidth(), c.getHeight());

            originalSize = new Dimension(image.getWidth(), image.getHeight());

            Graphics g = image.getGraphics();
            g.setColor(c.getBackground());
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
            c.paint(g);
        }
    
        @Override
        public Dimension getPreferredSize() {
            return size;
        }
    
        @Override
        public Dimension getMinimumSize() {
            return size;
        }
    
        @Override
        public Dimension getSize() {
            return size;
        }
    
        public void setPosition(int position) {
            if (position <= image.getHeight())
                size.height = position;
            setSize(size);
        }
    
        @Override
        public void paint(Graphics g) {
            if (size.height == 0)
                return;
            int y = -1, height = -1;
            switch (location) {
            case TOP_RIGHT:
            case TOP_LEFT:
                y = originalSize.height - size.height;
                height = size.height;
                break;
            case BOTTOM_RIGHT:
            case BOTTOM_LEFT:
                y = 0; height = size.height;
                break;
            case CENTER:
                return;
            }
            BufferedImage sub = image.getSubimage(0, y,
                    c.getWidth(), height);
            g.drawImage(sub, 0, 0, this);
        }
    }

    private Point calculateWindowLocation() {
        Dimension size = getSize();
        int x = 0, y = 0;
        switch (location) {
        case TOP_LEFT:
            x = r.x;
            y = r.y;
            break;
        case TOP_RIGHT:
            x = r.width - size.width + r.x;
            y = r.y;
            break;
        case CENTER:
            x = r.x + (r.width  - getSize().width)  / 2;
            y = r.y + (r.height - getSize().height) / 2;
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

        return new Point(x, y);
    }

    public static void main(String[] args) throws Exception {
        ZimbraTray zt = new ZimbraTray();
        TrustManager tm = new TrustManager(zt);
        javax.swing.JLabel l = new javax.swing.JLabel(
                tm.getString("certificateWarning"));
        final SlidingDialog d = new SlidingDialog(zt.HIDDEN_PARENT);

        d.setUndecorated(true);
        EtchedBorder b1 = new EtchedBorder(EtchedBorder.RAISED);
        MatteBorder b2 = new MatteBorder(new Insets(3,3,3,3), Color.blue);
        ((JComponent) d.getContentPane()).setBorder(
                new CompoundBorder(b1, b2));


        for (Prefs.ScreenLocation loc : Prefs.ScreenLocation.values()) {
            d.setComponent(l);
            d.setScreenLocation(loc);
            d.setVisible(true);
            Thread.sleep(2000);
            d.setVisible(false);
            Thread.sleep(2000);

        }
        System.exit(0);
    }
}

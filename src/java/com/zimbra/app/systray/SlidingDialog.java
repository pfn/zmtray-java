package com.zimbra.app.systray;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.Timer;

public class SlidingDialog extends JDialog {
    private final static int ANIMATE_FRAME_TIME = 1000 / 60; // ~60fps

    private Prefs.ScreenLocation location = Prefs.ScreenLocation.BOTTOM_RIGHT;

    private final Rectangle r;
    private long start;

    private JComponent c;
    private Dimension size;
    private SlidingCanvas canvas;
    private boolean okToAnimate = false;

    private Timer timer;

    public SlidingDialog(Frame parent) {
        super(parent);
        r = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
    }

    public void setComponent(JComponent c) {
        if (!isVisible())
            _setComponent(c, true);
        else {
            this.c = c;
            getContentPane().removeAll();
            add(c);
            pack();
        }
    }
    
    public void _setComponent(JComponent c, boolean show) {
        this.c = c;
        
        int width = c.getWidth();
        int height = c.getHeight();
        
        okToAnimate = width != 0 && height != 0;

        add(c);
        pack();
        size = getSize();
        getContentPane().removeAll();
        canvas = new SlidingCanvas(c);
        add(canvas);
        if (!show)
            canvas.setPosition(c.getHeight());
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
            animateTime = size.height * 3;
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
                if (!show) {
                    // cancel animation, nothing to animate
                    if (c.getHeight() == 0) {
                        timer.stop();
                        timer = null;
                        SlidingDialog.super.setVisible(false);
                        return;
                    }
                }
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
                    yPos = r.y + y - position;
                    yPos = r.y + y - getSize().height;
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
    public void pack() {
        super.pack();
        if (timer == null) {
            setLocation(calculateWindowLocation());
        }
    }

    @Override
    public void setVisible(boolean b) {
        setFocusableWindowState(false);
        if (Prefs.getPrefs().getAnimateMessageAlerts() &&
                location != Prefs.ScreenLocation.CENTER && okToAnimate) {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            if (!b) {
                _setComponent(c, b);
            }
            start = System.currentTimeMillis();
            timer = new Timer(ANIMATE_FRAME_TIME, new Animation(b));
            timer.start();
        } else {
            if (b) {
                getContentPane().removeAll();
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
            int width = c.getWidth();
            int height = c.getHeight();
            if (!okToAnimate)
                return;
            
            size.width = width;
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsConfiguration gc =
                    ge.getDefaultScreenDevice().getDefaultConfiguration();
            image = gc.createCompatibleImage(width, height);

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
    
        public void update(Graphics g) {
            paint(g);
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
            x = r.width  - size.width  + r.x;
            y = r.height - size.height + r.y;
            break;
        case BOTTOM_LEFT:
            x = r.x;
            y = r.height - size.height + r.y;
            break;
        }

        return new Point(x, y);
    }
}

package edu.gemini.shared.gui;

import scala.Option$;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;

/**
 * GlassLabel is a message displayed on top of a frame while an ongoing
 * operation is progressing in the background.  The message blocks all input to
 * the widgets underneath, preventing user interaction until the operation
 * completes and the label is hidden.
 */
public final class GlassLabel {

    /**
     * Creates and displays the glass label over the root pane displaying the
     * given message and capturing input events until hidden.  Use the returned
     * GlassLabel instance to dismiss.
     *
     * @param comp component whose root component should be covered with the
     *             glass pane
     * @param message message to display
     *
     * @return a GlassLabel wrapped in a Scala Option; if None the root pane of
     * the provided component could not be found
     */
    public static scala.Option<GlassLabel> show(final Component comp, final String message) {
        final Optional<JRootPane> frame  = Optional.ofNullable(comp).flatMap(c -> Optional.ofNullable(SwingUtilities.getRootPane(c)));
        final Optional<GlassLabel> glass = frame.map(f -> new GlassLabel(f, message));
        return Option$.MODULE$.apply(glass.orElse(null));
    }

    private Optional<JRootPane> frame;

    private GlassLabel(final JRootPane frame, final String message) {
        frame.getGlassPane().setVisible(false);
        final GlassPanel glass = new GlassPanel(message);
        frame.setGlassPane(glass);
        glass.setVisible(true);
        glass.invalidate();
        glass.requestFocusInWindow();

        this.frame = Optional.of(frame);
    }

    /**
     * Dismisses the label, allowing the user to interact with the UI again.
     */
    public void hide() {
        frame.ifPresent(f -> f.getGlassPane().setVisible(false));
        frame = Optional.empty();
    }

    private static final class GlassPanel extends JPanel implements MouseListener, MouseMotionListener, FocusListener {
        GlassPanel(String message) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 64));
            add(new FlashLabel(message), BorderLayout.CENTER);

            addMouseListener(this);
            addMouseMotionListener(this);
            addFocusListener(this);
        }

        // REL-2255 Starting in Java 1.7, painting doesn't obey the alpha value set on the background color
        // overriding paintComponent we can achieve the same result
        @Override
        protected void paintComponent(Graphics g) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setColor(getBackground());
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g2.fillRect(0, 0, getWidth(), getHeight());
            // Paint children before disposing
            super.paintChildren(g);
            g2.dispose();
        }

        @Override
        public void focusGained(FocusEvent e) {
            // eat event
        }

        @Override
        public void focusLost(FocusEvent e) {
            // eat event
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // eat event
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // eat event
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // eat event
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // eat event
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // eat event
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // eat event
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // eat event
        }
    }

    @SuppressWarnings("serial")
    static final class FlashLabel extends JLabel {

        private static final int alpha = 196;
        private static final int width = 500;

        FlashLabel(String message) {
            super(" " + message + " "); // :-/
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            Font f = getFont();
            setFont(f.deriveFont(f.getSize2D() * 2));
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int x = getBounds().width / 2 - width / 2;
            int y = getBounds().height / 2 - 30;
            g2d.setColor(new Color(64, 64, 64, alpha));
            g2d.fillRoundRect(x, y, width, 60, 20, 20);
            setForeground(new Color(255, 255, 255, alpha));
            super.paint(g);
        }
    }

}

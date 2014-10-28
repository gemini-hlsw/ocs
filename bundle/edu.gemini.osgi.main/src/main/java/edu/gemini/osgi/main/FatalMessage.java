package edu.gemini.osgi.main;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides support for displaying an error message (possibly containing simple
 * html) and quiting.
 */
final class FatalMessage {
    private FatalMessage() {}

    static /*Nothing*/ void show(final String title, final String message, final Logger log) {
        System.err.println(stripTags(message));
        if (!GraphicsEnvironment.isHeadless()) showPopup(title, message, log);
        System.exit(-1);
    }

    private static String stripTags(String message) {
        return message.replaceAll("<[^>]*>", "");
    }

    private static final class Link implements HyperlinkListener {
        private final Logger log;

        Link(Logger log) { this.log = log; }

        @Override
        public void hyperlinkUpdate(final HyperlinkEvent e) {
            if (HyperlinkEvent.EventType.ACTIVATED.toString().equals(e.getEventType().toString())) {
                try {
                    Desktop.getDesktop().browse(new URI(e.getDescription()));
                } catch (URISyntaxException ex) {
                    log.severe("Cannot parse " + e.getDescription() + " as a URI");
                } catch (IOException ex) {
                    log.log(Level.SEVERE, "Could not open " + e.getDescription(), ex);
                }
            }
        }
    }

    // An awful popup so that users who aren't launching the app from the
    // command line at least know what is going on and what to do.
    private static void showPopup(final String title, final String message, final Logger log) {
        final JPanel pan = new JPanel(new BorderLayout(0, 5)) {{
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }};

        final String wrapper = "<html><style type=\"text/css\">body { font:12pt dialog,sans-serif; }</style><body>%s</body></html>";
        final JEditorPane ed = new JEditorPane("text/html", String.format(wrapper, message)) {{
            setBackground(pan.getBackground());
            setHighlighter(null);
            setEditable(false);
            addHyperlinkListener(new Link(log));
        }};

        final JButton ok = new JButton("Ok");

        pan.add(ed, BorderLayout.CENTER);
        pan.add(new JPanel() {{ add(ok); }}, BorderLayout.SOUTH);

        final Dimension size = new Dimension(500, 200);
        final JDialog dialog = new JDialog((Frame)null, title, true) {{
            setMinimumSize(size);
            setPreferredSize(size);
            setContentPane(pan);
            ok.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        }};
        dialog.pack();

        final Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        dialog.setLocation(new Point(p.x - dialog.getWidth() / 2, p.y - dialog.getHeight() / 2));

        dialog.setVisible(true);
    }
}

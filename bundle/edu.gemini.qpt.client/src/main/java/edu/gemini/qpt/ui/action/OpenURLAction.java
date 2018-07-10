package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import edu.gemini.qpt.ui.util.Platform;

@SuppressWarnings("serial")
public class OpenURLAction extends AbstractAction {

    private static final Logger LOGGER = Logger.getLogger(OpenURLAction.class.getName());
    
    private final String url;
    
    public OpenURLAction(String url, String title) {
        super(title);
        this.url = url;
    }

    public OpenURLAction(String url, String title, KeyStroke ks) {
        this(url, title);
        putValue(ACCELERATOR_KEY, ks);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            Platform.displayURL(url);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Trouble opening " + url, ioe);
        }
    }

}

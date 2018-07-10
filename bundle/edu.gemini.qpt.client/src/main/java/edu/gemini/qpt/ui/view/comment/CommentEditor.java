package edu.gemini.qpt.ui.view.comment;

import java.util.logging.Logger;

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.gemini.qpt.core.util.Commentable;

@SuppressWarnings("serial")
public class CommentEditor extends JTextArea implements DocumentListener {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(CommentEditor.class.getName());
    private Commentable target;
    private boolean pushing;
    
    public CommentEditor() {
        setLineWrap(true);
        setWrapStyleWord(true);
        setEnabled(false);
        getDocument().addDocumentListener(this);
        setEnabled(false);
    }
        
    public void changedUpdate(DocumentEvent e) {
        pullComment();
    }

    public void removeUpdate(DocumentEvent e) {
        pullComment();
    }

    public void insertUpdate(DocumentEvent e) {
        pullComment();
    }

    private void pullComment() {
        if (target != null && !pushing)     {
            // RCN: probably dont need to do this check
            String prev = target.getComment();
            String next = getText();
            if (!next.equals(prev)) {
//                LOGGER.info("TEXT: \"" + prev + "\" => \"" + next);
                target.setComment(getText());
            }
        }
    }

    public Commentable getTarget() {
        return target;
    }
    
    public synchronized void setTarget(Commentable target) {
        pushing = true;
        try {
            this.target = target;
            if (target != null) {
                setText(target.getComment());
                setEnabled(true);
            } else {
                setText("");
                setEnabled(false);
            }
        } finally {
            pushing = false;
        }
    }
    
}

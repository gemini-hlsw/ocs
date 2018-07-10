package edu.gemini.qpt.ui.view.comment;

import static edu.gemini.qpt.ui.util.SharedIcons.NOTE;
import static edu.gemini.qpt.ui.util.SharedIcons.NOTE_DIS;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.util.Commentable;
import edu.gemini.qpt.ui.util.SimpleToolbar;
import edu.gemini.qpt.ui.util.SimpleToolbar.StaticText;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

public class CommentViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    // Disabled text
    private static final String DISABLED_TEXT = "Select a plan variant or a visit to edit its comment here.";
    
    // The viewer
    private final CommentViewer viewer = new CommentViewer();
    
    // And scroll pane
    private final JScrollPane scroll = Factory.createStrippedScrollPane(viewer.getControl());

    // Toolbar
    private SimpleToolbar toolbar = new SimpleToolbar();
    private StaticText text = new SimpleToolbar.StaticText(DISABLED_TEXT);
    private JButton top = new SimpleToolbar.IconButton(NOTE, NOTE_DIS);
    
    // Content
    private JPanel content = new JPanel(new BorderLayout());
    
    // Current schedule
    private Schedule schedule;
    
    public CommentViewAdvisor() {
        
        // Top button
        top.setText("Top");
        top.setEnabled(false);
        top.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GSelection<Commentable> sel = schedule != null ?
                    new GSelection<>(schedule) :
                    GSelection.<Commentable>emptySelection();
                viewer.setSelection(sel);
            }
        });
        
        // Toolbar needs a non-default layout
        // TODO: refactor, create factory methods for SimpleToolbar
        toolbar.setLayout(new BorderLayout());
        toolbar.setBorder(BorderFactory.createCompoundBorder(toolbar.getBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        toolbar.add(text, BorderLayout.WEST);
        toolbar.add(top, BorderLayout.EAST);
        
        // Set up the content
        content.add(scroll, BorderLayout.CENTER);
        content.add(toolbar, BorderLayout.SOUTH);
        
        // Listen for target changes
        viewer.addPropertyChangeListener(CommentViewer.PROP_TARGET, this);
        
    }
    
    public void close(IViewContext context) {
    }

    public void open(final IViewContext context) {
        
        // Set up the context
        context.setTitle("Comment");
        context.setContent(content);
        context.setSelectionBroker(viewer);
        context.getShell().addPropertyChangeListener(IShell.PROP_MODEL, this);
        
    }

    public void setFocus() {
        viewer.getControl().requestFocus();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        
        if (CommentViewer.PROP_TARGET.equals(evt.getPropertyName())) {        
            Commentable c = (Commentable) evt.getNewValue();
            if (c != null) {
                text.setText("Comment for " + c);
            } else {
                text.setText(DISABLED_TEXT);
            }            
        }
        
        if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
            schedule = (Schedule) evt.getNewValue();
            top.setEnabled(schedule != null);
        }
        
    }
    
}

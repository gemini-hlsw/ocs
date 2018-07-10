package edu.gemini.qpt.ui.view.property;

import static edu.gemini.qpt.ui.view.property.PropertyAttribute.Name;
import static edu.gemini.qpt.ui.view.property.PropertyAttribute.Value;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.Note;
import edu.gemini.qpt.ui.util.ScrollPanes;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

@SuppressWarnings("serial")
public class PropertyViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(PropertyViewAdvisor.class.getName());
    
    // Name of the cards in our card layout
    private static final String CARD_TEXT = "text";
    private static final String CARD_TABLE = "table";
    
    // The table viewer and its controller.
    private final PropertyController propertyController = new PropertyController();
    private final GTableViewer<GSelection<?>, Map.Entry<String, Object>, PropertyAttribute> tableViewer =
        new GTableViewer<GSelection<?>, Map.Entry<String, Object>, PropertyAttribute>(propertyController);
    
    // The text area.
    private final JTextArea textArea = new JTextArea();
    
    // Scroll bars for the table and text
    private final JScrollPane tableScroll = Factory.createStrippedScrollPane(tableViewer.getTable());
    private final JScrollPane textScroll =Factory.createStrippedScrollPane(textArea);

    // The content panel and its layout. This is the main control. 
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    
    public PropertyViewAdvisor() {

        // Set up the content panel
        content.add(tableScroll, CARD_TABLE);
        content.add(textScroll, CARD_TEXT);
        
        // Set up the text area
        textArea.setBackground(Color.WHITE);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        
        // Set up the viewer
        tableViewer.setColumns(Name, Value);
        tableViewer.setColumnSize(Name, 120);
        tableViewer.setDecorator(new PropertyDecorator());

        // Set up the scroll pane
        ScrollPanes.setViewportHeight(tableScroll, 8);

    }
    
    public void close(IViewContext context) {
    }

    public void open(IViewContext context) {
        context.setTitle("Properties");
        context.setContent(content);
        context.getShell().addPropertyChangeListener(IShell.PROP_SELECTION, this);    
        context.getShell().addPropertyChangeListener(IShell.PROP_MODEL, this);    
        context.getShell().addPropertyChangeListener(IShell.PROP_MODEL, propertyController);
    }

    public void setFocus() {
        content.requestFocusInWindow();
    }

    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent evt) {
        
        if (evt.getPropertyName().equals(IShell.PROP_SELECTION)) {
            GSelection<?> selection = (GSelection) evt.getNewValue();
            tableViewer.setModel(selection);
//            LOGGER.info("Selection: " + selection);
            if (selection.size() == 1 && selection.first() instanceof Note) {
                Note note = (Note) selection.first();
                textArea.setText(note.getTitle() + "\n\n" + note.getText()); // pthbbbt
                textArea.select(0, 0);
                cardLayout.show(content, CARD_TEXT);
            } else {
                textArea.setText(null);
                cardLayout.show(content, CARD_TABLE);
            }
        } else {            
            Schedule sched = (Schedule) evt.getNewValue();
            if (sched == null) {
                textArea.setText(null);
                cardLayout.show(content, CARD_TABLE);
            }
        }
        
    }

}

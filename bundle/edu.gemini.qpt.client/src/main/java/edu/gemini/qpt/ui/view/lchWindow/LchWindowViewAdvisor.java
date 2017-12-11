package edu.gemini.qpt.ui.view.lchWindow;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.ScrollPanes;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static edu.gemini.qpt.ui.view.lchWindow.LchWindowAttribute.Start;
import static edu.gemini.qpt.ui.view.lchWindow.LchWindowAttribute.End;
import static edu.gemini.qpt.ui.view.lchWindow.LchWindowAttribute.Length;
import static edu.gemini.qpt.ui.view.lchWindow.LchWindowAttribute.Type;
import static edu.gemini.qpt.ui.view.lchWindow.LchWindowAttribute.Name;

import static edu.gemini.qpt.ui.view.lchWindow.LchWindowType.clearance;

public class LchWindowViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    private final LchWindowType windowType;
    private final LchWindowController controller;

	// The table viewer
    private final GTableViewer<Schedule, LchWindow, LchWindowAttribute> tableViewer;

	// The scroll bar
    private final JScrollPane scroll;

	public LchWindowViewAdvisor(LchWindowType windowType) {
        this.windowType = windowType;
        controller = new LchWindowController(windowType);
        tableViewer = new GTableViewer<>(controller);
        scroll = Factory.createStrippedScrollPane(tableViewer.getTable());

//		// Set up the viewer
		tableViewer.setColumns(Start, End, Length, Type, Name);
		tableViewer.setColumnSize(Start, 80);
		tableViewer.setColumnSize(End, 80);
        tableViewer.setColumnSize(Length, 80);
        // LCH-191: Let name and type fill rest of table, since they may contain multiple values

		tableViewer.setDecorator(new LchWindowDecorator());
		tableViewer.setFilter(new LchWindowFilter());
        tableViewer.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		// And the scroll pane
		ScrollPanes.setViewportHeight(scroll, 5);

	}

	public void close(IViewContext context) {
	}

	public void open(IViewContext context) {
		context.setTitle(windowType == clearance ? "Clearance" : "Shutter");
		context.setContent(scroll);
		context.getShell().addPropertyChangeListener(IShell.PROP_MODEL, this);
        context.getShell().addPropertyChangeListener(IShell.PROP_SELECTION, this);
	}

	public void setFocus() {
		tableViewer.getControl().requestFocus();
	}

	public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(IShell.PROP_SELECTION)) {
            GSelection<?> selection = (GSelection) evt.getNewValue();
            if (selection.size() == 1 && selection.first() instanceof Alloc) {
                Alloc alloc = (Alloc)selection.first();
                controller.setAlloc(alloc);
                controller.propertyChange(evt);
            } else {
                controller.setAlloc(null);
                controller.propertyChange(evt);
            }
            tableViewer.refresh();
        } else if (evt.getPropertyName().equals(IShell.PROP_MODEL)) {
            tableViewer.setModel((Schedule) evt.getNewValue());
            controller.propertyChange(evt);
        }
    }
}

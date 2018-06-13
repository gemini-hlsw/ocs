package jsky.app.ot.gemini.editor.targetComponent.tableSelection;

import edu.gemini.spModel.target.SPCoordinates;

final public class TableCoordinateSelection implements TableSelection {
    private final SPCoordinates c;

    public TableCoordinateSelection(final SPCoordinates c) {
        this.c = c;
    }

    public SPCoordinates getCoordinates() {
        return c;
    }
}

package edu.gemini.shared.gui.calendar;

/**
 * This class supports cell arithmetic on calendar table cells.
 * For example, it provides the number of cells between two cells,
 */
public class CalendarRowColumn extends RowColumn {

    private static final int NUM_COLUMNS = 7;

    public CalendarRowColumn(int row, int column) {
        super(NUM_COLUMNS, row, column);
    }

    public CalendarRowColumn(RowColumn r) {
        super(NUM_COLUMNS, r.row, r.column);
    }

}


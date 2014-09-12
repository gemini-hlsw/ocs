package edu.gemini.shared.gui.calendar;

/**
 * This class supports cell arithmetic on table cells.
 * For example, it provides the number of cells between two cells,
 * Since this class is intended to make life
 * easy, the client does not want to constantly specify the number
 * of columns in a row.  So the client should use a subclass that
 * fixes the number of columns.
 * Motivation was to support a calendar displaying days in a
 * grid with 7 columns.  For this reason the column number is 1-based
 * since days are 1-based in Java Calendars.
 */
public class RowColumn {

    /** This constant indicates an operation on the row. */
    public static final int ROW = 0;

    /** This constant indicates an operation on the column. */
    public static final int COLUMN = 1;

    /** Smallest allowed column number. */
    public static final int MIN_COLUMN = 1;  // columns are 1-based

    private int _numColumns;

    /** The row number. */
    public int row;     // public data a la java.awt.Dimension

    /** The column number. */
    public int column;

    /** Constructs a RowColumn */
    public RowColumn(int numColumns, int row, int column) {
        this._numColumns = numColumns;
        this.row = row;
        this.column = column;
    }

    /** Constructs a RowColumn */
    public RowColumn(RowColumn r) {
        _numColumns = r._numColumns;
        row = r.row;
        column = r.column;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getColumnCount() {
        return _numColumns;
    }

    /**
     * Adds the specified number of rows or columns to this cell.
     * @param field - RowColumn.ROW or RowColumn.COLUMN
     * @param amount - Number of rows or columns to roll.  Can be negative.
     */
    public void add(int field, int amount) {
        if (field == ROW) {
            row += amount;
        } else if (field == COLUMN) {
            if (amount >= 0) {
                while (column + amount > getColumnCount()) {
                    amount -= (getColumnCount() + 1 - column);
                    column = MIN_COLUMN;
                    row++;
                }
                column += amount;
            } else {
                amount = -amount;
                while (column - amount < MIN_COLUMN) {
                    amount -= column;
                    column = getColumnCount();
                    row--;
                }
                column -= amount;
            }
        }
    }

    /** Rolls the column to MIN_COLUMN. */
    public void rollToBeginningOfRow() {
        column = MIN_COLUMN;
    }

    /** Rolls the column to the maximum column, getColumnCount(). */
    public void rollToEndOfRow() {
        column = getColumnCount();
    }

    /**
     * This returns the number of cells between two cells, this cell and
     * the argument cell.  If the two cells don't have the same number of
     * columns, 0 is returned.  I want this class to be as easy to use
     * as possible, so I don't throw any exceptions here.  The burden is
     * on the client to use RowColumns with the same number of columns.
     */
    public int difference(RowColumn arg) {
        if (getColumnCount() != arg.getColumnCount())
            return 0;
        boolean reversed = arg.greaterThan(this);
        RowColumn a, b;
        if (reversed) {
            b = arg;
            a = this;
        } else {
            a = arg;
            b = this;
        }
        // now a < b
        int difference = (b.getRow() - a.getRow()) * getColumnCount();
        difference += b.getColumn() - a.getColumn();
        if (reversed)
            difference = -difference;
        return difference;
    }

    /**
     * Returns whether this cell is less than (before) the argument.
     */
    public boolean lessThan(RowColumn arg) {
        if (getColumnCount() != arg.getColumnCount())
            return false;
        if (row < arg.row)
            return true;
        if (row > arg.row)
            return false;
        if (column < arg.column)
            return true;
        if (column > arg.column)
            return false;
        return false;  // they are equal
    }

    /**
     * Returns whether this cell is greater than (after) the argument.
     */
    public boolean greaterThan(RowColumn arg) {
        if (getColumnCount() != arg.getColumnCount())
            return false;
        if (row > arg.row)
            return true;
        if (row < arg.row)
            return false;
        if (column > arg.column)
            return true;
        if (column < arg.column)
            return false;
        return false;  // they are equal
    }

    /**
     * Returns whether this cell is equal to the argument.
     */
    public boolean equals(RowColumn arg) {
        if (getColumnCount() != arg.getColumnCount())
            return false;
        return (row == arg.row && column == arg.column);
    }

    public String toString() {
        return "row: " + row + " column: " + column;
    }

}


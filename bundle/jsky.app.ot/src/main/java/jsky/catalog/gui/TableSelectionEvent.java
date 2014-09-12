/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TableSelectionEvent.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.catalog.gui;

import java.util.EventObject;

import jsky.graphics.CanvasFigure;
import jsky.catalog.TableQueryResult;


/**
 * This event is generated when a catalog table is seletced or deselected.
 *
 * @version $Revision: 4414 $
 * @author Daniella Malin
 */
public class TableSelectionEvent extends EventObject {

    /** The catalog table data row corresponding to the table. */
    protected int row;

    /** The table containing the data */
    TableQueryResult table;

    /**
     * Create a TableSelectionEvent for the given row and table data.
     *
     * @param row the catalog table data row corresponding to the table
     * @param table the table containing the data
     */
    public TableSelectionEvent(int row, TableQueryResult table) {
        super(table);
        this.row = row;
        this.table = table;
    }


    /** Return the catalog table data row corresponding to the table. */
    public int getRow() {
        return row;
    }

    /** Return the table containing the data. */
    public TableQueryResult getTable() {
        return table;
    }
}

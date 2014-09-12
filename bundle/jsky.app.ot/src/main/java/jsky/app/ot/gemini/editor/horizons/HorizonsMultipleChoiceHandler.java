package jsky.app.ot.gemini.editor.horizons;

import edu.gemini.horizons.api.ResultsTable;
import edu.gemini.shared.gui.UIUtil;
import jsky.app.ot.ui.util.TableSorter;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

//$Id: HorizonsMultipleChoiceHandler.java 7461 2006-11-28 15:41:20Z anunez $

/**
 * A handler for the Multi-Answer GUI.
 * @see jsky.app.ot.gemini.editor.horizons.HorizonsMultipleChoiceDialog
 */
public class HorizonsMultipleChoiceHandler implements ActionListener {


    private HorizonsMultipleChoiceDialog _panel;
    private String _objectId;
    private JDialog _dialog;
    private AnswersTableModel _atm;

    public HorizonsMultipleChoiceHandler() {

        _dialog = new JDialog();

        _panel = new HorizonsMultipleChoiceDialog();
        _panel.getOkButton().addActionListener(this);
        _panel.getCancelButton().addActionListener(this);

        _initTable();

        _dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        _dialog.setPreferredSize(new Dimension(590, 300));
        _dialog.setMinimumSize(new Dimension(590, 300));
        _dialog.add(_panel);


    }

    /**
     * Initialize the <code>JTable</code> object. Enables sorting,
     * single-selection mode.
     */
    private void _initTable() {
        JTable table = _panel.getResultsTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setReorderingAllowed(false);

        _atm = new AnswersTableModel();
        TableSorter tableSorter = new TableSorter(_atm);
        table.setModel(tableSorter);
        tableSorter.setTableHeader(table.getTableHeader());
    }

    /**
     * Shows  a modal dialog with a table containing the multiple
     * answer reply from Horizons. The user will be able to
     * select one of those options to narrow the query.
     */
    public void show() {
        _dialog.pack();
        _dialog.setModal(true);

        //center the dialog in the screen
        Dimension d = _dialog.getSize();
        int w = d.width;
        int h = d.height;
        Point p = UIUtil.getUpperLeftCenteringCoordinate(w, h);
        _dialog.setLocation(p);
        _dialog.setVisible(true);

    }


    /**
     * Updates the table with the <code>results</code>
     * @param results the information about the
     * multiple answer gotten from Horizons.
     */
    public void updateTable(ResultsTable results) {
        _atm.setResults(results);
    }


    /**
     * A super simple table model for the results table.
     * Disables edition of cells, and allows updates of
     * <code>ResultsTable</code>
     */
    private class AnswersTableModel extends DefaultTableModel {

        /**
         * Disables the edtion for all the cells.
         */
        public boolean isCellEditable(int i, int i1) {
            return false;
        }

        /**
         * Set the new <code>ResultsTable</code> object
         * to be displayed on this dialog
         * @param results The new <code>ResultTable</code> that will
         * be shown.
         */
        public void setResults(ResultsTable results) {
            setDataVector(results.getResults(), results.getHeader());
            fireTableStructureChanged();
        }


    }

    /**
     * Action events for the OK and Cancel buttons
     */
    public void actionPerformed(ActionEvent actionEvent) {

        Object o = actionEvent.getSource();
        if (o == _panel.getOkButton()) {
            //get the selected item

            JTable table = _panel.getResultsTable();
            int row = table.getSelectedRow();
            if (row == -1) {
                DialogUtil.message("Must select a row in the table");
                return;
            }
            _objectId = (String) table.getModel().getValueAt(row, 0);
            _dialog.dispose();

        } else if (o == _panel.getCancelButton()) {
            _objectId = null;
            _dialog.dispose();
        }
    }


    /**
     * Retrieves the object Id selected by the user.
     * @return String with the object Id selected by the user, or <code>null</code> if
     * nothing was selected of the dialog was closed.
     */
    public String getObjectId() {
        return _objectId;
    }





    public static void main(String[] args) {
        HorizonsMultipleChoiceHandler handler = new HorizonsMultipleChoiceHandler();

        ResultsTable results = new ResultsTable();
        Vector<String> data = new Vector<String>();
        data.add("900023");
        data.add("Halley");
        data.add("test 1");


        results.addResult(data);

        data = new Vector<String>();
        data.add("900020");
        data.add("Otoas");
        data.add("test 2");

        results.addResult(data);

        data = new Vector<String>();
        data.add("900025");
        data.add("Fresh");
        data.add("test 3");

        results.addResult(data);



        Vector<String> header = new Vector<String>();
        header.add("ID");
        header.add("Name");
        header.add("Extra Information");

        results.setHeader(header);

        handler.updateTable(results);


        handler.show();

        System.out.println("The dialog was closed. Selected " + handler.getObjectId());


    }
}

package jsky.app.ot.viewer;

import javax.swing.*;
import java.awt.*;

/**
 * The panel that's used to show the 
 */
public class SPProblemsPanel extends JPanel {

    private JTable _table;

    public SPProblemsPanel() {
        setLayout(new BorderLayout());
        _table = new JTable();
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(_table), BorderLayout.CENTER);
    }

    public JTable getTable() {
        return _table;
    }
}

package jsky.app.ot.editor.seq;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.io.SequenceOutputService;
import jsky.app.ot.viewer.OpenUtils;
import jsky.util.Preferences;
import jsky.util.gui.ExampleFileFilter;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jsky.app.ot.editor.seq.Keys.DATALABEL_KEY;

/**
 *
 */
final class SequenceTabUtil {
    // file chooser dialog
    private static JFileChooser _fileChooser;

    // Preferences key used to remember the user's selected dirs for XML export of sequence
    public static final String EXPORT_DIR_KEY = EdIteratorFolder.class.getName() + ".exportDir";

    /**
     * Export the sequence as an XML file.
     * Download <a href="ftp://ftp.gemini.edu/pub/staff/gillies/sequence.tar">sequence.tar</a>
     * for details on the XML format.
     */
    static void exportAsXML(ISPObservation obs, Component w) throws IOException {
        if (obs == null) {
            JOptionPane.showMessageDialog(w, "The sequence cannot be calculated until it is placed in an observation.", "Empty Sequence", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Generate a default filename from the program reference and obs id
        String obsID = obs.getObservationIDAsString("unknown");
        File defaultFile = new File(obsID + ".xml");

        JFileChooser fc = _getFileChooser();
        fc.setSelectedFile(defaultFile);
        fc.setMultiSelectionEnabled(false);
        fc.setAccessory(null);

        int option = fc.showSaveDialog(w);
        if (option != JFileChooser.APPROVE_OPTION)
            return;

        File xmlFile = fc.getSelectedFile();
        if (xmlFile == null)
            return;  // the user canceled

        // remember the selected dir for the next session
        Preferences.set(EXPORT_DIR_KEY, fc.getCurrentDirectory().toString());

        if (!OpenUtils.checkOverwrite(xmlFile)) return;

        FileWriter fw = null;
        try {
            fw = new FileWriter(xmlFile);
            SequenceOutputService.printSequence(fw, obs, true);
        } finally {
            if (fw != null) fw.close();
        }
    }

    /** Return the file chooser dialog used by this class. */
    private static JFileChooser _getFileChooser() {
        if (_fileChooser == null) {
            _fileChooser = _makeFileChooser();

            // Restore any previously selected dir
            String dir = Preferences.get(EXPORT_DIR_KEY);
            if (dir != null) _fileChooser.setCurrentDirectory(new File(dir));
        }
        return _fileChooser;
    }

    // Create and return a new file chooser to be used to select a
    // science program to display.
    private static JFileChooser _makeFileChooser() {
        JFileChooser fc = new JFileChooser(new File("."));
        ExampleFileFilter spFilter = new ExampleFileFilter(new String[]{"xml"}, "Science Program Sequence File");
        fc.addChoosableFileFilter(spFilter);
        fc.setFileFilter(spFilter);
        return fc;
    }

    static void resizeTableColumns(JTable table, TableModel model) {
        final TableColumnModel colModel = table.getColumnModel();
        final FontMetrics fm = table.getFontMetrics(table.getFont());
        final int rows = model.getRowCount();

        for (int col=0; col<model.getColumnCount(); ++col) {
            final String title = model.getColumnName(col);

            // Start with the width of the column header
            int size = fm.stringWidth(title);

            // Check the width of each item in the column to get the maximum width
            for (int row=0; row<rows; ++row) {
                final TableCellRenderer renderer = table.getCellRenderer(row, col);
                final Component component = table.prepareRenderer(renderer, row, col);
                final int tmp = component.getPreferredSize().width;
                if (tmp > size) size = tmp;
            }

            size += 20; // add a bit of padding

            // Resize the column
            final TableColumn tc = colModel.getColumn(col);
            _setColumnWidth(tc, size, size, size);
        }
    }


    private static void _setColumnWidth(TableColumn col, int prefWidth, int minWidth,
                                  int maxWidth) {

        col.setPreferredWidth(prefWidth);
        col.setMinWidth(minWidth);
        col.setMaxWidth(maxWidth);
    }

    private static final Pattern PAT = Pattern.compile(".*-(\\d+-\\d+-\\d+)$");
    public static String shortDatasetLabel(Config c) {
        String  lab = c.getItemValue(DATALABEL_KEY).toString();
        Matcher mat = PAT.matcher(lab);
        return (mat.matches()) ? mat.group(1) : lab;
    }
}

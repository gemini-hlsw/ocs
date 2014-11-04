package jsky.app.ot.gemini.gmos;

import edu.gemini.spModel.core.Platform;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.TableWidget;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import static edu.gemini.spModel.gemini.gmos.GmosCommonType.*;

/**
 * Class GmosCustomROITableWidget
 */
public class GmosCustomROITableWidget extends TableWidget {
    private CustomROIList rois;
    private Binning xBinning = Binning.DEFAULT;
    private Binning yBinning = Binning.DEFAULT;

    public GmosCustomROITableWidget() {
        this.rois = CustomROIList.create();
    }

    public ROIDescription getSelectedROI() {
        int i = getSelectedRow();
        if (i >= 0 && i < rois.size()) {
            return rois.get(i);
        } else {
            return null;
        }
    }

    public void reinit(final CustomROIList customROIs, final Binning xBinning, final Binning yBinning) {
        rois = customROIs;
        this.xBinning = xBinning;
        this.yBinning = yBinning;
        _updateTable();
    }

    public void removeAllROIs() {
        rois = CustomROIList.create();
        _updateTable();
    }

    public void removeSelectedROI() {
        int sel = getSelectedRow();
        if (sel >= 0 && sel < rois.size()) {
            rois = rois.remove(sel);
            _updateTable();
            _selectLastRow();
        }
    }

    public void updateSelectedROI(final ROIDescription roi) {
        rois = rois.update(getSelectedRow(), roi);
        _updateTable();
    }

    public void addROI(final ROIDescription roi) {
        rois = rois.add(roi);
        _updateTable();
        _selectLastRow();
    }

    private void _updateTable() {
        if (rois != null && !rois.isEmpty()) {
            Vector[] dataV = new Vector[rois.size()];
            for (int i = 0; i < rois.size(); i++) {
                dataV[i] = _ROIToVector(rois.get(i));
            }
            setRows(dataV);
        } else {
            clear();
        }
    }

    private void _selectLastRow() {
        if (getRowCount() > 0) {
            selectRowAt(getRowCount() - 1);
        }
    }

    private Vector<Integer> _ROIToVector(ROIDescription roi) {
        Vector<Integer> v = new Vector<Integer>(4);
        v.addElement(roi.getXStart());
        v.addElement(roi.getYStart());
        v.addElement(roi.getXSize(xBinning));
        v.addElement(roi.getYSize(yBinning));
        return v;
    }

    public CustomROIList getCustomROIs() {
        return rois;
    }

    /**
     * REL-1056: paste ROI data from the system clipboard into the table.
     * Expected format: 4 or 5 lines containing 4 numbers separated by spaces. Example:
     * <pre>
     *  3714 582 250 250
     *  4432 1843 250 250
     *  4146 2299 250 250
     *  2740 2604 250 250
     * </pre>
     *
     * @param ccd the detector of the current configuration
     * @return true if the table was modified
     */
    public boolean paste(final DetectorManufacturer ccd) {

        // do we have something to paste?
        String s = getClipboardContents();
        if (s.length() == 0) {
            DialogUtil.error(pasteError(s, ccd.getMaxROIs()));
            return false;
        }

        // do we have less than the maximum number of ROIs to insert?
        String[] lines = s.trim().split("\n");
        if (lines.length > ccd.getMaxROIs()) {
            DialogUtil.error("You cannot declare more than 5 custom ROIs for HAMAMATSU CCDs or 4 for E2V CCDs\n"
                    + pasteError(s, ccd.getMaxROIs()));
            return false;
        }

        // can we parse the ROIs?
        int[][] ar = new int[lines.length][4];
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] values = line.trim().split("\\s+");
            if (values.length != 4) {
                DialogUtil.error("Invalid line: '" + line + "': Expected 4 integers separated by spaces\n"
                        + pasteError(s, ccd.getMaxROIs()));
                return false;
            }
            for (int j = 0; j < values.length; j++) {
                String value = values[j];
                try {
                    ar[i][j] = (Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    DialogUtil.error("Invalid value: '" + value + "': Expected an integer\n"
                            + pasteError(s, ccd.getMaxROIs()));
                    return false;
                }
            }
        }

        // are the ROIs valid?
        // if so, insert them otherwise reject them
        final Collection<ROIDescription> rois = createROIs(ar);
        final Collection<ROIDescription> invalidROIs = findInvalidROIs(rois, ccd);
        if (invalidROIs.isEmpty()) {
            removeAllROIs();
            for (ROIDescription r : rois) {
                addROI(r);
            }
            return true;
        } else {
            final StringBuffer error = new StringBuffer("The following ROIs are invalid for this configuration:\n");
            for (ROIDescription r : invalidROIs) {
                error.append("    ");
                error.append(r.toString());
                error.append("\n");
            }
            DialogUtil.error(error.toString() + pasteError(s, ccd.getMaxROIs()));
            return false;
        }
    }

    private Collection<ROIDescription> createROIs(final int[][] ar) {
        final ArrayList<ROIDescription> rois = new ArrayList<>();
        for (int[] a : ar) {
            rois.add(new ROIDescription(a[0], a[1], a[2], a[3]));
        }
        return rois;
    }

    private Collection<ROIDescription> findInvalidROIs(final Collection<ROIDescription> rois, final DetectorManufacturer ccd) {
        final ArrayList<ROIDescription> invalidROIs = new ArrayList<>();
        for (ROIDescription r : rois) {
            if (!r.validate(ccd.getXsize(), ccd.getYsize())) {
                invalidROIs.add(r);
            }
        }
        return invalidROIs;
    }

    private String pasteError(String clipboard, int maxRows) {
        String s = (Platform.get() == Platform.osx) ? "Cmd-C" : "Ctrl-C";
        return "Error parsing Custom ROIs from:\n"
                + clipboard
                + "\nCopy (" + s + ") up to "
                + maxRows
                + " custom ROI parameters as 4 space-separated integers per row and then click the Paste button.";
    }

    /**
     * Get the String residing on the clipboard.
     *
     * @return any text found on the Clipboard; if none found, return an
     * empty String.
     */
    private String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
}


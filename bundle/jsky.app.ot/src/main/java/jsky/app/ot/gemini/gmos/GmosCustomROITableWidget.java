package jsky.app.ot.gemini.gmos;

import edu.gemini.spModel.core.Platform;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.TableWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Vector;

/**
 * Class GmosCustomROITableWidget
 *
 * @author Nicolas A. Barriga
 *         Date: 4/11/12
 */
public class GmosCustomROITableWidget extends TableWidget {
    private GmosCommonType.CustomROIList rois;
    private GmosCommonType.Binning xBinning = GmosCommonType.Binning.DEFAULT;
    private GmosCommonType.Binning yBinning = GmosCommonType.Binning.DEFAULT;
    private GmosCommonType.DetectorManufacturer det = null;

    public GmosCustomROITableWidget() {
        this.rois = GmosCommonType.CustomROIList.create();
    }

    public GmosCommonType.ROIDescription getSelectedROI() {
        int i = getSelectedRow();
        if (i >= 0 && i < rois.size()) {
            return rois.get(i);
        } else {
            return null;
        }
    }

    public void reinit(GmosCommonType.CustomROIList customROIs, GmosCommonType.Binning xBinning, GmosCommonType.Binning yBinning, GmosCommonType.DetectorManufacturer det) {
        rois = customROIs;
        this.xBinning = xBinning;
        this.yBinning = yBinning;
        this.det = det;
        _updateTable();
        for(GmosCommonType.ROIDescription roi:rois.get()){
            if (det != null && !roi.validate(det.getXsize(), det.getYsize())) {
                throw new IllegalArgumentException("ROI ["+roi+"] is not within valid ranges");
            }
        }
    }

    public void removeAllROIs() {
        rois = GmosCommonType.CustomROIList.create();
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

    public void updateSelectedROI(int xMin, int yMin, int xRange, int yRange) {
        GmosCommonType.ROIDescription roi = new GmosCommonType.ROIDescription(xMin, yMin, xRange, yRange);
        if (det != null && roi.validate(det.getXsize(), det.getYsize())) {
            rois = rois.update(getSelectedRow(), roi);
            _updateTable();
        } else {
            throw new IllegalArgumentException("ROI is not within valid ranges");
        }
    }

    public void addROI(int xMin, int yMin, int xRange, int yRange) {
        GmosCommonType.ROIDescription roi = new GmosCommonType.ROIDescription(xMin, yMin, xRange, yRange);
        if (det != null && roi.validate(det.getXsize(), det.getYsize())) {
            rois = rois.add(roi);
            _updateTable();
            _selectLastRow();
        } else {
            throw new IllegalArgumentException("ROI is not within valid ranges");
        }
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

//    private Vector<Integer> _createRow(int xMin, int yMin, int xRange, int yRange) {
//        Vector<Integer> v = new Vector<Integer>(4);
//        v.addElement(xMin);
//        v.addElement(yMin);
//        v.addElement(xRange);
//        v.addElement(yRange);
//        return v;
//    }

    private Vector<Integer> _ROIToVector(GmosCommonType.ROIDescription roi) {
        Vector<Integer> v = new Vector<Integer>(4);
        v.addElement(roi.getXStart());
        v.addElement(roi.getYStart());
        v.addElement(roi.getXSize(xBinning));
        v.addElement(roi.getYSize(yBinning));
        return v;
    }

    public GmosCommonType.CustomROIList getCustomROIs() {
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
     * @param maxRows the max number of rows of data allowed
     * @return true if the table was modified
     */
    public boolean paste(int maxRows) {
        String s = getClipboardContents();
        if (s.length() == 0) {
            DialogUtil.error(pasteError(s, maxRows));
            return false;
        }
        String[] lines = s.trim().split("\n");
        if (lines.length > maxRows) {
            DialogUtil.error("You cannot declare more than 5 custom ROIs for HAMAMATSU CCDs or 4 for E2V CCDs\n"
                    + pasteError(s, maxRows));
            return false;
        }
        int[][] ar = new int[lines.length][4];
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] values = line.trim().split("\\s+");
            if (values.length != 4) {
                DialogUtil.error("Invalid line: '" + line + "': Expected 4 integers separated by spaces\n"
                        + pasteError(s, maxRows));
                return false;
            }
            for (int j = 0; j < values.length; j++) {
                String value = values[j];
                try {
                    ar[i][j] = (Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    DialogUtil.error("Invalid value: '" + value + "': Expected an integer\n"
                            + pasteError(s, maxRows));
                    return false;
                }
            }
        }
        removeAllROIs();
        for(int[] a : ar) {
            addROI(a[0], a[1], a[2], a[3]);
        }
        return true;
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


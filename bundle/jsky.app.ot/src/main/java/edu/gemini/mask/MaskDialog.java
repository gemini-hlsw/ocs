package edu.gemini.mask;

import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.MainImageDisplay;
import jsky.util.gui.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Hashtable;


/**
 * Dialog for editing the mask making parameters and generating the mask files.
 */
public class MaskDialog implements PropertyChangeListener {

    // The GUI for this dialog
    private MaskDialogGUI _w = new MaskDialogGUI();

    // Reference to the image display, needed to get the binning
    // factor and current image height.
    private MainImageDisplay _imageDisplay;

    // Links property names to widgets
    private Hashtable _propTable = new Hashtable();

    // Holds the values being edited here
    private MaskParams _maskParams;

    public MaskDialog(ObjectTable table, MainImageDisplay imageDisplay) {
        _maskParams = table.getMaskParams();
        _imageDisplay = imageDisplay;
        _initPropTable();
        _initGui();
    }

    // Map properties to widgets
    private void _initPropTable() {
        _propTable.put(MaskParams.NUM_MASKS, _w.numMasks);
        _propTable.put(MaskParams.WAVELENGTH, _w.wavelength);
        _propTable.put(MaskParams.INSTRUMENT, _w.instrument);
        _propTable.put(MaskParams.DISPERSER, _w.disperser);
        _propTable.put(MaskParams.FILTER, _w.filter);

        _propTable.put(BandDef.SLIT_LENGTH, _w.slitLength);
        _propTable.put(BandDef.MICRO_SHUFFLE_AMOUNT, _w.microShuffleAmountArcsec);
        _propTable.put(BandDef.MICRO_SHUFFLE_PIX, _w.microShuffleAmountPixels);

        _propTable.put(BandDef.BAND_SIZE, _w.bandSize);
        _propTable.put(BandDef.BAND_SHUFFLE_AMOUNT, _w.bandShuffleAmountArcsec);
        _propTable.put(BandDef.BAND_SHUFFLE_PIX, _w.bandShuffleAmountPixels);
        _propTable.put(BandDef.BANDS_Y_AMOUNT, _w.bandsYOffset);
    }

    private void _initGui() {
        final BandDef bandDef = _maskParams.getBandDef();

        _instrumentChanged();
        _shuffleModeChanged();

        _w.wavelength.setValue(_maskParams.getWavelength());
        _w.filter.setSelectedItem(_maskParams.getFilter());
        _w.disperser.setSelectedItem(_maskParams.getDisperser());
        _w.numMasks.setValue(new Integer(_maskParams.getNumMasks()));

        _w.slitLength.setValue(bandDef.getSlitLength());
        _w.microShuffleAmountArcsec.setValue(bandDef.getMicroShuffleAmount());
        _w.microShuffleAmountPixels.setValue(bandDef.getMicroShufflePix());

        _w.bandSize.setValue(bandDef.getBandSize());
        _w.bandsYOffset.setValue(bandDef.getBandsYOffset());
        _w.bandShuffleAmountArcsec.setValue(bandDef.getBandShuffleAmount());
        _w.bandShuffleAmountPixels.setValue(bandDef.getBandShufflePix());

        _maskParams.addPropertyChangeListener(this);
        _maskParams.getBandDef().addPropertyChangeListener(this);

        _w.numMasks.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                _maskParams.setNumMasks(((Integer)_w.numMasks.getModel().getValue()).intValue());
            }
        });

        _w.wavelength.addWatcher(new TextBoxWidgetAdapter() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                _maskParams.setWavelength(tbw.getDoubleValue(_maskParams.getWavelength()));
            }
        });

        _w.instrument.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _maskParams.setInstrument((String)_w.instrument.getSelectedItem());
            }
        });

        _w.disperser.addWatcher(new DropDownListBoxWidgetWatcher() {
            public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
                try {
                    _maskParams.setDisperser((GmosCommonType.Disperser)ddlbw.getSelectedItem());
                } catch(IllegalArgumentException e) {
                    DialogUtil.error(e.getMessage());
                    _w.disperser.setSelectedItem(_maskParams.getDisperser());
                }
            }
        });

        _w.filter.addWatcher(new DropDownListBoxWidgetWatcher() {
            public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
                try {
                    _maskParams.setFilter((GmosCommonType.Filter)ddlbw.getSelectedItem());
                } catch(IllegalArgumentException e) {
                    DialogUtil.error(e.getMessage());
                    _w.filter.setSelectedItem(_maskParams.getFilter());
                }
            }
        });

        _w.shuffleMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bandDef.setShuffleMode(_w.shuffleMode.getSelectedIndex());
            }
        });

        _w.slitLength.addWatcher(new jsky.util.gui.TextBoxWidgetAdapter() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                bandDef.setSlitLength(tbw.getIntegerValue(bandDef.getSlitLength()));
            }
        });

        _w.microShuffleAmountArcsec.addWatcher(new jsky.util.gui.TextBoxWidgetAdapter() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                bandDef.setMicroShuffleAmount(tbw.getDoubleValue(bandDef.getMicroShuffleAmount()));
            }
        });

        _w.microShuffleAmountPixels.addWatcher(new TextBoxWidgetAdapter() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                bandDef.setMicroShufflePix(tbw.getDoubleValue(bandDef.getMicroShufflePix()));
            }
        });

        _w.bandSize.addWatcher(new TextBoxWidgetAdapter() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                bandDef.setBandSize(tbw.getIntegerValue(bandDef.getBandSize()));
            }
        });

        _w.bandShuffleAmountArcsec.addWatcher(new TextBoxWidgetAdapter() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                bandDef.setBandShuffleAmount(tbw.getDoubleValue(bandDef.getBandShuffleAmount()));
            }
        });

        _w.bandShuffleAmountPixels.addWatcher(new TextBoxWidgetAdapter() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                bandDef.setBandShufflePix(tbw.getDoubleValue(bandDef.getBandShufflePix()));
            }
        });

        _w.bandsYOffset.addWatcher(new TextBoxWidgetAdapter() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                bandDef.setBandsYOffset(tbw.getIntegerValue(bandDef.getBandsYOffset()));
            }
        });

        // --

        _w.bandResetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bandDef.resetToDefault(BandDef.BAND_SHUFFLE);
            }
        });

        _w.microResetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bandDef.resetToDefault(BandDef.MICRO_SHUFFLE);
            }
        });

        _w.makeMaskFilesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _makeMaskFiles();
            }
        });

        _w.cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _w.setVisible(false);
            }
        });

        _w.pack();
    }


    private TableModel _getBandsTableModel(final BandDef.Band[] bands) {
        return new AbstractTableModel() {
            String[] _columnNames = new String[] {
                "Number", "Name", "Y Position", "Height"
            };
            public String getColumnName(int col) {
                return _columnNames[col];
            }
            public int getRowCount() { return bands.length; }
            public int getColumnCount() { return _columnNames.length; }
            public Object getValueAt(int row, int col) {
                switch(col) {
                    case 0: return new Integer(bands[row].getNum());
                    case 1: return bands[row].getName();
                    case 2: return new Double(bands[row].getYPos());
                    case 3: return new Double(bands[row].getHeight());
                }
                return null;
            }
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    // Called when the shuffle mode is changed to enable and display the correct tab
    private void _shuffleModeChanged() {
        BandDef bandDef = _maskParams.getBandDef();

        // update image related values
        bandDef.setBinning(_getBinning());
        bandDef.setImageHeight(_getImageHeight());

        int shuffleMode = bandDef.getShuffleMode();
        if (shuffleMode != _w.shuffleMode.getSelectedIndex()) {
            _w.shuffleMode.setSelectedIndex(shuffleMode);
        }

        // enable related tab pane
        _w.shufflePane.setEnabledAt(BandDef.NO_SHUFFLE, false);
        _w.shufflePane.setEnabledAt(BandDef.MICRO_SHUFFLE, false);
        _w.shufflePane.setEnabledAt(BandDef.BAND_SHUFFLE, false);
        _w.shufflePane.setEnabledAt(shuffleMode, true);
        _w.shufflePane.setSelectedIndex(shuffleMode);
    }

    // Set the GUI to display the filter and disperser choices for the given instrument.
    private void _instrumentChanged() {
        String instName = _maskParams.getInstrument();
        if (!instName.equals(_w.instrument.getSelectedItem())) {
            _w.instrument.setSelectedItem(instName);
        }
        if (instName.equals("GMOS-N")) {
            _w.filter.setChoices(GmosNorthType.FilterNorth.values());
            _w.filter.setMaximumRowCount(GmosNorthType.FilterNorth.values().length);
            _w.disperser.setChoices(GmosNorthType.DisperserNorth.values());
        } else if (instName.equals("GMOS-S")) {
            _w.filter.setChoices(GmosSouthType.FilterSouth.values());
            _w.filter.setMaximumRowCount(GmosSouthType.FilterSouth.values().length);
            _w.disperser.setChoices(GmosSouthType.DisperserSouth.values());
        }
    }

    // run the spoc algorithm
    private void _makeMaskFiles() {
        String msg = _maskParams.getBandDef().validate();
        if (msg != null) {
            DialogUtil.error(_w, msg);
            return;
        }

        if (!_checkOverwriteOdf()) {
            return;
        }

        try {
            new Spoc(_maskParams);
        } catch (Exception e) {
            DialogUtil.error(_w, e);
            return;
        }

        String dir = new File(Spoc.getOdfFileName(_maskParams, 1, ".fits")).getParent();
        int numMasks = _maskParams.getNumMasks();
        String files = (numMasks == 1 ? "file" : "files");
        DialogUtil.message(_w, "Done. Wrote " + numMasks + " minimal ODF FITS table "
                + files + " in " + dir);
        _w.setVisible(false);
    }

    // Return true if no ODF file exists, or if the user confirms overwriting an existing
    // one, or specifies a new name.
    private boolean _checkOverwriteOdf() {
        File file = new File(Spoc.getOdfFileName(_maskParams, 1, ".fits"));
        if (file.exists()) {

            String title = "Confirm";
            String msg = "The output file " + file.getName() + " already exists."
                    + " What Do you want to do?";
            String[] choices = {"Overwrite", "Enter New Name", "Cancel"};
            int val = 0;
            String defaultChoice = choices[val];

            Object[] array = {msg};

            val = JOptionPane.showOptionDialog(_w,
                    array,
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    choices,
                    defaultChoice);

            // answer is index in choices
            switch (val) {
                case 0: // Overwrite
                    return true;
                case 1: // Enter New Name
                    return _enterNewName();
                case 2: // Cancel
                    return false;
            }
        }
        return true;
    }

    // Ask the user to enter a new name and return false if cancel was pressed, otherwise
    // true
    private boolean _enterNewName() {
        while(true) {
            String msg = "Enter New Name (ODF<maskNum>.fits will be appended to name)";
            String newName = DialogUtil.input(_w, msg);
            if (newName == null || newName.length() == 0) {
                return false;
            }
            if (_changeTableFileName(newName)) {
                return true;
            }
        }
    }

    // Change the object table file name and return true if ok
    private boolean _changeTableFileName(String name) {
        if (name.indexOf(File.separatorChar) != -1
            || name.indexOf(' ') != -1) {
            DialogUtil.error(_w, "Invalid name: Please enter only letters, numbers or underscores.");
            return false;
        }
        ObjectTable table = _maskParams.getTable();
        table.setName(name);
        return true;
    }

    public void setVisible(boolean b) {
        _w.setVisible(b);
    }

    public void setTable(ObjectTable table) {
        _maskParams.setTable(table);
    }

    // Called when one of the mask parameters changes. Update the GUI to display the
    // new value, being careful to avoid recursion.
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        Object value = evt.getNewValue();

        if (name.equals(BandDef.SHUFFLE_MODE)) {
            _shuffleModeChanged();
        } else if (name.equals(MaskParams.INSTRUMENT)) {
            _instrumentChanged();
        } else if (name.equals(BandDef.BANDS)) {
            _w.bandTable.setModel(_getBandsTableModel((BandDef.Band[])value));
        } else {
            Object w = _propTable.get(name);
            if (w instanceof NumberBoxWidget) {
                Number n = (Number)value;
                NumberBoxWidget nbw = (NumberBoxWidget)w;
                if (n instanceof Double) {
                    if (!n.equals(new Double(nbw.getDoubleValue(n.doubleValue())))) {
                        nbw.setValue(n.doubleValue());
                    }
                } else if (n instanceof Integer) {
                    if (!n.equals(new Integer(nbw.getIntegerValue(n.intValue())))) {
                        nbw.setValue(n.intValue());
                    }
                }
            } else if (w instanceof JTextField) {
                String s = value.toString();
                JTextField tf = (JTextField)w;
                if (!s.equals(tf.getText())) {
                    tf.setText(s);
                }
            } else if (w instanceof JComboBox) {
                JComboBox cb = (JComboBox)w;
                if (!value.equals(cb.getSelectedItem())) {
                    cb.setSelectedItem(value);
                }
            }
        }
    }

    // Get the displayed image height
    private int _getImageHeight() {
        if (_imageDisplay != null) {
            return _imageDisplay.getImageHeight();
        }
        return 0;
    }

    // Get the binning factor from the image header
    private int _getBinning() {
        int binning = 1;
        if (_imageDisplay != null) {
            FITSImage fitsImage = _imageDisplay.getFitsImage();
            if (fitsImage == null) {
                DialogUtil.error(_w, "Please open the associated FITS image for display.");
            } else {
                String ccdsum = _imageDisplay.getFitsImage().getKeywordValue("CCDSUM",
                        "1 1");
                String[] ar = ccdsum.split(" ", 2);
                int binX = Integer.parseInt(ar[0]);
                int binY = Integer.parseInt(ar[1]);
                if (binX != binY) {
                    DialogUtil.error(_w, "WARNING Binning is Asymetric: CCDSUM=" + ccdsum
                            + ". Using binning=1");
                } else {
                    binning = binX;
                }
            }
        }
        return binning;
    }
}

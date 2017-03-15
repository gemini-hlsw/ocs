package jsky.app.ot.gemini.trecs;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.trecs.InstEngTReCS;
import edu.gemini.spModel.gemini.trecs.TReCSParams.*;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.editor.type.SpTypeUIUtil;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This is the editor for the TReCS instrument component.
 */
public final class EdCompInstEngTReCS extends OtItemEditor<ISPObsComponent, InstEngTReCS>
        implements ActionListener {

    // The GUI layout panel
    private final EngTReCSForm _w;

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstEngTReCS() {
        _w = new EngTReCSForm();

        // initialize the combo boxes
        SpTypeUIUtil.initListBox(_w.sectorWheelComboBox, SectorWheel.class, this);
        SpTypeUIUtil.initListBox(_w.lyotWheelComboBox, LyotWheel.class, this);
        SpTypeUIUtil.initListBox(_w.pupilImagingWheelComboBox, PupilImagingWheel.class, this);
        SpTypeUIUtil.initListBox(_w.apertureWheelComboBox, ApertureWheel.class, this);
        SpTypeUIUtil.initListBox(_w.wellDepthComboBox, WellDepth.class, this);
        SpTypeUIUtil.initListBox(_w.nodHandShakeComboBox,YesNoType.class, this);

        _w.frameTimeComboBox.addActionListener(this);
        _w.chopFrequencyComboBox.addActionListener(this);
    }


    /** Return the window containing the editor */
    @Override
    public JPanel getWindow() {
        return _w;
    }

    /** Set the data object corresponding to this editor. */
    @Override
    public void init() {
        _w.sectorWheelComboBox.getModel().setSelectedItem(getDataObject().getSectorWheel());
        _w.lyotWheelComboBox.getModel().setSelectedItem(getDataObject().getLyotWheel());
        _w.pupilImagingWheelComboBox.getModel().setSelectedItem(getDataObject().getPupilImagingWheel());
        _w.apertureWheelComboBox.getModel().setSelectedItem(getDataObject().getApertureWheel());
        _w.wellDepthComboBox.getModel().setSelectedItem(getDataObject().getWellDepth());
        _w.nodHandShakeComboBox.getModel().setSelectedItem(getDataObject().getNodHandshake());
        _w.frameTimeComboBox.getModel().setSelectedItem(getDataObject().getExposureTime());
        _w.chopFrequencyComboBox.getModel().setSelectedItem(getDataObject().getChopFrequency());
    }

    /**
     * Called when an item in a ComboBox is selected
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final Object w = actionEvent.getSource();
        if (w == _w.sectorWheelComboBox) {
            getDataObject().setSectorWheel((SectorWheel)_w.sectorWheelComboBox.getSelectedItem());
        } else if (w == _w.lyotWheelComboBox) {
            getDataObject().setLyotWheel((LyotWheel)_w.lyotWheelComboBox.getSelectedItem());
        } else if (w == _w.pupilImagingWheelComboBox) {
            getDataObject().setPupilImagingWheel((PupilImagingWheel)_w.pupilImagingWheelComboBox.getSelectedItem());
        } else if (w == _w.apertureWheelComboBox) {
            getDataObject().setApertureWheel((ApertureWheel)_w.apertureWheelComboBox.getSelectedItem());
        } else if (w == _w.wellDepthComboBox) {
            getDataObject().setWellDepth((WellDepth)_w.wellDepthComboBox.getSelectedItem());
        } else if (w == _w.nodHandShakeComboBox) {
            getDataObject().setNodHandshake((YesNoType)_w.nodHandShakeComboBox.getSelectedItem());
        } else if (w == _w.frameTimeComboBox) {
            getDataObject().setExposureTime(_w.frameTimeComboBox.getSelectedItem().toString());
        } else if (w == _w.chopFrequencyComboBox) {
            getDataObject().setChopFrequency(_w.chopFrequencyComboBox.getSelectedItem().toString());
        }
    }
}


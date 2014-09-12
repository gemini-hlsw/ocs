package jsky.app.ot.gemini.nici;

import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nici.NICIParams;
import edu.gemini.spModel.telescope.IssPort;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.type.SpTypeUIUtil;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Editor component for NICI.
 * $Id: EdCompInstNICI.java 24991 2010-04-12 15:12:40Z swalker $
 */
public final class EdCompInstNICI extends EdCompInstBase<InstNICI> {

    // GUI needs to know when the data object changes, so we support change listeners for this.
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final NICIForm gui = new NICIForm();
    private NICIFormEng eng;
    private NICIGui tabbed;
    private PropertyChangeListener pcl;

    public EdCompInstNICI() {
        super();
        if (OTOptions.isStaffGlobally()) { //getProgram().getProgramID())) {
            eng = new NICIFormEng();
            tabbed = new NICIGui(gui, eng);
        }
        initializeComponents();
        //SCT-231: Hide number of exposure widget since it's not implemented yet
        gui.getExposuresTB().setVisible(false);
        gui.getExposuresLabel().setVisible(false);
    }


    public JPanel getWindow() {
//        return OTOptions.isStaff(getProgram().getProgramID()) ? tabbed : gui;
        return OTOptions.isStaffGlobally() ? tabbed : gui;
    }

    @Override
    protected void cleanup() {
        pcs.firePropertyChange("DataObject", getDataObject(), null); // RCN: good enough?
    }

    @Override protected void init() {
        super.init();
        pcs.firePropertyChange("DataObject", null, getDataObject()); // RCN: good enough?
        getDataObject().addPropertyChangeListener(getPropertyChangeListener());
        gui.getFocalPlaneCB().getModel().setSelectedItem(getDataObject().getFocalPlaneMask());
        gui.getPupilMaskCB().getModel().setSelectedItem(getDataObject().getPupilMask());
        gui.getCassRotatorCB().getModel().setSelectedItem(getDataObject().getCassRotator());

        gui.getPositionAngleTB().setText(getDataObject().getPosAngleDegreesStr());
        gui.getImagingModeCB().getModel().setSelectedItem(getDataObject().getImagingMode());
        gui.getDichroicWheelCB().getModel().setSelectedItem(getDataObject().getDichroicWheel());
        gui.getChannel1CB().getModel().setSelectedItem(getDataObject().getChannel1Fw());
        gui.getChannel2CB().getModel().setSelectedItem(getDataObject().getChannel2Fw());
        gui.getExposuresTB().setText(Integer.toString(getDataObject().getExposures()));
        gui.getWellDepthCB().getModel().setSelectedItem(getDataObject().getWellDepth());
        updatePortButton();

        //engineering components
        if (OTOptions.isStaff(getProgram().getProgramID())) {
            eng.getFocsCB().getModel().setSelectedItem(getDataObject().getFocs());
            eng.getPupilImagerCB().getModel().setSelectedItem(getDataObject().getPupilImager());
            eng.getSpiderMaskCB().getModel().setSelectedItem(getDataObject().getSpiderMask());
            eng.getSmrAngleTF().setText(Double.toString(getDataObject().getSMRAngle()));
            eng.getDhsModeCB().getModel().setSelectedItem(getDataObject().getDhsMode());
        }
        //make the gui aware of the changes.
        updateGUI();
    }

    public TextBoxWidget getPosAngleTextBox() {
        //return gui.getPositionAngleTB();
        return null;
    }

    public TextBoxWidget getExposureTimeTextBox() {
        return gui.getExposureTimeTB();
    }

    public TextBoxWidget getCoaddsTextBox() {
        return gui.getCoaddsTB();
    }

    private final TextBoxWidgetWatcher crAngleWatcher = new TextBoxWidgetWatcher() {
        @Override public void textBoxKeyPress(TextBoxWidget tbwe) {
            if (getDataObject() == null) return;

            try {
                // Stop watching for changes or else we'll try to update the GUI
                // as a result of getting a property change event for the
                // angle update -- if we do that, then we'll be trying to
                // update the text box widget which we're currently editing
                // anyway.
                getDataObject().removePropertyChangeListener(pcl);

                // Update the proper angle based upon the current cass rotator
                // setting.
                final Angle angle = new Angle(tbwe.getDoubleValue(0), Angle.Unit.DEGREES);
                if (getDataObject().getCassRotator() == NICIParams.CassRotator.FIXED) {
                    getDataObject().setCassRotatorFixedAngle(angle);
                } else {
                    getDataObject().setCassRotatorFollowAngle(angle);
                }
            } finally {
                // Restore the property change listener
                getDataObject().addPropertyChangeListener(pcl);
            }
        }

        @Override public void textBoxAction(TextBoxWidget tbwe) {
            textBoxKeyPress(tbwe);
        }
    };

    protected void initializeComponents() {
        gui.getPositionAngleTB().addWatcher(crAngleWatcher);
        SpTypeUIUtil.initListBox(gui.getFocalPlaneCB(), NICIParams.FocalPlaneMask.class, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setFocalPlaneMask((NICIParams.FocalPlaneMask) gui.getFocalPlaneCB().getSelectedItem());
            }
        });

        SpTypeUIUtil.initListBox(gui.getPupilMaskCB(), NICIParams.PupilMask.class, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setPupilMask((NICIParams.PupilMask) gui.getPupilMaskCB().getSelectedItem());
            }
        });

        SpTypeUIUtil.initListBox(gui.getCassRotatorCB(), NICIParams.CassRotator.class, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setCassRotator((NICIParams.CassRotator) gui.getCassRotatorCB().getSelectedItem());
            }
        });

        SpTypeUIUtil.initListBox(gui.getImagingModeCB(), NICIParams.ImagingMode.class, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final NICIParams.ImagingMode imageMode = (NICIParams.ImagingMode) gui.getImagingModeCB().getSelectedItem();
                getDataObject().setImagingMode(imageMode);
            }
        });

        SpTypeUIUtil.initListBox(gui.getDichroicWheelCB(), NICIParams.DichroicWheel.class, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setDichroicWheel((NICIParams.DichroicWheel) gui.getDichroicWheelCB().getSelectedItem());
            }
        });

        SpTypeUIUtil.initListBox(gui.getChannel1CB(), NICIParams.Channel1FW.class, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setChannel1Fw((NICIParams.Channel1FW) gui.getChannel1CB().getSelectedItem());
            }
        });

        SpTypeUIUtil.initListBox(gui.getChannel2CB(), NICIParams.Channel2FW.class, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setChannel2Fw((NICIParams.Channel2FW) gui.getChannel2CB().getSelectedItem());
            }
        });

        SpTypeUIUtil.initListBox(gui.getWellDepthCB(), NICIParams.WellDepth.class, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setWellDepth((NICIParams.WellDepth) gui.getWellDepthCB().getSelectedItem());
            }
        });

        gui.getExposuresTB().addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                if (getDataObject() != null) {
                    final int val = gui.getExposuresTB().getIntegerValue(1);
                    getDataObject().setExposures(val);
                }
            }

            public void textBoxAction(TextBoxWidget tbwe) {
                //do nothing
            }
        });

        gui.getPortButtonSide().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setIssPort(IssPort.SIDE_LOOKING);
            }
        });
        gui.getPortButtonUp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getDataObject().setIssPort(IssPort.UP_LOOKING);
            }
        });

        //Engineering components
        if (OTOptions.isStaffGlobally()) { // (getProgram().getProgramID())) {
            SpTypeUIUtil.initListBox(eng.getFocsCB(), NICIParams.Focs.class, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getDataObject().setFocs((NICIParams.Focs) eng.getFocsCB().getSelectedItem());
                }
            });

            SpTypeUIUtil.initListBox(eng.getPupilImagerCB(), NICIParams.PupilImager.class, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getDataObject().setPupilImager((NICIParams.PupilImager) eng.getPupilImagerCB().getSelectedItem());
                }
            });

            SpTypeUIUtil.initListBox(eng.getSpiderMaskCB(), NICIParams.SpiderMask.class, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getDataObject().setSpiderMask((NICIParams.SpiderMask) eng.getSpiderMaskCB().getSelectedItem());
                }
            });

            eng.getSmrAngleTF().addWatcher(new TextBoxWidgetWatcher() {
                public void textBoxKeyPress(TextBoxWidget tbwe) {
                    if (getDataObject() != null) {
                        final double val = eng.getSmrAngleTF().getDoubleValue(0);
                        getDataObject().setSMRAngle(val);
                    }
                }

                public void textBoxAction(TextBoxWidget tbwe) {
                    //do nothing
                }
            });

            SpTypeUIUtil.initListBox(eng.getDhsModeCB(), NICIParams.DHSMode.class, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getDataObject().setDhsMode((NICIParams.DHSMode) eng.getDhsModeCB().getSelectedItem());
                }
            });

        }
    }

    private void updatePortButton() {
        switch (getDataObject().getIssPort()) {
            case SIDE_LOOKING: gui.getPortButtonSide().setSelected(true); break;
            case UP_LOOKING:   gui.getPortButtonUp().setSelected(true); break;
        }
    }

    private void updateGUI() {

        //cass rotator labels
        final Angle angle;
        if (getDataObject().getCassRotator() == NICIParams.CassRotator.FIXED) {
            gui.getPaLabel1().setText("CR Angle");
            gui.getPaLabel2().setText("(degrees)");
            angle = getDataObject().getCassRotatorFixedAngle();
        } else {
            gui.getPaLabel1().setText("Position Angle");
            gui.getPaLabel2().setText("(degrees E of N)");
            angle = getDataObject().getCassRotatorFollowAngle();
        }

        gui.getPositionAngleTB().deleteWatcher(crAngleWatcher);
        gui.getPositionAngleTB().setText(Double.toString(angle.toDegrees().getMagnitude()));
        gui.getPositionAngleTB().addWatcher(crAngleWatcher);

        //imaging Mode metaconfig
        if (getDataObject().getImagingMode() != NICIParams.ImagingMode.MANUAL) {
            //gray out the metaconfig fields.
            gui.getDichroicWheelCB().setEnabled(false);
            gui.getChannel1CB().setEnabled(false);
            gui.getChannel2CB().setEnabled(false);
            if (OTOptions.isStaff(getProgram().getProgramID())) {
                eng.getPupilImagerCB().setEnabled(false);
            }

        } else {
            gui.getDichroicWheelCB().setEnabled(true);
            gui.getChannel1CB().setEnabled(true);
            gui.getChannel2CB().setEnabled(true);
            if (OTOptions.isStaff(getProgram().getProgramID())) {
                eng.getPupilImagerCB().setEnabled(true);
            }
        }

        gui.getPupilMaskCB().setEnabled(true);
        gui.getExposureTimeTB().setEnabled(true);
        if (OTOptions.isStaff(getProgram().getProgramID())) {
            eng.getFocsCB().setEnabled(true);
        }
    }

//    private void updateCrAngleDisplay() {
//        gui.getPositionAngleTB().deleteWatcher(crAngleWatcher);
//        gui.getPositionAngleTB().setText(Double.toString(angle.toDegrees().getMagnitude()));
//        gui.getPositionAngleTB().addWatcher(crAngleWatcher);
//    }

    private PropertyChangeListener getPropertyChangeListener() {
        if (pcl != null) {
            return pcl;
        }
        pcl = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final String pName = evt.getPropertyName();
                if (evt.getSource() != getDataObject()) return;
                if (pName.equals(InstNICI.FOCAL_PLANE_MASK_PROP.getName())) {
                    gui.getFocalPlaneCB().getModel().setSelectedItem(getDataObject().getFocalPlaneMask());
                } else if (pName.equals(InstNICI.PUPIL_MASK_PROP.getName())) {
                    gui.getPupilMaskCB().getModel().setSelectedItem(getDataObject().getPupilMask());
                } else if (pName.equals(InstNICI.CASS_ROTATOR_PROP.getName())) {
                    gui.getCassRotatorCB().getModel().setSelectedItem(getDataObject().getCassRotator());
                } else if (pName.equals(InstNICI.IMAGING_MODE_PROP.getName())) {
                    gui.getImagingModeCB().getModel().setSelectedItem(getDataObject().getImagingMode());
                } else if (pName.equals(InstNICI.DICHROIC_WHEEL_PROP.getName())) {
                    gui.getDichroicWheelCB().getModel().setSelectedItem(getDataObject().getDichroicWheel());
                } else if (pName.equals(InstNICI.CHANNEL1_FW_PROP.getName())) {
                    gui.getChannel1CB().getModel().setSelectedItem(getDataObject().getChannel1Fw());
                } else if (pName.equals(InstNICI.CHANNEL2_FW_PROP.getName())) {
                    gui.getChannel2CB().getModel().setSelectedItem(getDataObject().getChannel2Fw());
                } else if (pName.equals(InstNICI.WELL_DEPTH_PROP.getName())) {
                    gui.getWellDepthCB().getModel().setSelectedItem(getDataObject().getWellDepth());
                } else if (pName.equals(InstNICI.PORT_PROP.getName())) {
                    updatePortButton();
                }
                //Engineering fields
                if (eng != null) {
                    if (pName.equals(InstNICI.FOCS_PROP.getName())) {
                        eng.getFocsCB().getModel().setSelectedItem(getDataObject().getFocs());
                    } else if (pName.equals(InstNICI.PUPIL_IMAGER_PROP.getName())) {
                        eng.getPupilImagerCB().getModel().setSelectedItem(getDataObject().getPupilImager());
                    } else if (pName.equals(InstNICI.SPIDER_MASK_PROP.getName())) {
                        eng.getSpiderMaskCB().getModel().setSelectedItem(getDataObject().getSpiderMask());
                    } else if (pName.equals(InstNICI.DHS_MODE_PROP.getName())) {
                        eng.getDhsModeCB().getModel().setSelectedItem(getDataObject().getDhsMode());
                    }
                }
                //make the gui aware of the changes.
                updateGUI();
            }
        };

        return pcl;
    }
}

package jsky.app.ot.gemini.trecs;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.spModel.gemini.trecs.TReCSParams;
import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;

public class TReCSForm extends JPanel {
    public TReCSForm() {
        initComponents();
    }

    private void initComponents() {
        disperserLabel = new JLabel();
        disperserComboBox = new JComboBox<>();
        centralWavelengthLabel = new JLabel();
        centralWavelength = new NumberBoxWidget();
        filterLabel = new JLabel();
        filterComboBox = new JComboBox<>();
        winWheelLabel = new JLabel();
        winWheelComboBox = new JComboBox<>();
        focalPlaneMaskLabel = new JLabel();
        focalPlaneMaskComboBox = new JComboBox<>();
        posAngleLabel = new JLabel();
        posAngle = new NumberBoxWidget();
        dataModeLabel = new JLabel();
        dataModeComboBox = new JComboBox<>();
        readoutModeLabel = new JLabel();
        readoutModeComboBox = new JComboBox<>();
        obsModeLabel = new JLabel();
        obsModeComboBox = new JComboBox<>();
        nodOrientationLabel = new JLabel();
        nodOrientationComboBox = new JComboBox<>();
        chopThrowLabel = new JLabel();
        chopThrow = new NumberBoxWidget();
        chopAngleLabel = new JLabel();
        chopAngle = new NumberBoxWidget();
        totalOnSourceTimeLabel = new JLabel();
        totalOnSourceTime = new NumberBoxWidget();
        savesetTimeLabel = new JLabel();
        savesetTime = new NumberBoxWidget();
        nodDwellLabel = new JLabel();
        nodDwell = new NumberBoxWidget();
        nodSettleLabel = new JLabel();
        nodSettle = new NumberBoxWidget();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setLayout(new FormLayout(
            new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                ColumnSpec.decode("max(pref;50dlu)"),
                new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX11, FormSpec.NO_GROW),
                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                ColumnSpec.decode("max(pref;50dlu)")
            },
            new RowSpec[] {
                new RowSpec(Sizes.DLUY11),
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                new RowSpec(RowSpec.TOP, Sizes.dluY(17), FormSpec.NO_GROW),
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                new RowSpec(RowSpec.TOP, Sizes.dluY(17), FormSpec.NO_GROW),
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC
            }));

        //---- disperserLabel ----
        disperserLabel.setText("Disperser");
        disperserLabel.setLabelFor(disperserComboBox);
        add(disperserLabel, cc.xy(1, 3));
        add(disperserComboBox, cc.xy(3, 3));

        //---- centralWavelengthLabel ----
        centralWavelengthLabel.setText("Central Wavelength");
        centralWavelengthLabel.setToolTipText("Grating Central Wavelength");
        centralWavelengthLabel.setLabelFor(centralWavelength);
        add(centralWavelengthLabel, cc.xy(5, 3));

        //---- centralWavelength ----
        centralWavelength.setToolTipText("Enter the Grating Central Wavelength in um");
        add(centralWavelength, cc.xy(7, 3));

        //---- filterLabel ----
        filterLabel.setText("Filter");
        filterLabel.setLabelFor(filterComboBox);
        add(filterLabel, cc.xy(1, 5));
        add(filterComboBox, cc.xy(3, 5));

        //---- winWheelLabel ----
        winWheelLabel.setText("Window Wheel");
        winWheelLabel.setLabelFor(winWheelComboBox);
        add(winWheelLabel, cc.xy(5, 5));
        add(winWheelComboBox, cc.xy(7, 5));

        //---- focalPlaneMaskLabel ----
        focalPlaneMaskLabel.setText("Focal Plane Mask");
        focalPlaneMaskLabel.setLabelFor(focalPlaneMaskComboBox);
        add(focalPlaneMaskLabel, cc.xy(1, 7));
        add(focalPlaneMaskComboBox, cc.xy(3, 7));

        //---- posAngleLabel ----
        posAngleLabel.setText("Position Angle");
        posAngleLabel.setLabelFor(posAngle);
        add(posAngleLabel, cc.xy(5, 7));

        //---- posAngle ----
        posAngle.setToolTipText("Enter the Position Angle in degrees E of N");
        add(posAngle, cc.xy(7, 7));

        //---- dataModeLabel ----
        dataModeLabel.setText("Data Mode");
        dataModeLabel.setLabelFor(dataModeComboBox);
        add(dataModeLabel, cc.xy(1, 9));
        add(dataModeComboBox, cc.xy(3, 9));

        //---- readoutModeLabel ----
        readoutModeLabel.setText("Readout Mode");
        readoutModeLabel.setLabelFor(readoutModeComboBox);
        add(readoutModeLabel, cc.xy(5, 9));
        add(readoutModeComboBox, cc.xy(7, 9));

        //---- obsModeLabel ----
        obsModeLabel.setText("Observing Mode");
        obsModeLabel.setLabelFor(obsModeComboBox);
        add(obsModeLabel, cc.xy(1, 11));
        add(obsModeComboBox, cc.xy(3, 11));

        //---- nodOrientationLabel ----
        nodOrientationLabel.setText("Nod Orientation");
        nodOrientationLabel.setLabelFor(nodOrientationComboBox);
        add(nodOrientationLabel, cc.xy(5, 11));
        add(nodOrientationComboBox, cc.xy(7, 11));

        //---- chopThrowLabel ----
        chopThrowLabel.setText("Chop Throw ");
        chopThrowLabel.setLabelFor(chopThrow);
        add(chopThrowLabel, cc.xy(1, 13));

        //---- chopThrow ----
        chopThrow.setToolTipText("Enter the Chop Throw in arcsec");
        add(chopThrow, cc.xy(3, 13));

        //---- chopAngleLabel ----
        chopAngleLabel.setText("Chop Angle");
        chopAngleLabel.setLabelFor(chopAngle);
        add(chopAngleLabel, cc.xy(5, 13));

        //---- chopAngle ----
        chopAngle.setToolTipText("Enter the Chop Angle in degrees E of N");
        add(chopAngle, cc.xy(7, 13));

        //---- totalOnSourceTimeLabel ----
        totalOnSourceTimeLabel.setText("Total On-Src Time");
        totalOnSourceTimeLabel.setLabelFor(totalOnSourceTime);
        add(totalOnSourceTimeLabel, cc.xy(1, 15));

        //---- totalOnSourceTime ----
        totalOnSourceTime.setToolTipText("Enter the Total On-Src Time in seconds");
        add(totalOnSourceTime, cc.xy(3, 15));

        //---- savesetTimeLabel ----
        savesetTimeLabel.setText("Saveset Time");
        savesetTimeLabel.setLabelFor(savesetTime);
        add(savesetTimeLabel, cc.xy(5, 15));

        //---- savesetTime ----
        savesetTime.setToolTipText("Enter the Time per Saveset in Seconds");
        add(savesetTime, cc.xy(7, 15));

        //---- nodDwellLabel ----
        nodDwellLabel.setText("Nod Dwell");
        nodDwellLabel.setLabelFor(nodDwell);
        add(nodDwellLabel, cc.xy(1, 17));

        //---- nodDwell ----
        nodDwell.setToolTipText("Enter the Nod Dwell in seconds");
        add(nodDwell, cc.xy(3, 17));

        //---- nodSettleLabel ----
        nodSettleLabel.setText("Nod Settle");
        nodSettleLabel.setLabelFor(nodSettle);
        add(nodSettleLabel, cc.xy(5, 17));

        //---- nodSettle ----
        nodSettle.setToolTipText("Enter the Nod Settle in seconds");
        add(nodSettle, cc.xy(7, 17));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    JLabel disperserLabel;
    JComboBox<TReCSParams.Disperser> disperserComboBox;
    JLabel centralWavelengthLabel;
    NumberBoxWidget centralWavelength;
    JLabel filterLabel;
    JComboBox<TReCSParams.Filter> filterComboBox;
    JLabel winWheelLabel;
    JComboBox<TReCSParams.WindowWheel> winWheelComboBox;
    JLabel focalPlaneMaskLabel;
    JComboBox<TReCSParams.Mask> focalPlaneMaskComboBox;
    JLabel posAngleLabel;
    NumberBoxWidget posAngle;
    JLabel dataModeLabel;
    JComboBox<TReCSParams.DataMode> dataModeComboBox;
    JLabel readoutModeLabel;
    JComboBox<TReCSParams.ReadoutMode> readoutModeComboBox;
    JLabel obsModeLabel;
    JComboBox<TReCSParams.ObsMode> obsModeComboBox;
    JLabel nodOrientationLabel;
    JComboBox<TReCSParams.NodOrientation> nodOrientationComboBox;
    JLabel chopThrowLabel;
    NumberBoxWidget chopThrow;
    JLabel chopAngleLabel;
    NumberBoxWidget chopAngle;
    JLabel totalOnSourceTimeLabel;
    NumberBoxWidget totalOnSourceTime;
    JLabel savesetTimeLabel;
    NumberBoxWidget savesetTime;
    JLabel nodDwellLabel;
    NumberBoxWidget nodDwell;
    JLabel nodSettleLabel;
    NumberBoxWidget nodSettle;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

/**
 * Title:        JSky<p>
 * Description:  AcqCam Instrument Editor GUI<p>
 * Company:      Gemini<p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.gemini.acqcam;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;

public class AcqCamGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel expTimeLabel = new JLabel();
    NumberBoxWidget exposureTime = new NumberBoxWidget();
    JLabel disperserLabel = new JLabel();
    DropDownListBoxWidget ndFilter = new DropDownListBoxWidget();
    JLabel posAngleLabel = new JLabel();
    NumberBoxWidget posAngle = new NumberBoxWidget();
    TitledBorder titledBorder1;
    JLabel expTimeUnits = new JLabel();
    JLabel posAngleUnits = new JLabel();
    ButtonGroup filterButtonGroup = new ButtonGroup();
    ButtonGroup readModeButtonGroup = new ButtonGroup();
    JLabel colorFilterLabel = new JLabel();
    DropDownListBoxWidget colorFilter = new DropDownListBoxWidget();
    ButtonGroup roiButtonGroup = new ButtonGroup();
    JLabel windowingLabel = new JLabel();
    JLabel xLabel = new JLabel();
    JLabel yLabel = new JLabel();
    JLabel widthLabel = new JLabel();
    JLabel heightLabel = new JLabel();
    JPanel windowingPanel = new JPanel();
    JRadioButton windowingOffButton = new JRadioButton();
    JRadioButton windowingOnButton = new JRadioButton();
    ButtonGroup binningButtonGroup = new ButtonGroup();
    ButtonGroup windowingButtonGroup = new ButtonGroup();
    NumberBoxWidget x = new NumberBoxWidget();
    NumberBoxWidget y = new NumberBoxWidget();
    NumberBoxWidget width = new NumberBoxWidget();
    NumberBoxWidget height = new NumberBoxWidget();
    JLabel xComment = new JLabel();
    Component fill;
    JLabel widthComment = new JLabel();
    JTabbedPane jTabbedPane1 = new JTabbedPane();
    JPanel binningPanel = new JPanel();
    JPanel roiPanel = new JPanel();
    JRadioButton binningOnButton = new JRadioButton();
    JRadioButton binningOffButton = new JRadioButton();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    Component component1;
    JLabel lensLabel = new JLabel();
    DropDownListBoxWidget lens = new DropDownListBoxWidget();
  JLabel cassRotatorLabel = new JLabel();
  JRadioButton cassRotatorFollowingButton = new JRadioButton();
  JRadioButton cassRotatorFixedButton = new JRadioButton();
  ButtonGroup cassRotatorButtonGroup = new ButtonGroup();

    public AcqCamGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), "Filter");


        fill = Box.createGlue();
        component1 = Box.createVerticalStrut(8);
        this.setMinimumSize(new Dimension(350, 378));
        this.setPreferredSize(new Dimension(350, 378));
        this.setLayout(gridBagLayout1);


        expTimeLabel.setToolTipText("Exposure time in seconds");
        expTimeLabel.setLabelFor(exposureTime);
        expTimeLabel.setText("Exposure Time");
        exposureTime.setAllowNegative(false);
        exposureTime.setToolTipText("Enter the exposure time in seconds");

        disperserLabel.setLabelFor(ndFilter);
        disperserLabel.setText("Neutral Density Filter");


        posAngleLabel.setToolTipText("Position angle in degrees E of N");
        posAngleLabel.setLabelFor(posAngle);
        posAngleLabel.setText("Position Angle");


        posAngle.setToolTipText("Enter the position angle in degrees E of N");
        expTimeUnits.setText("sec");
        posAngleUnits.setText("deg E of N");
        colorFilterLabel.setToolTipText("");
        colorFilterLabel.setLabelFor(colorFilter);
        colorFilterLabel.setText("Color Filter");
        colorFilter.setToolTipText("Select the Color Filter");
        ndFilter.setToolTipText("Select the Neutral Density Filter");
        windowingLabel.setLabelFor(windowingPanel);
        windowingLabel.setText("Windowing");
        xLabel.setLabelFor(x);
        xLabel.setText("X Start");
        yLabel.setLabelFor(y);
        yLabel.setText("Y Start");
        widthLabel.setToolTipText("");
    widthLabel.setLabelFor(width);
        widthLabel.setText("X Size");
        heightLabel.setLabelFor(height);
        heightLabel.setText("Y Size");
        windowingOffButton.setToolTipText("Turn off Windowing");
        windowingOffButton.setSelected(true);
        windowingOffButton.setText("Off");
        windowingOnButton.setToolTipText("Turn on Windowing");
        windowingOnButton.setText("On");
        xComment.setToolTipText("");
        xComment.setText("Origin of window in pixels");
        widthComment.setText("Size of window in pixels");
        x.setToolTipText(" X coordinate of the origin of the window in detector pixels");
        y.setToolTipText(" Y coordinate of the origin of the window in detector pixels");
        width.setToolTipText("Width of the window in detector pixels");
        height.setToolTipText("Height of the window in detector pixels");
        roiPanel.setToolTipText("Regions of Interest");
        roiPanel.setLayout(gridBagLayout3);
        binningOnButton.setToolTipText("Turn on 2x2 Binning");
        binningOnButton.setText("2x2 Binning");
        binningOffButton.setToolTipText("Turn off Binning");
        binningOffButton.setSelected(true);
        binningOffButton.setText("No  Binning (default)");
        binningPanel.setLayout(gridBagLayout2);
        lensLabel.setText("Lens");
        cassRotatorLabel.setText("Cass Rotator");
    cassRotatorFollowingButton.setText("Following");
    cassRotatorFixedButton.setText("Fixed");
    roiPanel.add(windowingLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 1.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        roiPanel.add(xLabel,  new GridBagConstraints(0, 6, 1, 1, 0.0, 1.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 33, 0, 0), 0, 0));
        roiPanel.add(yLabel,  new GridBagConstraints(0, 7, 1, 1, 0.0, 1.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 33, 0, 0), 0, 0));
        roiPanel.add(widthLabel,  new GridBagConstraints(0, 8, 1, 1, 0.0, 1.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 33, 0, 0), 0, 0));
        roiPanel.add(heightLabel,  new GridBagConstraints(0, 9, 1, 1, 0.0, 1.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 33, 0, 0), 0, 0));
        roiPanel.add(windowingPanel, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        windowingPanel.add(windowingOnButton, null);
        windowingPanel.add(windowingOffButton, null);
        windowingButtonGroup.add(windowingOnButton);
        windowingButtonGroup.add(windowingOffButton);
        roiPanel.add(x, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
                                               , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(y, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0
                                               , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(width, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(height, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0
                                                    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(xComment, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 6, 0, 0), 0, 0));
        roiPanel.add(fill, new GridBagConstraints(2, 10, 1, 1, 1.0, 1.0
                                                  , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        roiPanel.add(widthComment, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0
                                                          , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 6, 0, 0), 0, 0));
    this.add(cassRotatorLabel,  new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
    this.add(cassRotatorFollowingButton,   new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
    this.add(cassRotatorFixedButton,     new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jTabbedPane1.add(binningPanel, "Binning");
        jTabbedPane1.add(roiPanel, "Regions of Interest");
        binningPanel.add(binningOnButton, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
                                                                 , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 0), 0, 0));
        binningPanel.add(binningOffButton, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                                                                  , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 0), 0, 0));
        binningPanel.add(component1, new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0
                                                            , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        binningButtonGroup.add(binningOffButton);
        binningButtonGroup.add(binningOnButton);
        this.add(expTimeLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));
        this.add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));
        this.add(disperserLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));
        this.add(ndFilter, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                                                  , GridBagConstraints.WEST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0, 11, 0, 0), 0, 0));
        this.add(posAngleLabel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));
        this.add(posAngle, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                                                  , GridBagConstraints.WEST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0, 11, 0, 0), 0, 0));
        this.add(expTimeUnits,  new GridBagConstraints(2, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 11), 0, 0));
        this.add(posAngleUnits,  new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        this.add(colorFilterLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));
        this.add(colorFilter, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));
        this.add(lensLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));
        this.add(lens, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
                                              , GridBagConstraints.WEST,
                                              GridBagConstraints.HORIZONTAL,
                                              new Insets(0, 11, 0, 0), 0, 0));
        this.add(jTabbedPane1,  new GridBagConstraints(0, 10, 4, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 6, 6), 0, 0));
    cassRotatorButtonGroup.add(cassRotatorFollowingButton);
    cassRotatorButtonGroup.add(cassRotatorFixedButton);
    }
}

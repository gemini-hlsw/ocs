package jsky.app.ot.gemini.gnirs;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import jsky.app.ot.gemini.parallacticangle.PositionAnglePanel;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.SingleSelectComboBox;

import javax.swing.*;
import java.awt.*;

public class GnirsForm extends JPanel {
	public GnirsForm() {
		initComponents();
	}

	private void initComponents() {
		DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
		JPanel top1 = new JPanel();
		JLabel pixelScaleLabel = new JLabel();
		JLabel slitWidthLabel = new JLabel();
		pixelScale = new SingleSelectComboBox();
		JLabel disperserLabel = new JLabel();
		disperser = new SingleSelectComboBox();
		slitWidth = new SingleSelectComboBox<>();
		JPanel wollastonPrismPanel = new JPanel();
		JLabel component2 = new JLabel();
		JLabel scienceFovLabel = new JLabel();
		scienceFOV = new JLabel();
		JComponent goodiesFormsSeparator1 = compFactory.createSeparator(" ");
		centralWavelengthLabel = new JLabel();
		JLabel crossDispersedLabel = new JLabel();
		centralWavelength = new DropDownListBoxWidget();
		JLabel centralWavelengthUnitsLabel = new JLabel();
		crossDispersed = new SingleSelectComboBox();
		JComponent goodiesFormsSeparator2 = compFactory.createSeparator(" ");
		JLabel exposureTimeLabel = new JLabel();
		JLabel coaddsLabel = new JLabel();
		JLabel wellLabel = new JLabel();
		JLabel biasLevelLabel = new JLabel();
		exposureTime = new NumberBoxWidget();
		JLabel jLabel3 = new JLabel();
		coadds = new NumberBoxWidget();
		JLabel coaddsUnitsLabel = new JLabel();
		well = new SingleSelectComboBox();
		biasLevel = new JLabel();

        JComponent goodiesFormsSeparator3 = compFactory.createSeparator("Position Angle");
        posAnglePanel = PositionAnglePanel.apply(SPComponentType.INSTRUMENT_GNIRS);

		tabbedPane = new JTabbedPane();
		readModeTab = new JPanel();
		readModeBrightRadioButton = new JRadioButton();
		readModeFaintRadioButton = new JRadioButton();
		readModeVeryFaintRadioButton = new JRadioButton();
		readModeVeryBrightRadioButton = new JRadioButton();
		lowNoiseReadsLabel = new JLabel();
		minExpTimeLabel = new JLabel();
		minExpTime = new JLabel();
		lowNoiseReads = new JLabel();
		readNoiseLabel = new JLabel();
		readNoise = new JLabel();
		JPanel crossDispersedTab = new JPanel();
		JLabel crossDispersedCentralWavelengthsLabel = new JLabel();
		JLabel component1 = new JLabel();
		JScrollPane orderTableScrollPane = new JScrollPane();
		orderTable = new JTable();
		portTab = new JPanel();

		//======== this ========
		setMinimumSize(new Dimension(400, 453));
		setPreferredSize(new Dimension(400, 453));
		setToolTipText("");
		setLayout(new GridBagLayout());

		//======== top1 ========
		{
			top1.setLayout(new GridBagLayout());

			//---- pixelScaleLabel ----
			pixelScaleLabel.setLabelFor(null);
			pixelScaleLabel.setText("Pixel Scale");
			top1.add(pixelScaleLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- slitWidthLabel ----
			slitWidthLabel.setText("Focal Plane Unit");
			top1.add(slitWidthLabel, new GridBagConstraints(2, 0, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			// TODO: Add Filter and AcqMirror to GNIRS instrument component
	/*		//---- filterLabel ----
			filterLabel.setLabelFor(null);
			filterLabel.setText("Filter");
			top1.add(filterLabel, new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));

			//---- filter ----
			filter.setFont(new Font("Dialog", Font.PLAIN, 12));
			top1.add(filter, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(0, 11, 0, 0), 0, 0));

			//---- aqcMirrorLabel ----
			acqMirrorLabel.setText("Acquisition Mirror");
			top1.add(acqMirrorLabel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));

			//---- acqMirror ----
			acqMirror.setMaximumRowCount(2);
			top1.add(acqMirror, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(11, 11, 0, 0), 0, 0)); */

            // TODO: REMOVE
			//---- posAngleLabel ----
			//posAngleLabel.setText("Position Angle");
			//top1.add(posAngleLabel, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
			//	GridBagConstraints.WEST, GridBagConstraints.NONE,
			//	new Insets(11, 11, 0, 0), 0, 0));

			//---- pixelScale ----
			pixelScale.setFont(new Font("Dialog", Font.PLAIN, 12));
			top1.add(pixelScale, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));

			//---- disperserLabel ----
			disperserLabel.setLabelFor(null);
			disperserLabel.setText("Disperser");
			top1.add(disperserLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- disperser ----
			disperser.setFont(new Font("Dialog", Font.PLAIN, 12));
			top1.add(disperser, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));

			//---- slitWidth ----
			slitWidth.setMaximumRowCount(12);
			top1.add(slitWidth, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));

            // TODO: REMOVE
			//---- posAngle ----
			//posAngle.setToolTipText("Set the position angle in degrees east of north");
			//posAngle.setText("");
			//top1.add(posAngle, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
			//	GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			//	new Insets(0, 11, 0, 0), 0, 0));

			//---- posAngleUnitsLabel ----
			//posAngleUnitsLabel.setText("deg E of N");
			//top1.add(posAngleUnitsLabel, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0,
			//	GridBagConstraints.WEST, GridBagConstraints.NONE,
			//	new Insets(0, 6, 0, 0), 0, 0));

			//======== wollastonPrismPanel ========
			{
				wollastonPrismPanel.setLayout(new GridBagLayout());
			}
			top1.add(wollastonPrismPanel, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
			top1.add(component2, new GridBagConstraints(6, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

			//---- scienceFovLabel ----
			scienceFovLabel.setText("Science FOV");
			top1.add(scienceFovLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- scienceFOV ----
			scienceFOV.setRequestFocusEnabled(true);
			scienceFOV.setToolTipText("Science FOV: Calculated value, from slit width+pixel scale+IFU/XD/Woll");
			scienceFOV.setText("000.000 arcsecs");
			top1.add(scienceFOV, new GridBagConstraints(0, 3, 7, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 11, 0, 0), 0, 0));
			top1.add(goodiesFormsSeparator1, new GridBagConstraints(0, 4, 6, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));

			//---- centralWavelengthLabel ----
			centralWavelengthLabel.setToolTipText("");
			centralWavelengthLabel.setText("Central Wavelength");
			top1.add(centralWavelengthLabel, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- crossDispersedLabel ----
			crossDispersedLabel.setText("Cross-dispersed");
			top1.add(crossDispersedLabel, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- centralWavelength ----
			centralWavelength.setPreferredSize(new Dimension(110, 25));
			centralWavelength.setToolTipText("Central Wavelength (Menu shows default for each band");
			centralWavelength.setEditable(true);
			top1.add(centralWavelength, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));

			//---- centralWavelengthUnitsLabel ----
			centralWavelengthUnitsLabel.setRequestFocusEnabled(true);
			centralWavelengthUnitsLabel.setText("um");
			top1.add(centralWavelengthUnitsLabel, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));

			//---- crossDispersed ----
			crossDispersed.setMaximumRowCount(12);
			top1.add(crossDispersed, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			top1.add(goodiesFormsSeparator2, new GridBagConstraints(0, 7, 6, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));

			//---- exposureTimeLabel ----
			exposureTimeLabel.setLabelFor(null);
			exposureTimeLabel.setText("Exposure Time");
			top1.add(exposureTimeLabel, new GridBagConstraints(0, 8, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- coaddsLabel ----
			coaddsLabel.setText("Coadds");
			top1.add(coaddsLabel, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- wellLabel ----
			wellLabel.setText("Well Depth");
			top1.add(wellLabel, new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- biasLevelLabel ----
			biasLevelLabel.setText("Bias Level");
			top1.add(biasLevelLabel, new GridBagConstraints(5, 8, 1, 1, 0.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));

			//---- exposureTime ----
			exposureTime.setMaximumSize(new Dimension(1000, 1000));
			exposureTime.setToolTipText("Enter the exposure time in seconds");
			top1.add(exposureTime, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));

			//---- jLabel3 ----
			jLabel3.setText("sec");
			top1.add(jLabel3, new GridBagConstraints(1, 9, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));

			//---- coadds ----
			coadds.setText("");
			top1.add(coadds, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));

			//---- coaddsUnitsLabel ----
			coaddsUnitsLabel.setText("exp/obs");
			top1.add(coaddsUnitsLabel, new GridBagConstraints(3, 9, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, -2));

			//---- well ----
			well.setFont(new Font("Dialog", Font.PLAIN, 12));
			top1.add(well, new GridBagConstraints(4, 9, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));

			//---- biasLevel ----
			biasLevel.setText("0 mV");
			top1.add(biasLevel, new GridBagConstraints(5, 9, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 11, 0, 0), 0, 0));


            //----- Position Angle -----
            //---- posAngle ----
            top1.add(goodiesFormsSeparator3, new GridBagConstraints(0, 10, 6, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(10, 0, 0, 0), 0, 0));
            top1.add(posAnglePanel.peer(), new GridBagConstraints(0, 11, 2, 2, 0.0, 0.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 5, 0, 0), 0, 0));

		}
		add(top1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));

		//======== tabbedPane ========
		{

			//======== readModeTab ========
			{
                readModeTab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				readModeTab.setLayout(new GridBagLayout());

				//---- readModeBrightRadioButton ----
				readModeBrightRadioButton.setText("Bright Objects");
				readModeTab.add(readModeBrightRadioButton, new GridBagConstraints() {{
                    gridwidth = 2;
                    weightx = 1.0;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(0, 0, 10, 0);
                }});

				//---- readModeFaintRadioButton ----
				readModeFaintRadioButton.setText("Faint Objects");
				readModeTab.add(readModeFaintRadioButton, new GridBagConstraints() {{
                    gridy = 1;
                    gridwidth = 2;
                    weightx = 1.0;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(0, 0, 10, 0);
                }});

                //---- readModeVeryFaintRadioButton ----
                readModeVeryFaintRadioButton.setText("Very Faint Objects");
				readModeTab.add(readModeVeryFaintRadioButton, new GridBagConstraints() {{
                    gridy = 2;
                    gridwidth = 2;
                    weightx = 1.0;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(0, 0, 10, 0);
                }});

				//---- readModeVeryBrightRadioButton ----
				readModeVeryBrightRadioButton.setMinimumSize(new Dimension(200, 40));
				readModeVeryBrightRadioButton.setText("Very Bright Objects/Acquisition/High (thermal) Background");
				readModeTab.add(readModeVeryBrightRadioButton, new GridBagConstraints() {{
                    gridy = 3;
                    gridwidth = 2;
                    weightx = 1.0;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(0, 0, 10, 0);
                }});

                //---- space filler ----
                readModeTab.add(new JPanel(), new GridBagConstraints() {{
                    gridy = 4;
                    gridwidth = 4;
                    weighty = 1.0;
                    fill = GridBagConstraints.BOTH;
                }});

                //---- minExpTimeLabel ----
				minExpTimeLabel.setText("Min exposure time:");
				readModeTab.add(minExpTimeLabel, new GridBagConstraints() {{
                    gridx = 2;
                    gridy = 5;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(10, 10, 0, 0);
                }});

				//---- minExpTime ----
				minExpTime.setPreferredSize(new Dimension(80, 16));
				minExpTime.setToolTipText("");
				minExpTime.setText("0.00 sec");
				readModeTab.add(minExpTime, new GridBagConstraints() {{
                    gridx = 3;
                    gridy = 5;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(10, 10, 0, 0);
                }});

                //---- lowNoiseReadsLabel ----
                lowNoiseReadsLabel.setToolTipText("");
                lowNoiseReadsLabel.setText("Low Noise Reads:");
                readModeTab.add(lowNoiseReadsLabel, new GridBagConstraints() {{
                    gridy = 6;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(10, 0, 0, 0);
                }});

                //---- lowNoiseReads ----
                lowNoiseReads.setRequestFocusEnabled(true);
                lowNoiseReads.setText("0");
                readModeTab.add(lowNoiseReads, new GridBagConstraints() {{
                    gridx = 1;
                    gridy = 6;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(10, 10, 0, 0);
                }});

				//---- readNoiseLabel ----
				readNoiseLabel.setText("Read Noise:");
				readModeTab.add(readNoiseLabel, new GridBagConstraints() {{
                    gridx = 2;
                    gridy = 6;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(10, 10, 0, 0);
                }});

				//---- readNoise ----
				readNoise.setText("--");
				readModeTab.add(readNoise, new GridBagConstraints() {{
                    gridx = 3;
                    gridy = 6;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(10, 10, 0, 0);
                }});
			}
			tabbedPane.addTab("Read Mode", readModeTab);


			//======== crossDispersedTab ========
			{
				crossDispersedTab.setLayout(new GridBagLayout());

				//---- crossDispersedCentralWavelengthsLabel ----
				crossDispersedCentralWavelengthsLabel.setText("Central Wavelengths:");
				crossDispersedTab.add(crossDispersedCentralWavelengthsLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
					GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));
				crossDispersedTab.add(component1, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));

				//======== orderTableScrollPane ========
				{

					//---- orderTable ----
					orderTable.setBackground(Color.white);
					orderTable.setToolTipText("");
					orderTable.setShowHorizontalLines(false);
					orderTableScrollPane.setViewportView(orderTable);
				}
				crossDispersedTab.add(orderTableScrollPane, new GridBagConstraints(1, 2, 2, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 11, 11, 11), 0, 0));
			}
			tabbedPane.addTab("Cross-dispersed", crossDispersedTab);


			//======== portTab ========
			{
                portTab.setLayout(new GridBagLayout() {{
                    columnWidths  = new int[] {0};
                    columnWeights = new double[] {1.0e-4};
                    rowHeights    = new int[] {0};
                    rowWeights    = new double[] {1.0e-4};
                }});
			}
			tabbedPane.addTab("ISS Port", portTab);

		}
		add(tabbedPane, new GridBagConstraints(0, 1, 1, 2, 0.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(11, 6, 0, 6), 0, -9));

		//---- readModeButtonGroup ----
		ButtonGroup readModeButtonGroup = new ButtonGroup();
		readModeButtonGroup.add(readModeBrightRadioButton);
		readModeButtonGroup.add(readModeFaintRadioButton);
		readModeButtonGroup.add(readModeVeryFaintRadioButton);
		readModeButtonGroup.add(readModeVeryBrightRadioButton);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	SingleSelectComboBox pixelScale;
	SingleSelectComboBox disperser;
	SingleSelectComboBox<GNIRSParams.SlitWidth> slitWidth;
//	SingleSelectComboBox filter;
//	SingleSelectComboBox acqMirror;
	JLabel scienceFOV;
	JLabel centralWavelengthLabel;
	DropDownListBoxWidget centralWavelength;
	SingleSelectComboBox crossDispersed;
	NumberBoxWidget exposureTime;
	NumberBoxWidget coadds;
	SingleSelectComboBox well;
	JLabel biasLevel;
	JTabbedPane tabbedPane;
	JPanel readModeTab;
	JRadioButton readModeBrightRadioButton;
	JRadioButton readModeFaintRadioButton;
	JRadioButton readModeVeryFaintRadioButton;
	JRadioButton readModeVeryBrightRadioButton;
	JLabel lowNoiseReadsLabel;
	JLabel minExpTimeLabel;
	JLabel minExpTime;
	JLabel lowNoiseReads;
	JLabel readNoiseLabel;
	JLabel readNoise;
	JTable orderTable;
	JPanel portTab;
	// JFormDesigner - End of variables declaration  //GEN-END:variables

    PositionAnglePanel<InstGNIRS, EdCompInstGNIRS> posAnglePanel;
}

package jsky.app.ot.gemini.trecs;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;

import javax.swing.*;

public class EngTReCSForm extends JPanel {
	public EngTReCSForm() {
		initComponents();
	}

	private void initComponents() {
		JLabel sectorWheelLabel = new JLabel();
		sectorWheelComboBox = new JComboBox();
		JLabel lyotWheelLabel = new JLabel();
		lyotWheelComboBox = new JComboBox();
		JLabel pupilImagingWheelLabel = new JLabel();
		pupilImagingWheelComboBox = new JComboBox();
		JLabel apertureWheelLabel = new JLabel();
		apertureWheelComboBox = new JComboBox();
		JLabel wellDepthLabel = new JLabel();
		wellDepthComboBox = new JComboBox();
		JLabel frameTimeLabel = new JLabel();
		frameTimeComboBox = new JComboBox();
		JLabel chopFrequencyLabel = new JLabel();
		chopFrequencyComboBox = new JComboBox();
		JLabel nodHandshakeLabel = new JLabel();
		nodHandShakeComboBox = new JComboBox();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(pref;80dlu)")
			},
			new RowSpec[] {
				new RowSpec(Sizes.DLUY11),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC
			}));

		//---- sectorWheelLabel ----
		sectorWheelLabel.setText("Sector Wheel");
		sectorWheelLabel.setLabelFor(sectorWheelComboBox);
		add(sectorWheelLabel, cc.xy(1, 3));
		add(sectorWheelComboBox, cc.xy(3, 3));

		//---- lyotWheelLabel ----
		lyotWheelLabel.setText("Lyot Wheel");
		lyotWheelLabel.setLabelFor(lyotWheelComboBox);
		add(lyotWheelLabel, cc.xy(1, 5));
		add(lyotWheelComboBox, cc.xy(3, 5));

		//---- pupilImagingWheelLabel ----
		pupilImagingWheelLabel.setText("Pupil Imaging Wheel");
		pupilImagingWheelLabel.setLabelFor(pupilImagingWheelComboBox);
		add(pupilImagingWheelLabel, cc.xy(1, 7));
		add(pupilImagingWheelComboBox, cc.xy(3, 7));

		//---- apertureWheelLabel ----
		apertureWheelLabel.setText("Aperture Wheel");
		apertureWheelLabel.setLabelFor(apertureWheelComboBox);
		add(apertureWheelLabel, cc.xy(1, 9));
		add(apertureWheelComboBox, cc.xy(3, 9));

		//---- wellDepthLabel ----
		wellDepthLabel.setText("Well Depth");
		wellDepthLabel.setLabelFor(wellDepthComboBox);
		add(wellDepthLabel, cc.xy(1, 11));

		//---- wellDepthComboBox ----
		wellDepthComboBox.setMaximumRowCount(3);
		add(wellDepthComboBox, cc.xy(3, 11));

		//---- frameTimeLabel ----
		frameTimeLabel.setText("Frame Time");
		frameTimeLabel.setLabelFor(frameTimeComboBox);
		add(frameTimeLabel, cc.xy(1, 13));

		//---- frameTimeComboBox ----
		frameTimeComboBox.setEditable(true);
		frameTimeComboBox.setModel(new DefaultComboBoxModel(new String[] {
			"auto",
			"25",
			"40",
			"60",
			"80",
			"100"
		}));
		add(frameTimeComboBox, cc.xy(3, 13));

		//---- chopFrequencyLabel ----
		chopFrequencyLabel.setText("Chop Frequency");
		chopFrequencyLabel.setLabelFor(chopFrequencyComboBox);
		add(chopFrequencyLabel, cc.xy(1, 15));

		//---- chopFrequencyComboBox ----
		chopFrequencyComboBox.setEditable(true);
		chopFrequencyComboBox.setModel(new DefaultComboBoxModel(new String[] {
			"auto",
			"1.0",
			"1.5",
			"2.0",
			"2.5",
			"3.0"
		}));
		add(chopFrequencyComboBox, cc.xy(3, 15));

		//---- nodHandshakeLabel ----
		nodHandshakeLabel.setText("Nod Handshake");
		nodHandshakeLabel.setLabelFor(nodHandShakeComboBox);
		add(nodHandshakeLabel, cc.xy(1, 17));

		//---- nodHandShakeComboBox ----
		nodHandShakeComboBox.setEditable(false);
		add(nodHandShakeComboBox, cc.xy(3, 17));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	JComboBox sectorWheelComboBox;
	JComboBox lyotWheelComboBox;
	JComboBox pupilImagingWheelComboBox;
	JComboBox apertureWheelComboBox;
	JComboBox wellDepthComboBox;
	JComboBox frameTimeComboBox;
	JComboBox chopFrequencyComboBox;
	JComboBox nodHandShakeComboBox;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}

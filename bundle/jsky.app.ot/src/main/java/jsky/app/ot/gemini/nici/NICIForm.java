package jsky.app.ot.gemini.nici;

import javax.swing.border.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;
import java.awt.*;

/**
 * $Id: NICIForm.java 19597 2009-05-04 15:36:57Z swalker $
 */
public class NICIForm extends JPanel {
	public NICIForm() {
		initComponents();
	}

	private void initComponents() {
		JLabel label2 = new JLabel();
		JLabel label3 = new JLabel();
		JLabel label4 = new JLabel();
		JPanel panel1 = new JPanel();
		paLabel1 = new JLabel();
		paLabel2 = new JLabel();
		focalPlaneCB = new JComboBox();
		pupilMaskCB = new JComboBox();
		cassRotatorCB = new JComboBox();
		positionAngleTB = new NumberBoxWidget();
		JLabel label7 = new JLabel();
		JLabel label8 = new JLabel();
		JLabel label9 = new JLabel();
		JLabel label10 = new JLabel();
		imagingModeCB = new JComboBox();
		dichroicWheelCB = new JComboBox();
		channel1CB = new JComboBox();
		channel2CB = new JComboBox();
		Detector = new JTabbedPane();
		panel2 = new JPanel();
		JLabel label15 = new JLabel();
		JLabel label11 = new JLabel();
		JLabel label13 = new JLabel();
		exposuresLabel = new JLabel();
		wellDepthCB = new JComboBox();
		exposureTimeTB = new NumberBoxWidget();
		coaddsTB = new NumberBoxWidget();
		exposuresTB = new NumberBoxWidget();
		panel3 = new JPanel();
		portButtonSide = new JRadioButton();
		portButtonUp = new JRadioButton();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setBorder(Borders.DIALOG_BORDER);
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.PREF_COLSPEC,
				new ColumnSpec(ColumnSpec.LEFT, Sizes.bounded(Sizes.PREFERRED, Sizes.pixel(5), Sizes.pixel(20)), FormSpec.DEFAULT_GROW),
				FormFactory.PREF_COLSPEC,
				new ColumnSpec(ColumnSpec.LEFT, Sizes.bounded(Sizes.PREFERRED, Sizes.pixel(5), Sizes.pixel(20)), FormSpec.DEFAULT_GROW),
				FormFactory.PREF_COLSPEC,
				new ColumnSpec(ColumnSpec.LEFT, Sizes.bounded(Sizes.PREFERRED, Sizes.pixel(5), Sizes.pixel(20)), FormSpec.DEFAULT_GROW),
				FormFactory.PREF_COLSPEC,
				ColumnSpec.decode("left:max(pref;5px):grow")
			},
			RowSpec.decodeSpecs("max(pref;20px), fill:pref, top:max(pref;5px), fill:pref, top:max(pref;10px), fill:pref, top:max(pref;5px), fill:pref, top:max(pref;5px), fill:pref, top:max(pref;10px), default, top:max(pref;10px), fill:pref, top:max(pref;5px)")));

		//---- label2 ----
		label2.setText("Focal Plane Mask");
		label2.setVerticalAlignment(SwingConstants.BOTTOM);
		add(label2, cc.xy(2, 2));

		//---- label3 ----
		label3.setText("Pupil Mask");
		label3.setVerticalAlignment(SwingConstants.BOTTOM);
		add(label3, cc.xy(4, 2));

		//---- label4 ----
		label4.setText("Cass Rotator");
		label4.setVerticalAlignment(SwingConstants.BOTTOM);
		add(label4, cc.xy(6, 2));

		//======== panel1 ========
		{
			panel1.setLayout(new GridBagLayout());
			((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0};
			((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0, 0};
			((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
			((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

			//---- paLabel1 ----
			paLabel1.setText("Position Angle");
			paLabel1.setHorizontalAlignment(SwingConstants.LEFT);
			panel1.add(paLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));

			//---- paLabel2 ----
			paLabel2.setText("(degrees E of N)");
			panel1.add(paLabel2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));
		}
		add(panel1, cc.xy(8, 2));
		add(focalPlaneCB, cc.xy(2, 4));
		add(pupilMaskCB, cc.xy(4, 4));
		add(cassRotatorCB, cc.xy(6, 4));
		add(positionAngleTB, cc.xy(8, 4));

		//---- label7 ----
		label7.setText("Imaging Mode");
		add(label7, cc.xy(2, 6));

		//---- label8 ----
		label8.setText("Dichroic Wheel");
		add(label8, cc.xy(4, 6));

		//---- label9 ----
		label9.setText("Filter Red Channel");
		add(label9, cc.xy(6, 6));

		//---- label10 ----
		label10.setText("Filter Blue Channel");
		add(label10, cc.xy(8, 6));
		add(imagingModeCB, cc.xy(2, 8));
		add(dichroicWheelCB, cc.xy(4, 8));
		add(channel1CB, cc.xy(6, 8));
		add(channel2CB, cc.xy(8, 8));

		//======== Detector ========
		{

			//======== panel2 ========
			{
				panel2.setBorder(new EmptyBorder(10, 10, 10, 10));
				panel2.setLayout(new FormLayout(
					ColumnSpec.decodeSpecs("default, left:10dlu, default, left:10dlu, default, left:10dlu, default"),
					new RowSpec[] {
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.LINE_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC
					}));

				//---- label15 ----
				label15.setText("Well Depth");
				panel2.add(label15, cc.xy(1, 1));

				//---- label11 ----
				label11.setText("Exposure Time (sec)");
				panel2.add(label11, cc.xy(3, 1));

				//---- label13 ----
				label13.setText("No. Coadds");
				panel2.add(label13, cc.xy(5, 1));

				//---- exposuresLabel ----
				exposuresLabel.setText("No. Exposures");
				panel2.add(exposuresLabel, cc.xy(7, 1));
				panel2.add(wellDepthCB, cc.xy(1, 3));
				panel2.add(exposureTimeTB, cc.xy(3, 3));
				panel2.add(coaddsTB, cc.xy(5, 3));
				panel2.add(exposuresTB, cc.xy(7, 3));
			}
			Detector.addTab("Detector", panel2);


			//======== panel3 ========
			{
				panel3.setBorder(new EmptyBorder(10, 10, 10, 10));
				panel3.setLayout(new FormLayout(
					ColumnSpec.decodeSpecs("default"),
					new RowSpec[] {
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.LINE_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC
					}));

				//---- portButtonSide ----
				portButtonSide.setText("Side-looking");
				panel3.add(portButtonSide, cc.xy(1, 1));

				//---- portButtonUp ----
				portButtonUp.setText("Up-looking");
				panel3.add(portButtonUp, cc.xy(1, 3));
			}
			Detector.addTab("ISS Port", panel3);

		}
		add(Detector, cc.xywh(2, 12, 7, 1));

		//---- buttonGroup1 ----
		ButtonGroup buttonGroup1 = new ButtonGroup();
		buttonGroup1.add(portButtonSide);
		buttonGroup1.add(portButtonUp);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	public JLabel getPaLabel1() {
		return paLabel1;
	}

	public JLabel getPaLabel2() {
		return paLabel2;
	}

	public JComboBox getFocalPlaneCB() {
		return focalPlaneCB;
	}

	public JComboBox getPupilMaskCB() {
		return pupilMaskCB;
	}

	public JComboBox getCassRotatorCB() {
		return cassRotatorCB;
	}

	public NumberBoxWidget getPositionAngleTB() {
		return positionAngleTB;
	}

	public JComboBox getImagingModeCB() {
		return imagingModeCB;
	}

	public JComboBox getDichroicWheelCB() {
		return dichroicWheelCB;
	}

	public JComboBox getChannel1CB() {
		return channel1CB;
	}

	public JComboBox getChannel2CB() {
		return channel2CB;
	}

	public NumberBoxWidget getExposureTimeTB() {
		return exposureTimeTB;
	}

	public NumberBoxWidget getCoaddsTB() {
		return coaddsTB;
	}

	public NumberBoxWidget getExposuresTB() {
		return exposuresTB;
	}

	public JLabel getExposuresLabel() {
		return exposuresLabel;
	}

	public JComboBox getWellDepthCB() {
		return wellDepthCB;
	}

	public JRadioButton getPortButtonSide() {
		return portButtonSide;
	}

	public JRadioButton getPortButtonUp() {
		return portButtonUp;
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel paLabel1;
	private JLabel paLabel2;
	private JComboBox focalPlaneCB;
	private JComboBox pupilMaskCB;
	private JComboBox cassRotatorCB;
	private NumberBoxWidget positionAngleTB;
	private JComboBox imagingModeCB;
	private JComboBox dichroicWheelCB;
	private JComboBox channel1CB;
	private JComboBox channel2CB;
	private JTabbedPane Detector;
	private JPanel panel2;
	private JLabel exposuresLabel;
	private JComboBox wellDepthCB;
	private NumberBoxWidget exposureTimeTB;
	private NumberBoxWidget coaddsTB;
	private NumberBoxWidget exposuresTB;
	private JPanel panel3;
	private JRadioButton portButtonSide;
	private JRadioButton portButtonUp;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}

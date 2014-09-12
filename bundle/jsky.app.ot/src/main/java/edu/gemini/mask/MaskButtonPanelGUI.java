package edu.gemini.mask;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
/*
 * Created by JFormDesigner on Tue Jul 26 20:55:45 CEST 2005
 */



/**
 * @author User #1
 */
public class MaskButtonPanelGUI extends JPanel {
	public MaskButtonPanelGUI() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		label1 = new JLabel();
		slitSizeY = new NumberBoxWidget();
		label2 = new JLabel();
		slitSizeX = new NumberBoxWidget();
		label3 = new JLabel();
		slitTilt = new NumberBoxWidget();
		label4 = new JLabel();
		slitPosY = new NumberBoxWidget();
		label5 = new JLabel();
		slitPosX = new NumberBoxWidget();
		p0Button = new JButton();
		p1Button = new JButton();
		p2Button = new JButton();
		p3Button = new JButton();
		pXButton = new JButton();
		designMaskButton = new JButton();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setBorder(new EtchedBorder());
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(default;25dlu)"),
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(default;25dlu)"),
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(default;25dlu)"),
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(default;25dlu)"),
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(default;25dlu)"),
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(Sizes.dluX(0))
			},
			new RowSpec[] {
				new RowSpec(Sizes.dluY(0)),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				new RowSpec(Sizes.dluY(0))
			}));

		//---- label1 ----
		label1.setText("Slit Size Y");
		add(label1, cc.xy(3, 3));

		//---- slitSizeY ----
		slitSizeY.setToolTipText("Set the slit size for the selected table rows in arcsec");
		add(slitSizeY, cc.xy(5, 3));

		//---- label2 ----
		label2.setText("Slit Size X");
		add(label2, cc.xy(7, 3));

		//---- slitSizeX ----
		slitSizeX.setToolTipText("Set the slit size for the selected table rows in arcsec");
		add(slitSizeX, cc.xy(9, 3));

		//---- label3 ----
		label3.setText("Slit Tilt");
		add(label3, cc.xy(11, 3));

		//---- slitTilt ----
		slitTilt.setToolTipText("Set the slit tilt angle in degrees for the selected table rows");
		add(slitTilt, cc.xy(13, 3));

		//---- label4 ----
		label4.setText("Slit Pos Y");
		add(label4, cc.xy(15, 3));

		//---- slitPosY ----
		slitPosY.setToolTipText("Set the slit position for the selected table rows in arcsec");
		add(slitPosY, cc.xy(17, 3));

		//---- label5 ----
		label5.setText("Slit Pos X");
		add(label5, cc.xy(19, 3));

		//---- slitPosX ----
		slitPosX.setToolTipText("Set the slit position for the selected table rows in arcsec");
		add(slitPosX, cc.xy(21, 3));

		//---- p0Button ----
		p0Button.setText("Priority 0");
		p0Button.setToolTipText("Set the priority to 0 (acquisition objects) for the selected table rows");
		add(p0Button, cc.xywh(3, 5, 3, 1));

		//---- p1Button ----
		p1Button.setText("Priority 1");
		p1Button.setToolTipText("Set the priority to 1 (most important) for the selected table rows");
		add(p1Button, cc.xywh(7, 5, 3, 1));

		//---- p2Button ----
		p2Button.setText("Priority 2");
		p2Button.setToolTipText("Set the priority to 2 (second most important) for the selected table rows");
		add(p2Button, cc.xywh(11, 5, 3, 1));

		//---- p3Button ----
		p3Button.setText("Priority 3");
		p3Button.setToolTipText("Set the priority to 3 (command objects) for the selected table rows");
		add(p3Button, cc.xywh(15, 5, 3, 1));

		//---- pXButton ----
		pXButton.setText("Ignore");
		pXButton.setToolTipText("Set the priority to X (ignored objects) for the selected table rows");
		add(pXButton, cc.xywh(19, 5, 3, 1));

		//---- designMaskButton ----
		designMaskButton.setText("Design Mask...");
		designMaskButton.setToolTipText("Open the slit positioning and optimization panel");
		add(designMaskButton, cc.xy(25, 5));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JLabel label1;
	NumberBoxWidget slitSizeY;
	private JLabel label2;
	NumberBoxWidget slitSizeX;
	private JLabel label3;
	NumberBoxWidget slitTilt;
	private JLabel label4;
	NumberBoxWidget slitPosY;
	private JLabel label5;
	NumberBoxWidget slitPosX;
	JButton p0Button;
	JButton p1Button;
	JButton p2Button;
	JButton p3Button;
	JButton pXButton;
	JButton designMaskButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}

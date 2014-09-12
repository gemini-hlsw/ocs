package jsky.app.ot.gemini.altair;

import jsky.util.gui.CheckBoxWidget;
import jsky.util.gui.OptionWidget;

import javax.swing.*;
import java.awt.*;
/**
 * @author User #1
 */
public class AltairForm extends JPanel {
	public AltairForm() {
		initComponents();
	}

	private void initComponents() {
		adcLabel = new JLabel();
		beamSplitterLabel = new JLabel();
		adcCheck = new CheckBoxWidget();
		eight50Button = new OptionWidget();
		oneButton = new OptionWidget();
		cassRotatorLabel = new JLabel();
		cassRotatorFollowingButton = new JRadioButton();
		cassRotatorFixedButton = new JRadioButton();
		ndFilterLabel = new JLabel();
		ndFilterInButton = new JRadioButton();
		ndFilterOutButton = new JRadioButton();
		guideStarType = new JLabel();
		ngsRadioButton = new JRadioButton();
        ngsWithFieldLensRadioButton = new JRadioButton();
        lgsRadioButton = new JRadioButton();
        lgsP1RadioButton = new JRadioButton();
        lgsOiRadioButton = new JRadioButton();
        withFlLabel1 = new JLabel();
        withFlLabel2 = new JLabel();
        withFlLabel3 = new JLabel();

        //======== this ========
		setMinimumSize(new Dimension(400, 453));
		setPreferredSize(new Dimension(400, 453));
		setToolTipText("");
		setLayout(new GridBagLayout());

		//---- adcLabel ----
		adcLabel.setToolTipText("");
		adcLabel.setText("Atmospheric Dispersion Corrector");
		add(adcLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- adcCheck ----
		adcCheck.setSelected(true);
		adcCheck.setText("On");
		add(adcCheck, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

        //---- beamSplitterLabel ----
        beamSplitterLabel.setToolTipText("");
        beamSplitterLabel.setText("Dichroic Beamsplitter");
        add(beamSplitterLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- eight50Button ----
		eight50Button.setText("850 nm");
		add(eight50Button, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

        //---- oneButton ----
		oneButton.setText("1 um");
		add(oneButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 11, 0, 0), 0, 0));

        // This setting is no longer supported.
        oneButton.setEnabled(false);

		//---- cassRotatorLabel ----
		cassRotatorLabel.setToolTipText("");
		cassRotatorLabel.setVerifyInputWhenFocusTarget(true);
		cassRotatorLabel.setText("Cass Rotator");
		add(cassRotatorLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
			GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- cassRotatorFollowingButton ----
		cassRotatorFollowingButton.setText("Following");
		add(cassRotatorFollowingButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- cassRotatorFixedButton ----
		cassRotatorFixedButton.setText("Fixed");
		add(cassRotatorFixedButton, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- ndFilterLabel ----
		ndFilterLabel.setText("ND Filter");
		add(ndFilterLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- ndFilterInButton ----
		ndFilterInButton.setText("In");
		add(ndFilterInButton, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- ndFilterOutButton ----
		ndFilterOutButton.setText("Out");
		add(ndFilterOutButton, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- guideStarType ----
		guideStarType.setText("Guide Star Type");
		add(guideStarType, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
			GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- ngsRadioButton ----
		ngsRadioButton.setText("Natural Guide Star");
		add(ngsRadioButton, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

        //---- ngsWithFieldLensRadioButton ----
        ngsWithFieldLensRadioButton.setText("Natural Guide Star with Field Lens");
        add(ngsWithFieldLensRadioButton, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 11, 0, 0), 0, 0));

        //---- lgsRadioButton ----
		lgsRadioButton.setText("Laser Guide Star + AOWFS");
		add(lgsRadioButton, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 11, 0, 0), 0, 0));
        withFlLabel1.setText("(with Field Lens)");
        add(withFlLabel1, new GridBagConstraints(2, 9, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 11, 0, 0), 0, 0));

        //---- lgsP1RadioButton ----
        lgsP1RadioButton.setText("Laser Guide Star + PWFS1");
        add(lgsP1RadioButton, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 11, 0, 0), 0, 0));
        withFlLabel2.setText("(with Field Lens)");
        add(withFlLabel2, new GridBagConstraints(2, 10, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- lgsP1RadioButton ----
        // LAYOUT NOTE: bring both elements to the top left and let the last element
        // consume all remaining space (fill weight = 1.0 for x and y)
        lgsOiRadioButton.setText("Laser Guide Star + OIWFS");
        add(lgsOiRadioButton, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
            new Insets(0, 11, 0, 0), 0, 0));
        withFlLabel3.setText("(with Field Lens)");
        add(withFlLabel3, new GridBagConstraints(2, 11, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- beamSplitterButtonGroup ----
		ButtonGroup beamSplitterButtonGroup = new ButtonGroup();
		beamSplitterButtonGroup.add(eight50Button);
		beamSplitterButtonGroup.add(oneButton);

		//---- cassRotatorButtonGroup ----
		ButtonGroup cassRotatorButtonGroup = new ButtonGroup();
		cassRotatorButtonGroup.add(cassRotatorFollowingButton);
		cassRotatorButtonGroup.add(cassRotatorFixedButton);

		//---- ndFilterGroup ----
		ButtonGroup ndFilterGroup = new ButtonGroup();
		ndFilterGroup.add(ndFilterInButton);
		ndFilterGroup.add(ndFilterOutButton);

		//---- altairModeGroup ----
		ButtonGroup altairModeGroup = new ButtonGroup();
		altairModeGroup.add(ngsRadioButton);
        altairModeGroup.add(ngsWithFieldLensRadioButton);
        altairModeGroup.add(lgsRadioButton);
        altairModeGroup.add(lgsP1RadioButton);
        altairModeGroup.add(lgsOiRadioButton);

	}

	private JLabel adcLabel;
	private JLabel beamSplitterLabel;
	CheckBoxWidget adcCheck;
	OptionWidget eight50Button;
	OptionWidget oneButton;
	private JLabel cassRotatorLabel;
	JRadioButton cassRotatorFollowingButton;
	JRadioButton cassRotatorFixedButton;
	private JLabel ndFilterLabel;
	JRadioButton ndFilterInButton;
	JRadioButton ndFilterOutButton;
	private JLabel guideStarType;
	JRadioButton ngsRadioButton;
    JRadioButton ngsWithFieldLensRadioButton;
    JRadioButton lgsRadioButton;
    JRadioButton lgsP1RadioButton;
    JRadioButton lgsOiRadioButton;
	private JLabel horizontalFiller;
    private JLabel withFlLabel1;
    private JLabel withFlLabel2;
    private JLabel withFlLabel3;
}

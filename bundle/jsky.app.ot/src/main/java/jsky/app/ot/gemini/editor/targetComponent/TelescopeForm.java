package jsky.app.ot.gemini.editor.targetComponent;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.shared.gui.calendar.JCalendarPopup;
import jsky.app.ot.ags.AgsSelectorControl;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Date;
import java.util.TimeZone;
/*
 * Created by JFormDesigner on Wed Nov 02 16:41:11 CET 2005
 */



/**
 * @author User #1
 */

class TelescopeForm extends JPanel {

    private final EdCompTargetList owner;


    public TelescopeForm(EdCompTargetList owner) {
        this.owner = owner;
        initComponents();
    }
    JPanel panel1;
    final JPanel buttonPanel = new JPanel();
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        final JPanel targetListPanel = new JPanel();
        newMenuBar = new JMenuBar();
        newMenu = new JMenu();
        removeButton = new JButton();
        copyButton = new JButton();
        pasteButton = new JButton();
        duplicateButton = new JButton();
        primaryButton = new JButton();
        final JPanel spacerPanel = new JPanel();
        final JScrollPane posTableScrollPane = new JScrollPane();
        positionTable = new TelescopePosTableWidget(owner);
        coordinatesPanel = new JPanel();
//        objectGBW = new JPanel();
        guideGroupPanel = new JPanel();
        tag = new JComboBox();
//        final JLabel nameLabel = new JLabel();
        final JLabel guideGroupNameLabel = new JLabel();
//        targetName = new TextBoxWidget();
        guideGroupName = new TextBoxWidget();
//        resolveButton = new JButton();
//        nameServerBar = new JMenuBar();
//        nameServer = new JMenu();
//        final JPanel stretchPanel = new JPanel();
//        system = new DropDownListBoxWidget();
//        final JLabel RA_Az_STW = new JLabel();
//        final JPanel alignmentPanel = new JPanel();
//        xaxis = new TextBoxWidget();
//        final JLabel dec_El_STW = new JLabel();
//        yaxis = new TextBoxWidget();
//        extrasFolder = new JPanel();
//        nonsiderealPW = new JPanel();
//        epochofelUnits = new JLabel();
//        orbincUnits = new JLabel();
//        longascnodeUnits = new JLabel();
//        longofperiUnits = new JLabel();
//        argofperiUnits = new JLabel();
//        meandistUnits = new JLabel();
//        peridistUnits = new JLabel();
//        eccentricityUnits = new JLabel();
//        meanlongUnits = new JLabel();
//        meananomUnits = new JLabel();
//        dailymotUnits = new JLabel();
//        epochofperiUnits = new JLabel();
//        epochofelLabel = new JLabel();
//        epochofel = new NumberBoxWidget();
//        peridistLabel = new JLabel();
//        peridist = new NumberBoxWidget();
//        orbincLabel = new JLabel();
//        orbinc = new NumberBoxWidget();
//        eccentricityLabel = new JLabel();
//        eccentricity = new NumberBoxWidget();
//        longascnodeLabel = new JLabel();
//        longascnode = new NumberBoxWidget();
//        meanlongLabel = new JLabel();
//        meanlong = new NumberBoxWidget();
//        longofperiLabel = new JLabel();
//        longofperi = new NumberBoxWidget();
//        meananomLabel = new JLabel();
//        meananom = new NumberBoxWidget();
//        argofperiLabel = new JLabel();
//        argofperi = new NumberBoxWidget();
//        dailymotLabel = new JLabel();
//        dailymot = new NumberBoxWidget();
//        meandistLabel = new JLabel();
//        meandist = new NumberBoxWidget();
//        epochofperiLabel = new JLabel();
//        epochofperi = new NumberBoxWidget();
//        final JPanel orbitalElementFormatPanel = new JPanel();
//        final JLabel orbitalElementFormatLabel = new JLabel();
//        orbitalElementFormat = new DropDownListBoxWidget();
//        planetsPanel = new JPanel();
//        final JLabel label1 = new JLabel();
        final JRadioButton radioButton1 = new JRadioButton();
        final JRadioButton radioButton2 = new JRadioButton();
        final JRadioButton radioButton3 = new JRadioButton();
        final JRadioButton radioButton4 = new JRadioButton();
        final JRadioButton radioButton5 = new JRadioButton();
        final JRadioButton radioButton6 = new JRadioButton();
        final JRadioButton radioButton7 = new JRadioButton();
        final JRadioButton radioButton8 = new JRadioButton();
        final JRadioButton radioButton9 = new JRadioButton();
        panel1 = new JPanel();
//        final JLabel label2 = new JLabel();
//        calendarDate = new JCalendarPopup(new Date(), TimeZone.getTimeZone("UTC"));
//        final JLabel label4 = new JLabel();
//        calendarTime = new JComboBox();
//        final JLabel label3 = new JLabel();
//        updateRaDecButton = new JButton();
//        timeRangePlotButton = new JButton();
        setBaseButton = new JButton();
        final CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new GridBagLayout());

        //======== targetListPanel ========
        {
            targetListPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
            targetListPanel.setLayout(new BorderLayout());

            //======== buttonPanel ========
            {
                buttonPanel.setBackground(new Color(238, 238, 238));
                buttonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
                buttonPanel.setLayout(new GridBagLayout());
                ((GridBagLayout) buttonPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
                ((GridBagLayout) buttonPanel.getLayout()).rowHeights = new int[] {0, 0};
                ((GridBagLayout) buttonPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout) buttonPanel.getLayout()).rowWeights = new double[] {0.0, 1.0E-4};

                //---- newButton ----
                newMenu.setToolTipText("Create a new target or guide group");
                newMenu.setMargin(new Insets(2, 2, 2, 2));
                newMenu.setText("New");
                newMenu.setFocusable(false);
                newMenuBar.add(newMenu);
                buttonPanel.add(newMenuBar, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- removeButton ----
                removeButton.setToolTipText("Remove the selected target");
                removeButton.setMargin(new Insets(2, 2, 2, 2));
                removeButton.setText("Remove");
                removeButton.setFocusable(false);
                buttonPanel.add(removeButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- copyButton ----
                copyButton.setText("Copy");
                copyButton.setFocusable(false);
                copyButton.setToolTipText("Copy selected target coordinates");
                buttonPanel.add(copyButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 10, 0, 0), 0, 0));

                //---- pasteButton ----
                pasteButton.setText("Paste");
                pasteButton.setFocusable(false);
                pasteButton.setToolTipText("Paste coordinates on selected target");
                buttonPanel.add(pasteButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- duplicateButton ----
                duplicateButton.setText("Duplicate");
                duplicateButton.setFocusable(false);
                duplicateButton.setToolTipText("Duplicate selected target");
                buttonPanel.add(duplicateButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- primaryButton ----
                primaryButton.setText("Primary");
                primaryButton.setToolTipText("Set/unset as active guide star");
                buttonPanel.add(primaryButton, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 10, 0, 0), 0, 0));

                //======== spacerPanel ========
                {
                    spacerPanel.setBackground(null);
                    spacerPanel.setOpaque(false);
                    spacerPanel.setLayout(new BorderLayout());
                }
                buttonPanel.add(spacerPanel, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                guidingControls = new GuidingControls();
                buttonPanel.add(guidingControls.peer(), new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }

            final JPanel feedbackAndButtonPanel = new JPanel(new BorderLayout());
            feedbackAndButtonPanel.add(buttonPanel, BorderLayout.SOUTH);

            targetListPanel.add(feedbackAndButtonPanel, BorderLayout.SOUTH);

            //======== posTableScrollPane ========
            {
                posTableScrollPane.setViewportView(positionTable);
            }
            targetListPanel.add(posTableScrollPane, BorderLayout.CENTER);
        }
        add(targetListPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 0), 0, 0));

        //======== coordinatesPanel ========
        {
            coordinatesPanel.setLayout(new BorderLayout(0, 5));

            //======== objectGBW ========
            {
//                objectGBW.setLayout(new GridBagLayout());

                //---- tag ----
                tag.setToolTipText("Target Type");
//                objectGBW.add(tag, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                    new Insets(0, 0, 0, 0), 0, 0));
//
//                //---- nameLabel ----
//                nameLabel.setLabelFor(null);
//                nameLabel.setText("Name");
//                objectGBW.add(nameLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                    new Insets(0, 10, 0, 5), 0, 0));
//
//                //---- targetName ----
//                targetName.setToolTipText("Target name (type enter to resolve to RA,Dec via name server)");
//                targetName.setHorizontalAlignment(JTextField.LEFT);
//                targetName.setColumns(20);
//                objectGBW.add(targetName, new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0,
//                    GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
//                    new Insets(0, 0, 0, 0), 0, 0));
//
//                //---- resolveButton ----
//                resolveButton.setToolTipText("Resolve the target name to RA,Dec coordinates");
//                resolveButton.setMargin(new Insets(2, 2, 2, 2));
//                resolveButton.setText("Resolve");
//                resolveButton.setFocusable(false);
//                objectGBW.add(resolveButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                    new Insets(0, 2, 0, 0), 0, 0));
//
//                //---- nameServer ----
//                nameServer.setToolTipText("Select the name server for resolving the target name to RA,Dec");
//                nameServer.setFocusable(false);
//                nameServerBar.add(nameServer);
//                objectGBW.add(nameServerBar, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
//                    new Insets(0, 0, 0, 0), 0, 0));
//
//                //======== stretchPanel ========
//                {
//                    stretchPanel.setLayout(new FlowLayout());
//                }
//                objectGBW.add(stretchPanel, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0,
//                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                    new Insets(0, 0, 0, 0), 0, 0));
//
//                //---- system ----
//                system.setToolTipText("Target coordinate system");
//                system.setFocusable(false);
//                objectGBW.add(system, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                    new Insets(5, 0, 0, 0), 0, 0));
//
//                //---- RA_Az_STW ----
//                RA_Az_STW.setLabelFor(null);
//                RA_Az_STW.setText("RA");
//                objectGBW.add(RA_Az_STW, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.EAST, GridBagConstraints.NONE,
//                    new Insets(5, 10, 5, 5), 0, 0));
//
//                //======== alignmentPanel ========
//                {
//                    alignmentPanel.setLayout(new GridBagLayout());
//                    ((GridBagLayout)alignmentPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
//                    ((GridBagLayout)alignmentPanel.getLayout()).rowHeights = new int[] {0, 0};
//                    ((GridBagLayout)alignmentPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
//                    ((GridBagLayout)alignmentPanel.getLayout()).rowWeights = new double[] {0.0, 1.0E-4};
//
//                    //---- xaxis ----
//                    xaxis.setToolTipText("Target RA coordinate (HH:MM:SS.sss)");
//                    xaxis.setHorizontalAlignment(JTextField.LEFT);
//                    alignmentPanel.add(xaxis, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                        new Insets(0, 0, 0, 5), 0, 0));
//
//                    //---- Dec_El_STW ----
//                    dec_El_STW.setLabelFor(null);
//                    dec_El_STW.setText("Dec");
//                    alignmentPanel.add(dec_El_STW, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                        new Insets(0, 0, 0, 5), 0, 0));
//
//                    //---- yaxis ----
//                    yaxis.setToolTipText("Target Dec coordinate (DD:MM:SS.sss)");
//                    yaxis.setHorizontalAlignment(JTextField.LEFT);
//                    alignmentPanel.add(yaxis, new GridBagConstraints(2, 0, 1, 1, 0.5, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                        new Insets(0, 0, 0, 0), 0, 0));
//                }
//                objectGBW.add(alignmentPanel, new GridBagConstraints(2, 1, 4, 1, 0.0, 0.0,
//                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                    new Insets(5, 0, 5, 0), 0, 0));
            }
//            coordinatesPanel.add(objectGBW, BorderLayout.NORTH);

            //======== guideGroupPanel ========
            {
                guideGroupPanel.setLayout(new GridBagLayout());

                //---- guideGroupNameLabel ----
                guideGroupNameLabel.setLabelFor(null);
                guideGroupNameLabel.setText("Guide Group Name");
                guideGroupPanel.add(guideGroupNameLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 10, 0, 5), 0, 0));

                //---- guideGroupName ----
                guideGroupName.setToolTipText("Guide Group name (optional)");
                guideGroupName.setHorizontalAlignment(JTextField.LEFT);
                guideGroupName.setColumns(20);
                guideGroupPanel.add(guideGroupName, new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));

                // fill
                guideGroupPanel.add(new JPanel(), new GridBagConstraints(1, 1, 3, 1, 1.0, 1.0,
                    GridBagConstraints.NORTH, GridBagConstraints.NONE,
                    new Insets(233, 0, 0, 0), 0, 0));
            }
            coordinatesPanel.add(guideGroupPanel, BorderLayout.CENTER);
            guideGroupPanel.setVisible(false);

//            //======== extrasFolder ========
//            {
//                extrasFolder.setLayout(new CardLayout());
//
//                //======== nonsiderealPW ========
//                {
//                    nonsiderealPW.setLayout(new GridBagLayout());
//                    ((GridBagLayout)nonsiderealPW.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 11, 0, 5, 0, 6};
//                    ((GridBagLayout)nonsiderealPW.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 0.0, 0.0, 0.0};
//
//                    //---- epochofelUnits ----
//                    epochofelUnits.setText("JD");
//                    nonsiderealPW.add(epochofelUnits, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(6, 3, 0, 11), 0, 0));
//
//                    //---- orbincUnits ----
//                    orbincUnits.setText("deg");
//                    nonsiderealPW.add(orbincUnits, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(6, 3, 0, 11), 0, 0));
//
//                    //---- longascnodeUnits ----
//                    longascnodeUnits.setText("deg");
//                    nonsiderealPW.add(longascnodeUnits, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(6, 3, 0, 11), 0, 0));
//
//                    //---- longofperiUnits ----
//                    longofperiUnits.setText("deg");
//                    nonsiderealPW.add(longofperiUnits, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(6, 3, 0, 11), 0, 0));
//
//                    //---- argofperiUnits ----
//                    argofperiUnits.setText("deg");
//                    nonsiderealPW.add(argofperiUnits, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(6, 3, 0, 11), 0, 0));
//
//                    //---- meandistUnits ----
//                    meandistUnits.setVisible(false);
//                    meandistUnits.setText("AU");
//                    nonsiderealPW.add(meandistUnits, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(0, 3, 0, 11), 0, 0));
//
//                    //---- peridistUnits ----
//                    peridistUnits.setText("AU");
//                    nonsiderealPW.add(peridistUnits, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(6, 3, 0, 11), 0, 0));
//
//                    //---- eccentricityUnits ----
//                    eccentricityUnits.setRequestFocusEnabled(true);
//                    eccentricityUnits.setText("");
//                    nonsiderealPW.add(eccentricityUnits, new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(0, 0, 0, 11), 0, 0));
//
//                    //---- meanlongUnits ----
//                    meanlongUnits.setText("deg");
//                    nonsiderealPW.add(meanlongUnits, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(6, 3, 0, 11), 0, 0));
//
//                    //---- meananomUnits ----
//                    meananomUnits.setText("deg");
//                    nonsiderealPW.add(meananomUnits, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(6, 3, 0, 11), 0, 0));
//
//                    //---- dailymotUnits ----
//                    dailymotUnits.setVisible(false);
//                    dailymotUnits.setText("deg");
//                    nonsiderealPW.add(dailymotUnits, new GridBagConstraints(5, 6, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(0, 3, 0, 11), 0, 0));
//
//                    //---- epochofperiUnits ----
//                    epochofperiUnits.setVisible(false);
//                    epochofperiUnits.setText("JD");
//                    nonsiderealPW.add(epochofperiUnits, new GridBagConstraints(5, 10, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.WEST, GridBagConstraints.NONE,
//                        new Insets(0, 3, 0, 11), 0, 0));
//
//                    //---- epochofelLabel ----
//                    epochofelLabel.setText("epochofel:");
//                    nonsiderealPW.add(epochofelLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- epochofel ----
//                    epochofel.setText("");
//                    nonsiderealPW.add(epochofel, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- peridistLabel ----
//                    peridistLabel.setText("peridist:");
//                    nonsiderealPW.add(peridistLabel, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- peridist ----
//                    peridist.setText("");
//                    nonsiderealPW.add(peridist, new GridBagConstraints(4, 2, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- orbincLabel ----
//                    orbincLabel.setText("orbinc:");
//                    nonsiderealPW.add(orbincLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- orbinc ----
//                    orbinc.setText("");
//                    nonsiderealPW.add(orbinc, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- eccentricityLabel ----
//                    eccentricityLabel.setText("eccentricity:");
//                    nonsiderealPW.add(eccentricityLabel, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- eccentricity ----
//                    eccentricity.setText("");
//                    nonsiderealPW.add(eccentricity, new GridBagConstraints(4, 3, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- longascnodeLabel ----
//                    longascnodeLabel.setText("longascnode:");
//                    nonsiderealPW.add(longascnodeLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- longascnode ----
//                    longascnode.setText("");
//                    nonsiderealPW.add(longascnode, new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- meanlongLabel ----
//                    meanlongLabel.setText("meanlong:");
//                    nonsiderealPW.add(meanlongLabel, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- meanlong ----
//                    meanlong.setText("");
//                    nonsiderealPW.add(meanlong, new GridBagConstraints(4, 4, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- longofperiLabel ----
//                    longofperiLabel.setText("longofperi:");
//                    nonsiderealPW.add(longofperiLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- longofperi ----
//                    longofperi.setText("");
//                    nonsiderealPW.add(longofperi, new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- meananomLabel ----
//                    meananomLabel.setText("meananom:");
//                    nonsiderealPW.add(meananomLabel, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- meananom ----
//                    meananom.setText("");
//                    nonsiderealPW.add(meananom, new GridBagConstraints(4, 5, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 1), 0, 0));
//
//                    //---- argofperiLabel ----
//                    argofperiLabel.setVisible(false);
//                    argofperiLabel.setText("argofperi:");
//                    nonsiderealPW.add(argofperiLabel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- argofperi ----
//                    argofperi.setVisible(false);
//                    argofperi.setText("");
//                    nonsiderealPW.add(argofperi, new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- dailymotLabel ----
//                    dailymotLabel.setVisible(false);
//                    dailymotLabel.setText("dailymot:");
//                    nonsiderealPW.add(dailymotLabel, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- dailymot ----
//                    dailymot.setVisible(false);
//                    dailymot.setText("");
//                    nonsiderealPW.add(dailymot, new GridBagConstraints(4, 6, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 1), 0, 0));
//
//                    //---- meandistLabel ----
//                    meandistLabel.setVisible(false);
//                    meandistLabel.setText("meandist:");
//                    nonsiderealPW.add(meandistLabel, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- meandist ----
//                    meandist.setVisible(false);
//                    meandist.setText("");
//                    nonsiderealPW.add(meandist, new GridBagConstraints(1, 10, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 0), 0, 0));
//
//                    //---- epochofperiLabel ----
//                    epochofperiLabel.setVisible(false);
//                    epochofperiLabel.setText("epochofperi:");
//                    nonsiderealPW.add(epochofperiLabel, new GridBagConstraints(3, 10, 1, 1, 0.0, 0.0,
//                        GridBagConstraints.EAST, GridBagConstraints.NONE,
//                        new Insets(6, 11, 0, 0), 0, 0));
//
//                    //---- epochofperi ----
//                    epochofperi.setVisible(false);
//                    epochofperi.setText("");
//                    nonsiderealPW.add(epochofperi, new GridBagConstraints(4, 10, 1, 1, 1.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(6, 5, 0, 1), 0, 0));
//
//                    //======== orbitalElementFormatPanel ========
//                    {
//                        orbitalElementFormatPanel.setLayout(new FlowLayout());
//
//                        //---- orbitalElementFormatLabel ----
//                        orbitalElementFormatLabel.setText("Orbital Element Format:");
//                        orbitalElementFormatPanel.add(orbitalElementFormatLabel);
//                        orbitalElementFormatPanel.add(orbitalElementFormat);
//                    }
//                    nonsiderealPW.add(orbitalElementFormatPanel, new GridBagConstraints(0, 0, 6, 1, 0.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//                        new Insets(0, 0, 0, 0), 0, 0));
//
//                    //======== planetsPanel ========
//                    {
//                        planetsPanel.setLayout(new FormLayout(
//                            new ColumnSpec[] {
//                                new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
//                                new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX9, FormSpec.NO_GROW),
//                                FormFactory.DEFAULT_COLSPEC,
//                                new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX9, FormSpec.NO_GROW),
//                                FormFactory.DEFAULT_COLSPEC,
//                                new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX9, FormSpec.NO_GROW),
//                                FormFactory.DEFAULT_COLSPEC,
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
//                            },
//                            new RowSpec[] {
//                                FormFactory.DEFAULT_ROWSPEC,
//                                FormFactory.LINE_GAP_ROWSPEC,
//                                new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
//                                FormFactory.LINE_GAP_ROWSPEC,
//                                new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
//                                FormFactory.PARAGRAPH_GAP_ROWSPEC,
//                                new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.NO_GROW),
//                                FormFactory.PARAGRAPH_GAP_ROWSPEC,
//                                new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.NO_GROW)
//                            }));
//
//                        //---- label1 ----
//                        label1.setText("Select Solar System Object");
//                        planetsPanel.add(label1, cc.xywh(3, 1, 5, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
//
//                        //---- radioButton1 ----
//                        radioButton1.setText("Moon");
//                        radioButton1.setSelected(true);
//                        planetsPanel.add(radioButton1, cc.xy(3, 5));
//
//                        //---- radioButton2 ----
//                        radioButton2.setText("Mercury");
//                        planetsPanel.add(radioButton2, cc.xy(5, 5));
//
//                        //---- radioButton3 ----
//                        radioButton3.setText("Venus");
//                        planetsPanel.add(radioButton3, cc.xy(7, 5));
//
//                        //---- radioButton4 ----
//                        radioButton4.setText("Mars");
//                        planetsPanel.add(radioButton4, cc.xy(3, 7));
//
//                        //---- radioButton5 ----
//                        radioButton5.setText("Jupiter");
//                        planetsPanel.add(radioButton5, cc.xy(5, 7));
//
//                        //---- radioButton6 ----
//                        radioButton6.setText("Saturn");
//                        planetsPanel.add(radioButton6, cc.xy(7, 7));
//
//                        //---- radioButton7 ----
//                        radioButton7.setText("Uranus");
//                        planetsPanel.add(radioButton7, cc.xy(3, 9));
//
//                        //---- radioButton8 ----
//                        radioButton8.setText("Neptune");
//                        planetsPanel.add(radioButton8, cc.xy(5, 9));
//
//                        //---- radioButton9 ----
//                        radioButton9.setText("Pluto");
//                        planetsPanel.add(radioButton9, cc.xy(7, 9));
//                    }
//                    nonsiderealPW.add(planetsPanel, new GridBagConstraints(0, 7, 6, 1, 0.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                        new Insets(0, 0, 0, 0), 0, 0));
//
//                    //======== panel1 ========
//                    {
//                        panel1.setLayout(new FormLayout(
//                            new ColumnSpec[] {
//                                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                FormFactory.DEFAULT_COLSPEC,
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                //FormFactory.DEFAULT_COLSPEC,
//                                ColumnSpec.decode("75dlu"),
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                FormFactory.DEFAULT_COLSPEC,
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                FormFactory.DEFAULT_COLSPEC,
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                FormFactory.DEFAULT_COLSPEC,
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                ColumnSpec.decode("min(default;80dlu)"),
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                ColumnSpec.decode("min(default;80dlu)"),
//                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
//                                new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
//                            },
//                            RowSpec.decodeSpecs("default")));
//
//                        //---- label2 ----
//                        label2.setText("Valid on");
//                        panel1.add(label2, cc.xy(3, 1));
//                        panel1.add(calendarDate, cc.xy(5, 1));
//
//                        //---- label4 ----
//                        label4.setText("at");
//                        panel1.add(label4, cc.xy(7, 1));
//
//                        //---- calendarTime ----
//                        calendarTime.setEditable(true);
//                        panel1.add(calendarTime, cc.xy(9, 1));
//
//                        //---- label3 ----
//                        label3.setText("UTC");
//                        panel1.add(label3, cc.xy(11, 1));
//
//                        //---- updateRaDecButton ----
//                        updateRaDecButton.setText("Go");
//                        updateRaDecButton.setToolTipText("Get the position of the object at the specified date and time ");
//                        panel1.add(updateRaDecButton, cc.xy(13, 1));
//
//                        //---- timeRangePlotButton ----
//                        timeRangePlotButton.setText("Plot");
//                        timeRangePlotButton.setToolTipText("Plot the ephemeris of this object starting at the given time.\nThe ephemeris will be obtained from JPL Horizons if needed. ");
//                        panel1.add(timeRangePlotButton, cc.xy(15, 1));
//                    }
//                    nonsiderealPW.add(panel1, new GridBagConstraints(0, 9, 6, 1, 0.0, 0.0,
//                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
//                        new Insets(0, 0, 0, 0), 0, 0));
//                }
//            }
//            coordinatesPanel.add(extrasFolder, BorderLayout.SOUTH);
        }
        add(coordinatesPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(5, 0, 5, 0), 0, 0));

        //======== guidingPanel ========

        autoGuideStarButton         = guidingControls.autoGuideStarButton().peer();
        manualGuideStarButton       = guidingControls.manualGuideStarButton().peer();
        autoGuideStarGuiderSelector = guidingControls.autoGuideStarGuiderSelector();

//        //---- planetButtonGroup ----
//        final ButtonGroup planetButtonGroup = new ButtonGroup();
//        planetButtonGroup.add(radioButton1);
//        planetButtonGroup.add(radioButton2);
//        planetButtonGroup.add(radioButton3);
//        planetButtonGroup.add(radioButton4);
//        planetButtonGroup.add(radioButton5);
//        planetButtonGroup.add(radioButton6);
//        planetButtonGroup.add(radioButton7);
//        planetButtonGroup.add(radioButton8);
//        planetButtonGroup.add(radioButton9);
//        // JFormDesigner - End of component initialization  //GEN-END:initComponents
//
//        planetButtons = new JRadioButton[9];
//        planetButtons[0] = radioButton1;
//        planetButtons[1] = radioButton2;
//        planetButtons[2] = radioButton3;
//        planetButtons[3] = radioButton4;
//        planetButtons[4] = radioButton5;
//        planetButtons[5] = radioButton6;
//        planetButtons[6] = radioButton7;
//        planetButtons[7] = radioButton8;
//        planetButtons[8] = radioButton9;
    }

    JMenuBar newMenuBar;
    JMenu newMenu;
    JButton removeButton;
    JButton copyButton;
    JButton pasteButton;
    JButton duplicateButton;
    JButton primaryButton;
    TelescopePosTableWidget positionTable;
    JPanel coordinatesPanel;
//    JPanel objectGBW;
    JPanel guideGroupPanel;
    JComboBox<PositionType> tag;
//    TextBoxWidget targetName;
    TextBoxWidget guideGroupName;
//    JButton resolveButton;
//    JMenuBar nameServerBar;
//    JMenu nameServer;
//    DropDownListBoxWidget system;
//    TextBoxWidget xaxis;
//    TextBoxWidget yaxis;
//    JPanel extrasFolder;
//    JPanel nonsiderealPW;
//    JLabel epochofelUnits;
//    JLabel orbincUnits;
//    JLabel longascnodeUnits;
//    JLabel longofperiUnits;
//    JLabel argofperiUnits;
//    JLabel meandistUnits;
//    JLabel peridistUnits;
//    JLabel eccentricityUnits;
//    JLabel meanlongUnits;
//    JLabel meananomUnits;
//    JLabel dailymotUnits;
//    JLabel epochofperiUnits;
//    JLabel epochofelLabel;
//    NumberBoxWidget epochofel;
//    JLabel peridistLabel;
//    NumberBoxWidget peridist;
//    JLabel orbincLabel;
//    NumberBoxWidget orbinc;
//    JLabel eccentricityLabel;
//    NumberBoxWidget eccentricity;
//    JLabel longascnodeLabel;
//    NumberBoxWidget longascnode;
//    JLabel meanlongLabel;
//    NumberBoxWidget meanlong;
//    JLabel longofperiLabel;
//    NumberBoxWidget longofperi;
//    JLabel meananomLabel;
//    NumberBoxWidget meananom;
//    JLabel argofperiLabel;
//    NumberBoxWidget argofperi;
//    JLabel dailymotLabel;
//    NumberBoxWidget dailymot;
//    JLabel meandistLabel;
//    NumberBoxWidget meandist;
//    JLabel epochofperiLabel;
//    NumberBoxWidget epochofperi;
//    DropDownListBoxWidget orbitalElementFormat;
//    JPanel planetsPanel;
//    JCalendarPopup calendarDate;
//    JComboBox<TimeConfig> calendarTime;
//    JButton updateRaDecButton;
//    JButton timeRangePlotButton;
    JButton setBaseButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

//    JRadioButton[] planetButtons;

    // Components for the Guider panel.
    GuidingControls guidingControls;

    // These are just convenient placeholders to simplify the interactions with EdCompTargetList and are
    // set to the peers in the guidingPanel.
    JButton autoGuideStarButton;
    JButton manualGuideStarButton;
    AgsSelectorControl autoGuideStarGuiderSelector;
}

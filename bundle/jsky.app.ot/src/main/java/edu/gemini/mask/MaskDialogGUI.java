package edu.gemini.mask;

import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import jsky.util.gui.*;

public class MaskDialogGUI extends JFrame {

    protected final JComboBox<String> instrument;
    protected final NumberBoxWidget wavelength;
    protected final DropDownListBoxWidget<GmosCommonType.Filter> filter;
    protected final DropDownListBoxWidget<GmosCommonType.Disperser> disperser;
    protected final JComboBox<String> shuffleMode;
    protected final JTabbedPane shufflePane;
    protected final JPanel noShufflePanel;
    protected final JPanel microShufflePanel;
    protected final NumberBoxWidget slitLength;
    protected final NumberBoxWidget microShuffleAmountArcsec;
    protected final NumberBoxWidget microShuffleAmountPixels;
    protected final JButton microResetButton;
    protected final JPanel bandShufflePanel;
    protected final NumberBoxWidget bandSize;
    protected final NumberBoxWidget bandsYOffset;
    protected final NumberBoxWidget bandShuffleAmountArcsec;
    protected final NumberBoxWidget bandShuffleAmountPixels;
    protected final JButton bandResetButton;
    protected final JLabel label13;
    protected final JScrollPane scrollPane1;
    protected final JTable bandTable;
    protected final JSpinner numMasks;
    protected final JButton makeMaskFilesButton;
    protected final JButton cancelButton;

    public MaskDialogGUI() {

        JPanel dialogPane = new JPanel();
        JPanel contentPane = new JPanel();
        JLabel label2 = new JLabel();
        instrument = new JComboBox<>();
        JLabel label5 = new JLabel();
        wavelength = new NumberBoxWidget();
        JLabel label3 = new JLabel();
        JLabel label4 = new JLabel();
        filter = new DropDownListBoxWidget<>();
        disperser = new DropDownListBoxWidget<>();
        JLabel label14 = new JLabel();
        JLabel label1 = new JLabel();
        shuffleMode = new JComboBox<>();
        shufflePane = new JTabbedPane();
        noShufflePanel = new JPanel();
        microShufflePanel = new JPanel();
        JLabel label6 = new JLabel();
        slitLength = new NumberBoxWidget();
        JLabel label7 = new JLabel();
        microShuffleAmountArcsec = new NumberBoxWidget();
        JLabel label8 = new JLabel();
        microShuffleAmountPixels = new NumberBoxWidget();
        microResetButton = new JButton();
        bandShufflePanel = new JPanel();
        JLabel label9 = new JLabel();
        bandSize = new NumberBoxWidget();
        JLabel label10 = new JLabel();
        bandsYOffset = new NumberBoxWidget();
        JLabel label11 = new JLabel();
        bandShuffleAmountArcsec = new NumberBoxWidget();
        JLabel label12 = new JLabel();
        bandShuffleAmountPixels = new NumberBoxWidget();
        bandResetButton = new JButton();
        label13 = new JLabel();
        scrollPane1 = new JScrollPane();
        bandTable = new JTable();
        numMasks = new JSpinner();
        JPanel buttonBar = new JPanel();
        makeMaskFilesButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Mask Dialog");
        Container contentPane2 = getContentPane();
        contentPane2.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setPreferredSize(new Dimension(560, 442));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPane ========
            {
                contentPane.setPreferredSize(new Dimension(486, 390));
                contentPane.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        ColumnSpec.decode("max(default;50dlu)"),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                    },
                    new RowSpec[] {
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.PARAGRAPH_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC
                    }));

                //---- label2 ----
                label2.setText("Instrument");
                contentPane.add(label2, cc.xy(1, 1));

                //---- instrument ----
                instrument.setModel(new DefaultComboBoxModel<>(new String[] {
                    "GMOS-N",
                    "GMOS-S"
                }));
                contentPane.add(instrument, cc.xy(3, 1));

                //---- label5 ----
                label5.setText("Central Wavelength");
                contentPane.add(label5, cc.xy(5, 1));
                contentPane.add(wavelength, cc.xy(7, 1));

                //---- label3 ----
                label3.setText("Filter");
                contentPane.add(label3, cc.xy(1, 3));

                //---- label4 ----
                label4.setText("Disperser");
                contentPane.add(label4, cc.xy(5, 3));
                contentPane.add(filter, cc.xy(3, 3));
                contentPane.add(disperser, cc.xy(7, 3));

                //---- label14 ----
                label14.setText("Number of Masks");
                contentPane.add(label14, cc.xy(1, 5));

                //---- label1 ----
                label1.setText("Shuffle Mode");
                contentPane.add(label1, cc.xy(5, 5));

                //---- shuffleMode ----
                shuffleMode.setModel(new DefaultComboBoxModel<>(new String[] {
                    "No Shuffle",
                    "Microshuffle Mode",
                    "Band Shuffling"
                }));
                contentPane.add(shuffleMode, cc.xy(7, 5));

                //======== shufflePane ========
                {

                    //======== noShufflePanel ========
                    {
                        noShufflePanel.setLayout(new FormLayout(
                            "default",
                            "default"));
                    }
                    shufflePane.addTab("No Shuffle", noShufflePanel);


                    //======== microShufflePanel ========
                    {
                        microShufflePanel.setLayout(new FormLayout(
                            new ColumnSpec[] {
                                new ColumnSpec(Sizes.dluX(0)),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                ColumnSpec.decode("max(default;50dlu):grow")
                            },
                            new RowSpec[] {
                                new RowSpec(Sizes.dluY(0)),
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC
                            }));

                        //---- label6 ----
                        label6.setText("Slit Length (arcsec)");
                        microShufflePanel.add(label6, cc.xy(3, 3));
                        microShufflePanel.add(slitLength, cc.xy(5, 3));

                        //---- label7 ----
                        label7.setText("Shuffle Amount (arcsec)");
                        microShufflePanel.add(label7, cc.xy(3, 5));
                        microShufflePanel.add(microShuffleAmountArcsec, cc.xy(5, 5));

                        //---- label8 ----
                        label8.setText("Shuffle Amount (unbinned pixels)");
                        microShufflePanel.add(label8, cc.xy(3, 7));
                        microShufflePanel.add(microShuffleAmountPixels, cc.xy(5, 7));

                        //---- microResetButton ----
                        microResetButton.setText("Reset to Default Settings");
                        microShufflePanel.add(microResetButton, cc.xywh(5, 9, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
                    }
                    shufflePane.addTab("Microshuffle Mode Settings", microShufflePanel);


                    //======== bandShufflePanel ========
                    {
                        bandShufflePanel.setLayout(new FormLayout(
                            new ColumnSpec[] {
                                new ColumnSpec(Sizes.dluX(0)),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                ColumnSpec.decode("max(default;50dlu):grow")
                            },
                            new RowSpec[] {
                                new RowSpec(Sizes.dluY(0)),
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
                                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC
                            }));

                        //---- label9 ----
                        label9.setText("Band Size (unbinned pixels)");
                        bandShufflePanel.add(label9, cc.xy(3, 3));
                        bandShufflePanel.add(bandSize, cc.xy(5, 3));

                        //---- label10 ----
                        label10.setText("Bands y Offset (unbinned pix)");
                        bandShufflePanel.add(label10, cc.xy(3, 5));
                        bandShufflePanel.add(bandsYOffset, cc.xy(5, 5));

                        //---- label11 ----
                        label11.setText("Shuffle Amount (arcsec)");
                        bandShufflePanel.add(label11, cc.xy(3, 7));
                        bandShufflePanel.add(bandShuffleAmountArcsec, cc.xy(5, 7));

                        //---- label12 ----
                        label12.setText("Shuffle Amount (unbinned pixels)");
                        bandShufflePanel.add(label12, cc.xy(3, 9));
                        bandShufflePanel.add(bandShuffleAmountPixels, cc.xy(5, 9));

                        //---- bandResetButton ----
                        bandResetButton.setText("Reset to Default Settings");
                        bandShufflePanel.add(bandResetButton, cc.xywh(5, 11, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                        //---- label13 ----
                        label13.setText("Title");
                        bandShufflePanel.add(label13, cc.xywh(3, 13, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                        //======== scrollPane1 ========
                        {
                            scrollPane1.setViewportView(bandTable);
                        }
                        bandShufflePanel.add(scrollPane1, cc.xywh(3, 15, 3, 1));
                    }
                    shufflePane.addTab("Band Shuffle Settings", bandShufflePanel);

                }
                contentPane.add(shufflePane, cc.xywh(1, 7, 9, 1));

                //---- numMasks ----
                numMasks.setModel(new SpinnerNumberModel(1, 1, null, 1));
                contentPane.add(numMasks, cc.xy(3, 5));
            }
            dialogPane.add(contentPane, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.GLUE_COLSPEC,
                        FormFactory.BUTTON_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.BUTTON_COLSPEC
                    },
                    RowSpec.decodeSpecs("pref")));

                //---- makeMaskFilesButton ----
                makeMaskFilesButton.setText("Make Mask Files");
                buttonBar.add(makeMaskFilesButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane2.add(dialogPane, BorderLayout.CENTER);
    }

}

package jsky.app.ot.gemini.editor;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.obsclass.ObsClass;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import java.awt.*;

public class GhostIterFlatObsForm extends JPanel {
    final JRadioButton[] lamps;
    final JCheckBox[] arcs;
    final DropDownListBoxWidget<CalUnitParams.Shutter> shutter = new DropDownListBoxWidget<>();
    final JComboBox<CalUnitParams.Filter> filter = new JComboBox<>();
    final DropDownListBoxWidget<CalUnitParams.Diffuser> diffuser = new DropDownListBoxWidget<>();
    final JSpinner repeatSpinner = new JSpinner();
    final DropDownListBoxWidget<ObsClass> obsClass = new DropDownListBoxWidget<>();
    final TextBoxWidget redExposureTime = new TextBoxWidget();
    final JSpinner redExposureCount = new JSpinner();
    final TextBoxWidget blueExposureTime = new TextBoxWidget();
    final JSpinner blueExposureCount = new JSpinner();

    public GhostIterFlatObsForm() {
        final CellConstraints cc = new CellConstraints();
        final FormLayout formLayout = new FormLayout(
                new ColumnSpec[] {
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        ColumnSpec.decode("max(pref;50dlu)"),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.PREF_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX7, FormSpec.NO_GROW)
                },
                new RowSpec[] {
                        FormFactory.PARAGRAPH_GAP_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
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
                        new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                });
        setLayout(formLayout);
        formLayout.setRowGroups(new int[][] {{5, 25}});

        /** Lamp **/
        add(new JLabel("Lamp"), cc.xy(3, 7));
        final JPanel lampPanel = new JPanel();
        lampPanel.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC
                },
                RowSpec.decodeSpecs("default"))
        );

        final ButtonGroup group = new ButtonGroup();
        lamps = new JRadioButton[3];
        for (int i = 0; i < 3; ++i) {
            lamps[i] = new JRadioButton();
            lampPanel.add(lamps[i], cc.xy(i*2 + 1, 1));
            group.add(lamps[i]);
        }
        lamps[0].setSelected(true);

        add(lampPanel, cc.xywh(5, 7, 5, 1));

        /** Arc **/
        add(new JLabel("Arcs"), cc.xy(3, 9));
        final JPanel arcPanel = new JPanel();
        arcPanel.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                },
                RowSpec.decodeSpecs("default"))
        );

        arcs = new JCheckBox[4];
        for (int i=0; i < 4; ++i) {
            arcs[i] = new JCheckBox();
            if (i == 3)
                arcs[i].setHorizontalAlignment(SwingConstants.LEADING);
            arcPanel.add(arcs[i], cc.xy(i*2 + 1, 1));
        }

        add(arcPanel, cc.xywh(5, 9, 5, 1));

        /** Shutter **/
        add(new JLabel("Shutter"), cc.xy(3, 11));
        add(shutter, cc.xy(5, 11));

        /** Filter **/
        add(new JLabel("Filter"), cc.xy(3, 13));
        add(filter, cc.xy(5, 13));

        /** Observe **/
        add(new JLabel("Diffuser"), cc.xy(3, 15));
        add(diffuser, cc.xy(5, 15));

        /** Observe **/
        add(new JLabel("Observe"), cc.xy(3, 17));
        repeatSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        repeatSpinner.setMinimumSize(new Dimension(80, 20));
        repeatSpinner.setPreferredSize(new Dimension(80, 20));
        repeatSpinner.setOpaque(false);
        add(repeatSpinner, cc.xy(5, 17));

        /** Red Exposure Time **/
        add(new JLabel("Red Exposure Time"), cc.xy(3, 19));
        redExposureTime.setMinimumSize(new Dimension(80, 20));
        redExposureTime.setPreferredSize(new Dimension(80, 20));
        add(redExposureTime, cc.xy(5, 19));
        add(new JLabel("(sec)"), cc.xy(7, 19));

        /** Red Exposure Count **/
        add(new JLabel("Red Exposure Count"), cc.xy(3, 21));
        redExposureCount.setModel(new SpinnerNumberModel(1, 1, null, 1));
        redExposureCount.setMinimumSize(new Dimension(80, 20));
        redExposureCount.setPreferredSize(new Dimension(80, 20));
        redExposureCount.setOpaque(false);
        add(redExposureCount, cc.xy(5, 21));
        add(new JLabel("X"), cc.xy(7, 21));

        /** Blue Exposure Time **/
        add(new JLabel("Blue Exposure Time"), cc.xy(3, 23));
        blueExposureCount.setModel(new SpinnerNumberModel(1, 1, null, 1));
        blueExposureTime.setMinimumSize(new Dimension(80, 20));
        blueExposureTime.setPreferredSize(new Dimension(80, 20));
        add(blueExposureTime, cc.xy(5, 23));
        add(new JLabel("(sec)"), cc.xy(7, 23));

        /** Blue Exposure Count **/
        add(new JLabel("Blue Exposure Count"), cc.xy(3, 25));
        blueExposureCount.setMinimumSize(new Dimension(80, 20));
        blueExposureCount.setPreferredSize(new Dimension(80, 20));
        blueExposureCount.setOpaque(false);
        add(blueExposureCount, cc.xy(5, 25));
        add(new JLabel("X"), cc.xy(7, 25));

        /** Class Controls **/
        add(new JLabel("Class"), cc.xywh(7, 3, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        add(obsClass, cc.xywh(9, 3, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
    }
}

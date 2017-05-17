package jsky.app.ot.gemini.editor;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.obsclass.ObsClass;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import java.awt.*;

public class IterFlatObsForm extends JPanel {
    public IterFlatObsForm() {
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
                new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
            });
        setLayout(formLayout);
        formLayout.setRowGroups(new int[][] {{5, 25}});

        // --- Lamp Controls ---
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

        // --- Arc Controls ---
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
                    FormFactory.DEFAULT_COLSPEC
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

        // --- Shutter Controls ---
        add(new JLabel("Shutter"), cc.xy(3, 11));
        shutter = new DropDownListBoxWidget<>();
        add(shutter, cc.xy(5, 11));

        // --- Filter Controls ---
        add(new JLabel("Filter"), cc.xy(3, 13));
        filter = new JComboBox<>();
        add(filter, cc.xy(5, 13));

        // --- Diffuser Controls ---
        add(new JLabel("Diffuser"), cc.xy(3, 15));
        diffuser = new DropDownListBoxWidget<>();
        add(diffuser, cc.xy(5, 15));

        // --- Observe Controls ---
        add(new JLabel("Observe"), cc.xy(3, 17));
        repeatSpinner = new JSpinner();
        repeatSpinner.setMinimumSize(new Dimension(80, 20));
        repeatSpinner.setOpaque(false);
        repeatSpinner.setPreferredSize(new Dimension(80, 20));
        add(repeatSpinner, cc.xy(5, 17));

        // --- Exposure Time Controls ---
        add(new JLabel("Exposure Time"), cc.xy(3, 19));
        exposureTime = new TextBoxWidget();
        exposureTime.setMinimumSize(new Dimension(80, 20));
        exposureTime.setPreferredSize(new Dimension(80, 20));
        add(exposureTime, cc.xy(5, 19));
        add(new JLabel("(sec)"), cc.xy(7, 19));

        // --- Coadds Controls ---
        add(new JLabel("Coadds"), cc.xy(3, 21));
        coadds = new TextBoxWidget();
        coadds.setMinimumSize(new Dimension(80, 20));
        coadds.setPreferredSize(new Dimension(80, 20));
        add(coadds, cc.xy(5, 21));
        add(new JLabel("(exp / obs)"), cc.xy(7, 21));

        // --- Class Controls ---
        add(new JLabel("Class"), cc.xywh(7, 3, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        obsClass = new DropDownListBoxWidget<>();
        add(obsClass, cc.xywh(9, 3, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
    }

    final JRadioButton[] lamps;
    final JCheckBox[] arcs;
    final DropDownListBoxWidget<CalUnitParams.Shutter> shutter;
    final JComboBox<CalUnitParams.Filter> filter;
    final DropDownListBoxWidget<CalUnitParams.Diffuser> diffuser;
    final JSpinner repeatSpinner;
    final TextBoxWidget exposureTime;
    final TextBoxWidget coadds;
    final DropDownListBoxWidget<ObsClass> obsClass;
}

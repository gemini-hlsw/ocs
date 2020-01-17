package jsky.app.ot.editor;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.spModel.obsclass.ObsClass;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;

final public class GhostIterDarkObsForm extends JPanel {
    final DropDownListBoxWidget<ObsClass> obsClass = new DropDownListBoxWidget<>();
    final JSpinner repeatSpinner = new JSpinner();
    final NumberBoxWidget redExposureTime = new NumberBoxWidget();
    final JSpinner redExposureCount = new JSpinner();
    final NumberBoxWidget blueExposureTime = new NumberBoxWidget();
    final JSpinner blueExposureCount = new JSpinner();

    public GhostIterDarkObsForm() {
        final CellConstraints cc = new CellConstraints();

        setLayout(new FormLayout(
                new ColumnSpec[]{
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DLUX11, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        ColumnSpec.decode("max(default;40dlu)"),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC
                },
                new RowSpec[]{
                        FormFactory.PARAGRAPH_GAP_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        new RowSpec(RowSpec.TOP, Sizes.DLUY9, FormSpec.DEFAULT_GROW),
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
                        new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                }));

        add(new JLabel("Class"), cc.xy(9, 3));
        add(obsClass, cc.xywh(11, 3, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

        add(new JLabel("Observe"), cc.xy(3, 5));
        repeatSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        add(repeatSpinner, cc.xy(5, 5));
        add(new JLabel("X"), cc.xy(7, 5));

        add(new JLabel("Red Exposure Time"), cc.xy(3, 7));
        redExposureTime.setToolTipText("The red exposure time in seconds");
        add(redExposureTime, cc.xy(5, 7));
        add(new JLabel("(sec)"), cc.xy(7, 7));

        add(new JLabel("Red Exposure Count"), cc.xy(3, 9));
        redExposureCount.setModel(new SpinnerNumberModel(1, 1, null, 1));
        add(redExposureCount, cc.xy(5, 9));
        add(new JLabel("X"), cc.xy(7, 9));

        add(new JLabel("Blue Exposure Time"), cc.xy(3, 11));
        blueExposureTime.setToolTipText("The blue exposure time in seconds");
        add(blueExposureTime, cc.xy(5, 11));
        add(new JLabel("(sec)"), cc.xy(7, 11));

        add(new JLabel("Blue Exposure Count"), cc.xy(3, 13));
        blueExposureCount.setModel(new SpinnerNumberModel(1, 1, null, 1));
        add(blueExposureCount, cc.xy(5, 13));
        add(new JLabel("X"), cc.xy(7, 13));
    }
}

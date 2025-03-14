package jsky.app.ot.gemini.visitor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.gui.bean.ComboPropertyCtrl;
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl;
import edu.gemini.spModel.gemini.visitor.VisitorConfig;
import edu.gemini.spModel.gemini.visitor.VisitorConfig$;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.gemini.visitor.VisitorPosAngleMode;
import jsky.app.ot.StaffBean;
import jsky.app.ot.gemini.editor.ComponentEditor;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyDescriptor;
import java.util.function.Function;

/**
 * User Interface for visitor instruments.
 */
public class VisitorEditor extends ComponentEditor<ISPObsComponent, VisitorInstrument> {
    private final JPanel pan = new JPanel(new GridBagLayout());

    private final TextFieldPropertyCtrl<VisitorInstrument, Double> expTimeCtrl;
    private final TextFieldPropertyCtrl<VisitorInstrument, Double> posAngleCtrl;
    private final TextFieldPropertyCtrl<VisitorInstrument, String> nameCtrl;
    private final TextFieldPropertyCtrl<VisitorInstrument, Double> wavelengthCtrl;
    private final ComboPropertyCtrl<VisitorInstrument, VisitorConfig> configCtrl;

    private static final int rightLabelCol = 1;
    private static final int rightWidgetCol= 2;
    private static final int rightUnitsCol = 3;
    private static final int colCount      = rightUnitsCol + 1;

    public VisitorEditor() {
        pan.setBorder(PANEL_BORDER);

        int row = 0;

        // Name
        PropertyDescriptor instrumentNameProp = VisitorInstrument.NAME_PROP;
        nameCtrl = TextFieldPropertyCtrl.createStringInstance(instrumentNameProp);
        nameCtrl.setColumns(30);

        nameCtrl.getTextField().addMouseListener(focusOnCaretPositionListener);
        pan.add(new JLabel(instrumentNameProp.getDisplayName()), propLabelGbc(rightLabelCol, row));
        pan.add(nameCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row, 2, 1));

        ++row;

        // Visitor Config
        final PropertyDescriptor configProp = VisitorInstrument.CONFIG_PROP;
        final Function<VisitorConfig, String> renderer = v -> (v == VisitorConfig.GenericVisitor$.MODULE$) ? "Other" : v.displayValue();
        configCtrl = new ComboPropertyCtrl<>(configProp, VisitorConfig$.MODULE$.AllArray(), renderer);

        pan.add(new JLabel("Visitor"), propLabelGbc(rightLabelCol, row));
        pan.add(configCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));

        ++row;

        // Exposure Time
        PropertyDescriptor exposureTimeProp = VisitorInstrument.EXPOSURE_TIME_PROP;
        expTimeCtrl = TextFieldPropertyCtrl.createDoubleInstance(exposureTimeProp, 1);
        expTimeCtrl.setColumns(6);
        expTimeCtrl.getTextField().addMouseListener(focusOnCaretPositionListener);

        pan.add(new JLabel("Exp Time"), propLabelGbc(rightLabelCol, row));
        pan.add(expTimeCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));
        pan.add(new JLabel("sec"), propUnitsGbc(rightUnitsCol, row));

        ++row;
        // Position Angle
        PropertyDescriptor positionAngleProp = VisitorInstrument.POS_ANGLE_PROP;
        posAngleCtrl = TextFieldPropertyCtrl.createDoubleInstance(positionAngleProp, 1);
        posAngleCtrl.setColumns(4);
        posAngleCtrl.getTextField().addMouseListener(focusOnCaretPositionListener);
        pan.add(new JLabel(positionAngleProp.getDisplayName()), propLabelGbc(rightLabelCol, row));
        pan.add(posAngleCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));
        pan.add(new JLabel("deg E of N"), propUnitsGbc(rightUnitsCol, row));

        ++row;
        // Wavelength
        PropertyDescriptor wavelengthAngleProp = VisitorInstrument.WAVELENGTH_PROP;
        wavelengthCtrl = TextFieldPropertyCtrl.createDoubleInstance(wavelengthAngleProp, 1);
        wavelengthCtrl.setColumns(6);
        wavelengthCtrl.getTextField().addMouseListener(focusOnCaretPositionListener);
        pan.add(new JLabel(wavelengthAngleProp.getDisplayName()), propLabelGbc(rightLabelCol, row));
        pan.add(wavelengthCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));
        pan.add(new JLabel("microns"), propUnitsGbc(rightUnitsCol, row));

        // Filler
        pan.add(new JPanel(), pushGbc(colCount, row + 1));

        // When the key changes, enable/disable the visitor instrument selector
        StaffBean.addPropertyChangeListener(evt -> adjustStaffOnlyFields());
        adjustStaffOnlyFields();
    }

    @Override
    public JPanel getWindow() {
        return pan;
    }

    @Override
    public void handlePostDataObjectUpdate(VisitorInstrument inst) {
        expTimeCtrl.setBean(inst);
        posAngleCtrl.setBean(inst);
        nameCtrl.setBean(inst);
        wavelengthCtrl.setBean(inst);
        configCtrl.setBean(inst);
    }

    private void adjustStaffOnlyFields() {
        // Do nothing for now.
    }

    private void updatePosAngleEnabledState(final boolean editorEnabledState) {
        posAngleCtrl.getComponent().setEnabled(
            editorEnabledState &&
                (getDataObject().getVisitorConfig().positionAngleMode() != VisitorPosAngleMode.Fixed0$.MODULE$)
        );
    }

     protected void updateEnabledState(final boolean enabled) {
        super.updateEnabledState(enabled);
        updatePosAngleEnabledState(enabled);
     }

     public void afterApply() {
        super.afterApply();
        updatePosAngleEnabledState(isEnabled());
     }

}

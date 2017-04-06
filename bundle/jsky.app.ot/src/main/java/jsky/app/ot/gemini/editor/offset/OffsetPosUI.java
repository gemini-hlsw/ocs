package jsky.app.ot.gemini.editor.offset;

import edu.gemini.spModel.guide.GuideProbe;
import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;
import java.util.Set;


/**
 * Widgets for editing an offset position.
 */
public interface OffsetPosUI {

    JPanel getPanel();
    JLabel getOrientationLabel();
    NumberBoxWidget getPOffsetTextBox();
    NumberBoxWidget getQOffsetTextBox();

    JComboBox getDefaultGuiderCombo();
    JComboBox getAdvancedGuiderCombo(GuideProbe guider);
    void setGuiders(Set<GuideProbe> guiders);
}

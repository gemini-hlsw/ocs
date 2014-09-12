//
// $Id$
//

package jsky.app.ot.progadmin;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.dataflow.GsaAspect;

import javax.swing.*;


/**
 * An editor for the GSA attributes.
 */
public final class GsaEditor implements ProgramTypeListener {
    private GsaUI ui;
    private GsaAspect model;

    public GsaEditor(GsaUI ui, ProgramTypeModel ptm) {
        this.ui = ui;

        ptm.addProgramTypeListener(this);
    }

    public void programTypeChanged(ProgramTypeEvent event) {
        Option<ProgramType> gemType = event.getNewType().getProgramType();

        ProgramTypeInfo oldType = event.getOldType();
        if ((oldType != null) && gemType.equals(oldType.getProgramType())) {
            return;
        }

        setModel(GsaAspect.getDefaultAspect(gemType));
    }

    public GsaAspect getGsaAspect() {
        int months = (Integer) ui.getMonthSpinner().getModel().getValue();
        boolean isPrivate = ui.getHeaderCheckbox().getModel().isSelected();

        boolean sendToGsa = false;
        if (model != null) sendToGsa = model.isSendToGsa();
        return new GsaAspect(sendToGsa, months, isPrivate);
    }

    public void setModel(GsaAspect model) {
        this.model = model;

        final JSpinner spinner = ui.getMonthSpinner();
        spinner.getModel().setValue(model.getProprietaryMonths());

        final JCheckBox headerCheckBox = ui.getHeaderCheckbox();
        headerCheckBox.setSelected(model.isHeaderPrivate());

        spinner.setEnabled(model.isSendToGsa());
        headerCheckBox.setEnabled(model.isSendToGsa());
    }

    public GsaAspect getModel() {
        return getGsaAspect();
    }
}

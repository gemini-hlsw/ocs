//
// $
//

package jsky.app.ot.progadmin;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;


/**
 * An editor that combines and controls all the sub-editors.
 */
final class AdminEditor {
    private ProgramTypeModel programTypeModel;
    private ProgramAttrEditor programAttrEditor;
    private TimeAcctEditor timeAcctEditor;
    private GsaEditor gsaEditor;

    public AdminEditor(AdminUI ui) {
        programTypeModel = new ProgramTypeModel();
        programAttrEditor = new ProgramAttrEditor(ui.getProgramAttrUI(), programTypeModel);
        timeAcctEditor    = new TimeAcctEditor(ui.getTimeAcctUI(), programTypeModel);
        gsaEditor         = new GsaEditor(ui.getGsaUI(), programTypeModel);
    }

    public void setModel(AdminModel model) {
        SPProgramID progId = model.getProgramAttrModel().getProgramId();
        SPProgram.ProgramMode mode = model.getProgramAttrModel().getProgramMode();
        Integer band = model.getProgramAttrModel().getQueueBand();
        if (band == null) band = 0;

        ProgramTypeInfo pk = new ProgramTypeInfo(ProgramTypeModel.getProgramType(progId), mode, band);
        programTypeModel.setProgramType(pk);

        programAttrEditor.setModel(model.getProgramAttrModel());
        timeAcctEditor.setModel(model.getTimeAcctModel());
        gsaEditor.setModel(model.getGsaAspect());
    }

    public AdminModel getModel() {
        return new AdminModel(programAttrEditor.getModel(), timeAcctEditor.getModel(), gsaEditor.getModel());
    }
}

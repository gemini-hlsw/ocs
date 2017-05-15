//
// $
//

package edu.gemini.auxfile.workflow;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;

/**
 * A builder for simple science programs for use in testing email sending.
 */
public final class ProgramBuilder {
    private final IDBDatabaseService odb;

    private SPProgramID progId;
    private String piEmails;
    private String ngoEmails;
    private String csEmails;

    public ProgramBuilder(IDBDatabaseService odb) {
        if (odb == null) throw new NullPointerException("odb == null");
        this.odb = odb;
    }

    public ProgramBuilder progId(SPProgramID progId) {
        this.progId = progId;
        return this;
    }

    public ProgramBuilder pi(String piEmails) {
        this.piEmails = piEmails;
        return this;
    }

    public ProgramBuilder ngo(String ngoEmails) {
        this.ngoEmails = ngoEmails;
        return this;
    }

    public ProgramBuilder cs(String csEmails) {
        this.csEmails = csEmails;
        return this;
    }

    public ISPProgram build() {
        ISPProgram prog;
        try {
            prog = odb.getFactory().createProgram(null, progId);

            SPProgram dataObj = (SPProgram) prog.getDataObject();
            if (ngoEmails != null) dataObj.setPrimaryContactEmail(ngoEmails);
            if (csEmails != null) dataObj.setContactPerson(csEmails);
            if (piEmails != null) {
                dataObj.setPIInfo(new SPProgram.PIInfo("Biff", "Henderson", piEmails, "123 456-7890", null));
            }
            prog.setDataObject(dataObj);
            odb.put(prog);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return prog;
    }

}

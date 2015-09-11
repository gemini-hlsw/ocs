//
// $
//

package jsky.app.ot.progadmin;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.*;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.DBIDClashException;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spModel.dataflow.GsaAspect;


import java.io.Serializable;

/**
 * A lame grab bag bean of Science Program administration properties.
 */
final class AdminModel implements Serializable {
//    private static final Logger LOG = Logger.getLogger(AdminModel.class.getName());

    private ProgramAttrModel programModel;
    private TimeAcctModel timeAcctModel;
    private GsaAspect gsaAspect;


    /**
     * Constructs and empty property model.
     */
    public AdminModel(ProgramAttrModel programModel, TimeAcctModel timeAcctModel, GsaAspect gsaAspect) {
        this.programModel  = programModel;
        this.timeAcctModel = timeAcctModel;
        this.gsaAspect     = gsaAspect;
    }

    /**
     * Constructs the property model by gathering information from various
     * places in the science program.  This method should be called from a
     * functor so that multiple trips to the ODB are avoided.
     */
    public AdminModel(ISPProgram prog)  {
        programModel  = new ProgramAttrModel(prog);
        timeAcctModel = new TimeAcctModel(prog);
        gsaAspect     = GsaAspect.lookup(prog);
    }

    public ISPProgram apply(IDBDatabaseService db, ISPProgram prog) throws DBIDClashException {
        // Update the program id, which requires going directly to the database
        // since it isn't store in the SPProgram data object.
        ISPProgram res;
        final SPProgramID progId = programModel.getProgramId();
        if ((progId != null) && (!progId.equals(prog.getProgramID()))) {
            res = db.getFactory().copyWithNewKeys(prog, progId);
        } else {
            res = prog;
        }

        // Update the Science Program data object.
        SPProgram dataObj = (SPProgram) res.getDataObject();
        programModel.apply(dataObj);

        // Update the time allocation
        timeAcctModel.apply(dataObj);

        // Don't store the default GSA Aspect.  We could I suppose, but the vast
        // majority of programs will be the default / null.
        GsaAspect cur = dataObj.getGsaAspect();
        if ((cur != null) || !gsaAspect.equals(GsaAspect.getDefaultAspect(ProgramTypeModel.getProgramType(progId)))) {
            dataObj.setGsaAspect(gsaAspect);
        }

        // Store all the changes.
        res.setDataObject(dataObj);

        // Update the TOO type.
        TooType tooType = programModel.getTooType();
        if (tooType != null) Too.set(res, tooType);

        if (prog != res) {
            db.put(res);
            db.remove(prog);
        }
        return res;
    }

    public ProgramAttrModel getProgramAttrModel() {
        return programModel;
    }

    public TimeAcctModel getTimeAcctModel() {
        return timeAcctModel;
    }

    public GsaAspect getGsaAspect() {
        return gsaAspect;
    }
}

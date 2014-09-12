//
// $
//

package jsky.app.ot.progadmin;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.ProgramType$;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.dataflow.GsaAspect;
import edu.gemini.spModel.gemini.obscomp.SPProgram;



/**
 *
 */
final class GsaModel {
    static GsaAspect getDefaultAspect(ISPProgram prog)  {
        return getDefaultAspect(prog.getProgramID());
    }

    static GsaAspect getDefaultAspect(SPProgramID progId) {
        return GsaAspect.getDefaultAspect(ProgramType$.MODULE$.readOrNull(progId));
    }

    private GsaAspect aspect;

    public GsaModel(GsaAspect aspect) {
        this.aspect = aspect;
    }

    public GsaModel(ISPProgram prog)  {
        SPProgram dataObj = (SPProgram) prog.getDataObject();
        aspect = dataObj.getGsaAspect();
        if (aspect == null) aspect = getDefaultAspect(prog);
    }

    public void apply(SPProgram dataObj, GsaAspect defaultAspect) {
        // Don't store the default values.  We could I suppose, but the vast
        // majority of programs will be the default / null.
        GsaAspect cur = dataObj.getGsaAspect();
        if ((cur == null) && aspect.equals(defaultAspect)) return;

        dataObj.setGsaAspect(aspect);
    }

    public GsaAspect getGsaAspect() {
        return aspect;
    }
}

//
// $
//

package jsky.app.ot.progadmin;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.core.ProgramType$;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.EventSupport;
import edu.gemini.spModel.gemini.obscomp.SPProgram;

import java.io.Serializable;

/**
 *
 */
final class ProgramTypeModel implements Serializable {
    private final EventSupport eventSupport =
            new EventSupport(ProgramTypeListener.class, ProgramTypeEvent.class);

    private ProgramTypeInfo programType = new ProgramTypeInfo(None.<ProgramType>instance(), SPProgram.ProgramMode.QUEUE, 0);

    public ProgramTypeModel() {
    }

    public void addProgramTypeListener(ProgramTypeListener listener) {
        eventSupport.addListener(listener);
    }

    public void removeProgramTypeListener(ProgramTypeListener listener) {
        eventSupport.removeListener(listener);
    }

    public ProgramTypeInfo getProgramType() {
        return programType;
    }

    public void setProgramType(ProgramTypeInfo newType) {
        if (programType.equals(newType)) return;

        ProgramTypeInfo oldType = programType;
        programType = newType;
        eventSupport.fireEvent(new ProgramTypeEvent(this, oldType, newType), "programTypeChanged");
    }

    public static Option<ProgramType> getProgramType(SPProgramID id) {
        return id == null ? None.<ProgramType>instance() : ImOption.apply(ProgramType$.MODULE$.readOrNull(id));
    }
}

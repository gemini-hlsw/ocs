//
// $
//

package jsky.app.ot.progadmin;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.gemini.obscomp.SPProgram;

/**
 * A tuple of the ProgramType, program mode, and queue band.  These bits of
 * information are required by the various admin editors.
 */
public final class ProgramTypeInfo {
    private final Option<ProgramType> programType;
    private final SPProgram.ProgramMode mode;
    private final int queueBand;

    public ProgramTypeInfo(Option<ProgramType> type, SPProgram.ProgramMode mode, int band) {
        if (type == null) throw new NullPointerException("type == null");
        this.programType = type;
        this.mode        = mode;
        this.queueBand   = band;
    }

    public Option<ProgramType> getProgramType() {
        return programType;
    }

    public ProgramTypeInfo withProgramType(Option<ProgramType> type) {
        return new ProgramTypeInfo(type, mode, queueBand);
    }

    public SPProgram.ProgramMode getMode() {
        return mode;
    }

    public ProgramTypeInfo withMode(SPProgram.ProgramMode mode) {
        return new ProgramTypeInfo(programType, mode, queueBand);
    }

    public int getQueueBand() {
        return queueBand;
    }

    public ProgramTypeInfo withQueueBand(int band) {
        return new ProgramTypeInfo(programType, mode, band);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ProgramTypeInfo that = (ProgramTypeInfo) o;

        if (queueBand != that.queueBand) return false;
        if (mode != that.mode) return false;
        return programType.equals(that.programType);
    }

    public int hashCode() {
        int result;
        result = programType.hashCode();
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        result = 31 * result + queueBand;
        return result;
    }
}

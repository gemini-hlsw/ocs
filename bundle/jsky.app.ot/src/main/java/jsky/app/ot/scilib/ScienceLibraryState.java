package jsky.app.ot.scilib;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;

public final class ScienceLibraryState {
    public final Site site;
    public final SPComponentType inst;
    public final SPProgramID pid;
    public final boolean checkedOut;

    public ScienceLibraryState(Site site, SPComponentType inst, SPProgramID pid, boolean checkedOut) {
        if (site == null) throw new NullPointerException("site == null");
        if (inst == null) throw new NullPointerException("inst == null");
        if (pid  == null) throw new NullPointerException("pid == null");

        this.site       = site;
        this.inst       = inst;
        this.pid        = pid;
        this.checkedOut = checkedOut;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ScienceLibraryState that = (ScienceLibraryState) o;
        if (checkedOut != that.checkedOut) return false;
        if (inst != that.inst) return false;
        if (!pid.equals(that.pid)) return false;
        if (site != that.site) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = site.hashCode();
        result = 31 * result + inst.hashCode();
        result = 31 * result + pid.hashCode();
        result = 31 * result + (checkedOut ? 1 : 0);
        return result;
    }
}

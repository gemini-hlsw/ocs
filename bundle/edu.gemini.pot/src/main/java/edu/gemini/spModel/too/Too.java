package edu.gemini.spModel.too;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.SPObservation;

/**
 * Gross procedural code for working with ToOs.
 */
public final class Too {

    private Too() {}

    /**
     * Gets the ToO state of the program in which the given node resides.
     * Note that the observation ToO state is irrelevant here.  Every
     * observation in a ToO program is ToO regardless of the individual
     * observation ToO state.
     *
     * @return <code>true</code> if the program in which the node resides is
     * a ToO program
     */
    public static boolean isToo(ISPNode node) {
        return ((SPProgram) node.getProgram().getDataObject()).isToo();
    }

    /**
     * Gets the ToO type of the given program, which is the same as getting its
     * data object and getting the TooType from that.
     */
    public static TooType get(ISPProgram prog) {
        return ((SPProgram) prog.getDataObject()).getTooType();
    }

    /**
     * Gets the ToO type of the given observation which may differ from the
     * type of the program iff the program type is rapid and the observation
     * type is standard.
     */
    public static TooType get(ISPObservation obs) {
        final TooType progType = get(obs.getProgram());
        if (progType == TooType.rapid) {
            final boolean override = ((SPObservation) obs.getDataObject()).isOverrideRapidToo();
            return override ? TooType.standard : TooType.rapid;
        } else {
            return progType;
        }
    }

    /**
     * Sets the ToO type of the program and all of its observations to match.
     */
    public static void set(ISPProgram prog, TooType newType) {
        // REL-538 Move TooType to SPProgram
        final SPProgram spProgram = (SPProgram) prog.getDataObject();
        if (newType != spProgram.getTooType()) {
            spProgram.setTooType(newType);
            prog.setDataObject(spProgram);
        }
    }
}

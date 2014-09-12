package edu.gemini.spdb.shell.misc.deactivate;

import edu.gemini.spModel.core.SPProgramID;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This class encapsulates a program ID and a property.
 * Example uses are a string to attach to the ID, or a boolean indicating the activation state.
 */
final class SPProgramIDProperty<T> implements Serializable {

    static final Comparator<SPProgramIDProperty> ID_COMPARATOR = new Comparator<SPProgramIDProperty>() {
        public int compare(final SPProgramIDProperty a, final SPProgramIDProperty b){
            return a.id.compareTo(b.id);
        }
    };

    final SPProgramID id;
    final T property;

    SPProgramIDProperty(final SPProgramID id, final T property){
        this.id=id;
        this.property=property;
    }

}

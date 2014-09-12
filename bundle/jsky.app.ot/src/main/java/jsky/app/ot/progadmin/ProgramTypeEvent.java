//
// $
//

package jsky.app.ot.progadmin;

import java.util.EventObject;

/**
 *
 */
public final class ProgramTypeEvent extends EventObject {
    private ProgramTypeInfo oldType;
    private ProgramTypeInfo newType;

    public ProgramTypeEvent(Object source, ProgramTypeInfo oldType, ProgramTypeInfo newType) {
        super(source);
        this.oldType = oldType;
        this.newType = newType;
    }

    public ProgramTypeInfo getOldType() {
        return oldType;
    }

    public ProgramTypeInfo getNewType() {
        return newType;
    }
}

package edu.gemini.qpt.ui.find;

import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.sp.Prog;

class FindElement implements Comparable<FindElement> {
    
    private final Comparable<?> target;
    private final Object error;
        
    FindElement(final Prog target, final Object error) {
        if (target == null) throw new IllegalArgumentException("Target may not be null.");
        this.target = target;
        this.error = error;
    }
    
    FindElement(final Obs target, final Object error) {
        if (target == null) throw new IllegalArgumentException("Target may not be null.");
        this.target = target;
        this.error = error;
    }
    
    @SuppressWarnings("unchecked")
    public int compareTo(FindElement o) {
        
        Object a = target;
        Object b = o.target;

        if (a instanceof Prog && b instanceof Prog) return ((Prog) a).compareTo((Prog) b);
        if (a instanceof Obs  && b instanceof Obs)  return ((Obs)  a).compareTo((Obs)  b);
        
        if (a instanceof Prog) return ((Prog) a).compareTo(((Obs) b).getProg());
        return ((Obs) a).getProg().compareTo((Prog) b);
        
    }

    public Object getError() {
        return error;
    }

    public Object getTarget() {
        return target;
    }
    
}

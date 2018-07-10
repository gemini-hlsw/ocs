package edu.gemini.qpt.core.util;

import java.util.SortedSet;

import edu.gemini.qpt.core.Alloc;

@SuppressWarnings("serial")
public class Variants {

    public static abstract class EditException extends Exception {

        public EditException(String message) {
            super(message);
        }
        
    }
    
    /**
     * Thrown when attempting to add an Alloc with the same Obs and first step as
     * an existing Alloc.
     * @author rnorris
     */
    public static final class CollisionException extends EditException {        
        public final Alloc oldAlloc, newAlloc;
        public CollisionException(final Alloc oldAlloc, final Alloc newAlloc) {
            super(newAlloc + " would collide with " + oldAlloc);
            this.oldAlloc = oldAlloc;
            this.newAlloc = newAlloc;
        }        
    }
    
    /**
     * Throw when attempting to add an Alloc whose predecessor is not present.
     * @author rnorris
     */
    public static final class MissingPredecessorException extends EditException {    
        public final Alloc alloc;        
        public MissingPredecessorException(final Alloc alloc) {
            super(alloc + " requires a predecessor");
            this.alloc = alloc;
        }        
    }

    /**
     * Thrown when attempting to add an Alloc whose start time falls before its
     * predecessor's end time.
     * @author rnorris
     */
    public static final class OrderingException extends EditException {
        public final Alloc alloc;
        public final long earliestStartTime;
        public OrderingException(final Alloc alloc, final long earliestStartTime) {
            super(alloc + " starts before its predecessr ends; earliest allowed start time is " + earliestStartTime);
            this.alloc = alloc;
            this.earliestStartTime = earliestStartTime;
        }
    }
    

    /**
     * Thrown when attempting to remove an Alloc that has a successor.
     * @author rnorris
     */
    public static final class AbandonedSuccessorException extends EditException {    
        public final Alloc alloc;
        public AbandonedSuccessorException(final Alloc alloc) {
            super(alloc + " would be abandoned because its predecessor would be missing");
            this.alloc = alloc;
        }        
    }
    
    
    public static void tryAdd(SortedSet<Alloc> existing, Alloc toAdd) throws CollisionException, MissingPredecessorException, OrderingException {

        // Check for simple collision first
        for (Alloc a: existing)
            if (a.getObs() == toAdd.getObs() && a.getFirstStep() == toAdd.getFirstStep())
                throw new CollisionException(a, toAdd);
    
        // If a needs a predecessor, look for it
        if (toAdd.getFirstStep() > toAdd.getObs().getFirstUnexecutedStep()) {
            Alloc pred = null;
            for (Alloc a: existing) {
                if (a.getObs() == toAdd.getObs() && a.getLastStep() == toAdd.getFirstStep() - 1) {
                    pred = a;
                    break;
                }
            }
            if (pred == null) throw new MissingPredecessorException(toAdd);
            if (pred.getEnd() > toAdd.getStart()) throw new OrderingException(toAdd, pred.getEnd());
        }
        
    }
    
    public static void tryRemove(SortedSet<Alloc> existing, Alloc toRemove) throws AbandonedSuccessorException {
        for (Alloc a: existing)
            if (a.getObs() == toRemove.getObs() && a.getFirstStep() > toRemove.getLastStep())
                throw new AbandonedSuccessorException(a);                
    }
    
    
    
    
}

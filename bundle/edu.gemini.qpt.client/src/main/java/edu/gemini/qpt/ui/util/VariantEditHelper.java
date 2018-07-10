package edu.gemini.qpt.ui.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.util.Variants;
import edu.gemini.qpt.core.util.Variants.AbandonedSuccessorException;
import edu.gemini.qpt.core.util.Variants.CollisionException;
import edu.gemini.qpt.core.util.Variants.EditException;
import edu.gemini.qpt.core.util.Variants.MissingPredecessorException;
import edu.gemini.qpt.core.util.Variants.OrderingException;
import edu.gemini.ui.gface.GSelection;

public class VariantEditHelper {

    private static final Logger LOGGER = Logger.getLogger(VariantEditHelper.class.getName());
    
    private final Component parentComponent;
    
    public VariantEditHelper(final Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    /**
     * Attempt to remove <code>which</code> Allocs from <code>variant</code>. If some can't be
     * removed, the user will be prompted to skip or cancel. This method returns a (possibly empty)
     * collection of Allocs that were actually removed.
     * @param variant
     * @param which
     * @return a Collection of Allocs that were cut
     * @throws CancelledException if the user cancels the cut operation
     */
    public GSelection<Alloc> cut(Variant variant, GSelection<Alloc> which) throws CancelledException {
        // Try to delete in reverse natural order
        List<Alloc> allocs = new LinkedList<Alloc>();
        for (Alloc a: which) allocs.add(a);
        Collections.sort(allocs, Collections.reverseOrder());
        SortedSet<Alloc> target = variant.getAllocs();
        for (Iterator<Alloc> it = allocs.iterator(); it.hasNext(); ) {
            Alloc toRemove = it.next();
            try {
                Variants.tryRemove(target, toRemove);
                target.remove(toRemove);
            } catch (AbandonedSuccessorException e) {
                
                int ret = JOptionPane.showOptionDialog(
                        parentComponent, 
                        e.alloc + " has a successor and cannot be deleted.",
                        "Resolve Abandoned Successor", 
                        JOptionPane.DEFAULT_OPTION, 
                        JOptionPane.QUESTION_MESSAGE, 
                        null, 
                        AbandonedSuccessorResolution.values(), 
                        AbandonedSuccessorResolution.Skip.ordinal());
                
                switch (AbandonedSuccessorResolution.values()[ret]) {
                case Cancel:
                    throw new CancelledException();
                case Skip:
                    it.remove();
                    break;
                default:
                    throw new Error("Impossible!");
                }
            }
        }
        for (Alloc a: allocs)
            try {
                a.remove();
            } catch (AbandonedSuccessorException e) {
                // should never happen since we just tried it above
                LOGGER.log(Level.SEVERE, "This should never happen.", e);
                throw new RuntimeException(e);//?
            }
        Collections.sort(allocs); // natural order this time
        return new GSelection<Alloc>(allocs.toArray(new Alloc[allocs.size()]));
    }
    
    private enum AbandonedSuccessorResolution {
        Skip, Cancel
    }
    
    /**
     * Attempts to add the allocs in <code>paste</code> to <code>variant</code>, allowing the user to 
     * resolve any conflicts that are caused or cancel the operation entirely.
     * @param target
     * @param paste
     * @return the Allocs actually pasted
     * @throws CancelledException 
     */
    public GSelection<Alloc> paste(Variant target, GSelection<Alloc> paste) throws CancelledException {
        SortedSet<Alloc> pasteSet = new TreeSet<Alloc>();
        for (Alloc a: paste) pasteSet.add(a);
        return paste(target, pasteSet);
    }
    
    private GSelection<Alloc> paste(Variant target, SortedSet<Alloc> paste) throws CancelledException {
        List<Alloc> pasted = new ArrayList<Alloc>();
        List<Edit> edits = new ArrayList<Edit>();
        SortedSet<Alloc> allocs = target.getAllocs(); // this is a new collection for us; we can edit it
        for (Alloc a: paste) 
            add(edits, allocs, a);
        for (Edit e: edits) {
            e.invoke(target, paste);
            if (e.opcode == Edit.Opcode.ADD_TO_TARGET)
                pasted.add(e.alloc);
        }
        return new GSelection<Alloc>(pasted.toArray(new Alloc[pasted.size()]));
    }
    
    private void add(List<Edit> edits, SortedSet<Alloc> target, Alloc alloc) throws CancelledException {
        try {
            Variants.tryAdd(target, alloc);
            edits.add(new Edit(Edit.Opcode.ADD_TO_TARGET, alloc));
        } catch (CollisionException e) {
            
            int ret = JOptionPane.showOptionDialog(
                    parentComponent, 
                    "There is already a visit starting at step " + alloc.getFirstStep() + " of " + alloc.getObs() + ", do you want to replace it?", 
                    "Resolve Collision", 
                    JOptionPane.DEFAULT_OPTION, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    CollisionResolution.values(), 
                    CollisionResolution.Replace.ordinal());
            
            switch (CollisionResolution.values()[ret]) {
            case Cancel: 
                throw new CancelledException();
            case Replace: 
                target.remove(e.oldAlloc);
                edits.add(new Edit(Edit.Opcode.REMOVE_FROM_TARGET, e.oldAlloc));
                edits.add(new Edit(Edit.Opcode.ADD_TO_TARGET, e.newAlloc));
                break;
            case Skip:
                edits.add(new Edit(Edit.Opcode.REMOVE_FROM_PASTE, e.newAlloc));
                break;
            default:
                throw new Error("Impossible");
            }
            
        } catch (MissingPredecessorException e) {
            
            int ret = JOptionPane.showOptionDialog(
                    parentComponent, 
                    "The visit starting at step " + (alloc.getFirstStep() + 1) + " of " + alloc.getObs() + " requires a predecessor which is not present.", 
                    "Resolve Missing Predecessor", 
                    JOptionPane.DEFAULT_OPTION, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    MissingPredecessorResolution.values(), 
                    MissingPredecessorResolution.Skip.ordinal());

            switch (MissingPredecessorResolution.values()[ret]) {
            case Cancel: 
                throw new CancelledException();
            case Skip:
                edits.add(new Edit(Edit.Opcode.REMOVE_FROM_PASTE, e.alloc));
                break;
            default:
                throw new Error("Impossible");
            }
            
        } catch (OrderingException e) {
            
            
            int ret = JOptionPane.showOptionDialog(
                    parentComponent, 
                    alloc + " must start after its predecessor, do you want to move it forward?", 
                    "Resolve Ordering Problem", 
                    JOptionPane.DEFAULT_OPTION, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    OrderingResolution.values(), 
                    OrderingResolution.Move.ordinal());
            
            switch (OrderingResolution.values()[ret]) {
            case Cancel: 
                throw new CancelledException();
            case Move: 
                edits.add(new Edit(Edit.Opcode.ADD_TO_TARGET, e.alloc, e.earliestStartTime));
                break;
            case Skip:
                edits.add(new Edit(Edit.Opcode.REMOVE_FROM_PASTE, e.alloc));
                break;
            default:
                throw new Error("Impossible");
            }
            
        }
    }
    
    private enum OrderingResolution {
        Move, Skip, Cancel
    }
    
    private enum MissingPredecessorResolution {
        Skip, Cancel
    }
    
    private enum CollisionResolution {
        Replace, Skip, Cancel
    }
    
    private static class Edit {
        enum Opcode { REMOVE_FROM_TARGET, REMOVE_FROM_PASTE, ADD_TO_TARGET }
        public final Opcode opcode;
        public final Alloc alloc;
        public final long start;
        public Edit(final Opcode opcode, final Alloc alloc) {
            this(opcode, alloc, alloc.getStart());
        }
        public Edit(final Opcode opcode, final Alloc alloc, final long start) {
            this.opcode = opcode;
            this.alloc = alloc;
            this.start = start;
        }
        void invoke(Variant target, SortedSet<Alloc> paste) {
            switch (opcode) {
            case ADD_TO_TARGET: 
                
                try {
                
                    target.addAlloc(
                        alloc.getObs(), 
                        start, // ours; usally == alloc.getStart() but not always
                        alloc.getFirstStep(), 
                        alloc.getLastStep(), 
                        alloc.getSetupType(),
                        alloc.getComment());

                } catch (EditException e) {
                    // should never happen since we just tried it above
                    LOGGER.log(Level.SEVERE, "This should never happen.", e);
                    throw new RuntimeException(e);//?
                }
                
                break;
            case REMOVE_FROM_PASTE: paste.remove(alloc); break;
            case REMOVE_FROM_TARGET: 
                    try {
                        alloc.remove();
                    } catch (AbandonedSuccessorException e) {
                        // should never happen since we just tried it above
                        LOGGER.log(Level.SEVERE, "This should never happen.", e);
                        throw new RuntimeException(e);//?
                    } 
                
                break;
            default: throw new Error("Impossible!");
            }
        }
    }
    
}

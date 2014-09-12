package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.util.ReadableNodeName;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.nsp.SPTreeEditUtil.PendingUpdate;
import jsky.app.ot.nsp.SPTreeEditUtil.PendingPasteUpdate;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.SPViewerActions;
import jsky.util.gui.ClipboardHelper;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: rnorris
 * Date: 1/17/13
 * Time: 2:01 PM
 */
public final class PasteAction extends AbstractViewerAction implements ChangeListener {
    public PasteAction(SPViewer viewer) {
        super(viewer, "Paste", jsky.util.Resources.getIcon("Paste24.gif", SPViewerActions.class));
        putValue(SHORT_DESCRIPTION, "Paste a tree node from the clipboard at the selected position.");
        ClipboardHelper.addChangeListener(this);
    }

    // If we are going to paste over something, get confirmation first.
    // Construct a somewhat useful message depending on what will happen.
    private boolean verifyUpdate(List<PendingUpdate> pus) {
        final List<PendingPasteUpdate> ppus = new ArrayList<PendingPasteUpdate>();
        for (PendingUpdate pu : pus) {
            if (pu instanceof PendingPasteUpdate)
                ppus.add((PendingPasteUpdate) pu);
        }
        final boolean okToContinue;
        if (ppus.size() == 0) {
            okToContinue = true;
        } else {
            final String question;
            if (ppus.size() == 1) {
                final PendingPasteUpdate ppu = ppus.get(0);
                question = "Replace the " + ppu.target.getType().readableStr + " in " +
                        ReadableNodeName.format(ppu.target.getParent()) + "?";
            } else {
                final Set<SPComponentType> types = new HashSet<SPComponentType>();
                for (PendingPasteUpdate ppu : ppus)
                    types.add(ppu.target.getType());
                if (types.size() == 1) {
                    question = "Replace the " + types.iterator().next().readableStr +
                            " in multiple locations?";
                } else {
                    question = "Replace existing components of the same type?";
                }
            }
            okToContinue = DialogUtil.confirm(null, question) == JOptionPane.OK_OPTION;
        }
        return okToContinue;
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            final List<PendingUpdate> pus = getUpdates();
            if (verifyUpdate(pus)) {
                for (SPTreeEditUtil.PendingUpdate pu : pus) pu.apply();
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    private List<PendingUpdate> getUpdates() {
        final ISPProgram prog = viewer.getProgram();
        if (prog == null) return Collections.emptyList();

        final Object clipboardContent = ClipboardHelper.getClipboard();
        if (!(clipboardContent instanceof ISPNode[]))
            return Collections.emptyList();

        final List<SPTreeEditUtil.PendingUpdate> ups = new ArrayList<SPTreeEditUtil.PendingUpdate>();
        final ISPNode[] locs = viewer.getTree().getSelectedNodes();
        if (locs != null) {
            final ISPNode[] nodes = (ISPNode[]) clipboardContent;
            for (ISPNode loc : locs) {
                final List<SPTreeEditUtil.PendingUpdate> tmp = SPTreeEditUtil.getUpdates(prog, loc, nodes);
                if (tmp.size() == 0) return Collections.emptyList();
                else ups.addAll(tmp);
            }
        }
        return ups;
    }

    @Override
    public boolean computeEnabledState() {
        return !getUpdates().isEmpty();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        setEnabled(computeEnabledState());
    }
}

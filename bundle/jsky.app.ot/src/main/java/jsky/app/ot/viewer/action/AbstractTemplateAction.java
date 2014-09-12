package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPTemplateFolder;
import edu.gemini.pot.sp.ISPTemplateGroup;
import jsky.app.ot.viewer.SPViewer;

import javax.swing.Icon;

/**
 * Created with IntelliJ IDEA.
 * User: rnorris
 * Date: 1/29/13
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractTemplateAction extends AbstractViewerAction {

    public AbstractTemplateAction(SPViewer viewer, String name, Icon icon) {
        super(viewer, name, icon);
    }

    public AbstractTemplateAction(SPViewer viewer, String name) {
        super(viewer, name);
    }

    protected boolean isTemplateGroup() {
        return getContextNode(ISPNode.class) instanceof ISPTemplateGroup;
    }

    protected boolean isTemplateFolder() {
        return getContextNode(ISPNode.class) instanceof ISPTemplateFolder;
    }

    protected boolean isTemplateEnabled() {
        final ISPProgram prog = getProgram();
        return (prog != null) && (prog.getTemplateFolder() != null);
    }

}

package edu.gemini.spModel.template;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Functor that forks off a copy of a template group.
 */
public class SplitFunctor extends DBAbstractFunctor {

    private static final Logger LOGGER = Logger.getLogger(SplitFunctor.class.getName());

    private final ISPTemplateGroup node;
    private final Set<ISPTemplateParameters> toMove = new HashSet<ISPTemplateParameters>();

    public SplitFunctor(ISPTemplateGroup node) {
        this.node = node;
    }

    public void add(ISPTemplateParameters parameters) {
        toMove.add(parameters);
    }

    public void execute(IDBDatabaseService db, ISPNode ignored, Set<Principal> principals) {
        try {
            final ISPTemplateFolder templateFolder = (ISPTemplateFolder) node.getParent();

            // Copy
            final ISPTemplateGroup templateGroupCopy = split(db.getFactory());

            // Hook it up and we're done
            templateFolder.addTemplateGroup(templateGroupCopy);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Trouble during split.", e);
            setException(e);
        }
    }

    public ISPTemplateGroup split(ISPFactory factory) throws SPException {
        // Copy
        final ISPTemplateGroup templateGroupCopy = factory.createTemplateGroupCopy(node.getProgram(), node, false);

        // Remove params from copy; we'll move some over later
        for (ISPTemplateParameters ps: templateGroupCopy.getTemplateParameters())
            templateGroupCopy.removeTemplateParameters(ps);

        // Ok move params
        templateGroupCopy.setTemplateParameters(Collections.<ISPTemplateParameters>emptyList());
        for (ISPTemplateParameters ps: toMove) {
            node.removeTemplateParameters(ps);
            templateGroupCopy.addTemplateParameters(ps);
        }

        // Split the version token
        final TemplateGroup tg = (TemplateGroup) node.getDataObject();
        final TemplateGroup tgc = (TemplateGroup) templateGroupCopy.getDataObject();
        tgc.setVersionToken(tg.getVersionToken().next());

        // Reset data objects (must do BOTH because next() above is a mutator)
        node.setDataObject(tg);
        templateGroupCopy.setDataObject(tgc);

        return templateGroupCopy;
    }
}

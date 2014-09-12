package edu.gemini.phase2.core.odb;

import edu.gemini.phase2.core.model.TemplateFolderExpansion;
import edu.gemini.phase2.core.odb.TemplateFolderService.BaselineOption;
import edu.gemini.phase2.core.odb.TemplateFolderService.TemplateOption;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPException;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;


import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Wraps the TemplateFolderService in an ODB functor for use from remote
 * clients such as the OT.  If using the TemplateFolderService from another
 * functor, then it is highly recommended to directly work with the
 * TemplateFolderService.
 */
public final class TemplateFolderFunctor extends DBAbstractFunctor {
    public static final Logger LOG = Logger.getLogger(TemplateFolderFunctor.class.getName());

    public final TemplateFolderExpansion expansion;
    public final TemplateOption templateOption;
    public final BaselineOption baselineOption;

    public TemplateFolderFunctor(TemplateFolderExpansion expansion, TemplateOption templateOption, BaselineOption baselineOption) {
        if (expansion == null) throw new IllegalArgumentException("expansion == null");
        if (templateOption == null) throw new IllegalArgumentException("templateOption == null");
        if (baselineOption == null) throw new IllegalArgumentException("baselineOption == null");

        this.expansion      = expansion;
        this.templateOption = templateOption;
        this.baselineOption = baselineOption;
    }

    @Override public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        ISPProgram prog = (ISPProgram) node;
        TemplateFolderService.store(expansion, templateOption, baselineOption, prog, db.getFactory());
    }

    public static void store(TemplateFolderExpansion expansion, TemplateOption templateOption, BaselineOption baselineOption, ISPProgram prog, IDBDatabaseService odb, Set<Principal> user) throws SPException {
        TemplateFolderFunctor fun = new TemplateFolderFunctor(expansion, templateOption, baselineOption);
        odb.getQueryRunner(user).execute(fun, prog);
    }
}

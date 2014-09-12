package edu.gemini.spModel.template;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.gemini.security.UserRolePrivileges;
import edu.gemini.spModel.obs.SPObservation;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.gemini.spModel.template.FunctorHelpers.lookupNode;

/**
 * Created by IntelliJ IDEA.
 * User: rnorris
 * Date: 6/4/12
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class OriginatingTemplateFunctor extends DBAbstractFunctor {

    private static final Logger LOGGER = Logger.getLogger(ReapplicationCheckFunctor.class.getName());

    private final UserRolePrivileges urps;

    private boolean isTemplateDerived;
    private ISPObservation templateObservation;
    private SPObservation templateObservationDataObject;
    private boolean canReapply;

    public OriginatingTemplateFunctor(UserRolePrivileges urps) {
        this.urps = urps;
    }

    public void execute(IDBDatabaseService db, ISPNode obs, Set<Principal> principals) {
        try {
            if (obs instanceof ISPObservation) {
                final SPObservation dataObj = (SPObservation) obs.getDataObject();
                final SPNodeKey templateKey = dataObj.getOriginatingTemplate();
                if (templateKey != null) {
                    isTemplateDerived = true;
                    templateObservation = (ISPObservation) lookupNode(templateKey, db.lookupProgram(obs.getProgramKey()));
                    if (templateObservation != null) {
                        templateObservationDataObject = (SPObservation) templateObservation.getDataObject();
                        canReapply = ReapplicationCheckFunctor.canReapply(db, urps, (ISPObservation) obs);
                    }
                }
            } else {
                throw new IllegalArgumentException("Not an observation: " + obs);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Trouble during template reapply check.", e);
            setException(e);
        }
    }

    public ISPObservation getTemplateObservation() {
        return templateObservation;
    }

    public SPObservation getTemplateObservationDataObject() {
        return templateObservationDataObject;
    }

    public boolean isTemplateDerived() {
        return isTemplateDerived;
    }

    public boolean canReapply() {
        return canReapply;
    }

}





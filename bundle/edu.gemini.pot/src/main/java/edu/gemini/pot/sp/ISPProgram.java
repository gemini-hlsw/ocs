// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ISPProgram.java 46987 2012-07-25 22:04:59Z rnorris $
//

package edu.gemini.pot.sp;

import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.shared.util.VersionVector;


/**
 * This is the interface for the root Science Program node.  Note that
 * as an <code>{@link ISPContainerNode}</code>, the <code>ISPProgram</code>
 * may accept structure listeners.  See the <code>ISPContainerNode</code>
 * class description for more detail.
 */
public interface ISPProgram extends ISPGroupContainer, ISPObservationContainer,
        ISPObsComponentContainer, ISPContainerNode, ISPProgramNode, ISPRootNode {

    /**
     * Names the property change event fired when the program id is modified.
     */
    String PROGRAM_ID_PROP = "ProgramID";

    /**
     * Names the property in the {@link SPStructureChange} object delivered
     * when a template folder is added or removed.
     */
    String TEMPLATE_FOLDER_PROP = "TemplateFolder";

    /**
     * Gets the (possibly null) template folder associated with this program.
     */
    ISPTemplateFolder getTemplateFolder() ;

    /**
     * Sets the templates to be associated with this program.
     *
     * @param folder if <code>null</code> templates are removed
     */
    void setTemplateFolder(ISPTemplateFolder folder) throws SPNodeNotLocalException, SPTreeStateException;

    scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> getVersions();
    void setVersions(scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> m);

    VersionVector<LifespanId, Integer> getVersions(SPNodeKey key);
    void setVersions(SPNodeKey key, VersionVector<LifespanId, Integer> vv);
}


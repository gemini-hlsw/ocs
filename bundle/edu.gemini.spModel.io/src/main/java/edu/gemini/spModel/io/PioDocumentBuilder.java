package edu.gemini.spModel.io;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.Encrypted;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.io.impl.ConflictPio;
import edu.gemini.spModel.io.impl.Encryption;
import edu.gemini.spModel.io.impl.SpIOTags;
import edu.gemini.spModel.io.impl.VersionVectorPio;
import edu.gemini.spModel.pio.*;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a {@link Document PIO document} from a {@link ISPProgram program} or
 * {@link edu.gemini.pot.sp.ISPNightlyRecord plan}.
 */
public enum PioDocumentBuilder {
    instance;

    private static final Logger LOG = Logger.getLogger(PioDocumentBuilder.class.getName());

    public Document toDocument(ISPNode node) {
        final PioFactory factory = new PioXmlFactory();
        final Document doc = factory.createDocument();
        _addContainer(factory, doc, node, doc);
        if (node instanceof ISPProgram) {
            doc.addContainer(VersionVectorPio.toContainer(factory, ((ISPProgram) node).getVersions()));
        }
        return doc;
    }

    private String _getProgId(ISPNode node)  {
        SPProgramID progId = node.getProgramID();
        if (progId != null) {
            return progId.stringValue();
        }
        return "";
    }

    private String _getObservationId(ISPObservation node)  {
        SPObservationID obsId = node.getObservationID();
        if (obsId != null) {
            return obsId.stringValue();
        }
        return "";
    }

    private ParamSet encryptedParamSet(SPNodeKey key, PioFactory factory, ParamSet in) {
        try {
            final String clearText = PioXmlUtil.toXmlString(in);
            final String encrypted = Encryption.encrypt(key, clearText);

            final ParamSet res = factory.createParamSet(in.getName());
            res.setKind(in.getKind());
            Pio.addParam(factory, res, Encryption.ParamName(), encrypted);
            return res;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not encrypt param set", ex);
            throw new RuntimeException(ex);
        }
    }

    // Add a container element. The node should be the one corresponding to the data object.
    // The new element will be added under the given parent element.
    private void _addContainer(PioFactory factory, Document doc, ISPNode node, ContainerParent parent) {
        ISPDataObject dataObject = node.getDataObject();

        Container container = factory.createContainer("", "", "");
        _addContainerAttributes(node, dataObject, container);

        // Add a paramset element for the node's data object
        final SPNodeKey key = node.getNodeKey();
        final ParamSet paramSet = dataObject.getParamSet(factory);
        if (dataObject instanceof Encrypted) {
            container.addParamSet(encryptedParamSet(key, factory, paramSet));
        } else {
            container.addParamSet(paramSet);
        }

        // Add a paramset element for any conflicts associated with the node
        final Conflicts conflicts = node.getConflicts();
        if (!conflicts.isEmpty()) {
            final ParamSet cParamSet = ConflictPio.toParamSet(factory, conflicts);
            if (dataObject instanceof Encrypted) {
                container.addParamSet(encryptedParamSet(key, factory, cParamSet));
            } else {
                container.addParamSet(cParamSet);
            }
        }

        // Add elements for the user objects
        //_addUserObjects(factory, node, container);

        parent.addContainer(container);

        // Add elements for the sub-nodes
        _addSubNodes(factory, doc, node, container);
    }


    // Add attributes for the given node and data object to the given container element
    private void _addContainerAttributes(ISPNode node, ISPDataObject dataObject, Container container) {
        SPComponentType t = dataObject.getType();

        container.setKind(_getNodeKind(node));
        container.setType(t.broadType.value);
        container.setVersion(dataObject.getVersion());
        container.setSubtype(t.narrowType);
        if (node != null) {
            container.setKey(node.getNodeKey().toString());
        }
        if (node instanceof ISPProgram || node instanceof ISPNightlyRecord) {
            container.setName(_getProgId(node));
        } else if (node instanceof ISPObservation) {
            container.setName(_getObservationId((ISPObservation)node));
        } else {
            container.setName(t.readableStr);
        }
    }

    // Add container elements to the given element for the sub-nodes of the given node.
    private void _addSubNodes(PioFactory factory, Document doc, ISPNode node, Container container) {
        if (node instanceof ISPContainerNode) {
            List l = ((ISPContainerNode) node).getChildren();
            if (l != null) {
                Iterator it = l.iterator();
                while (it.hasNext()) {
                    ISPNode sub = (ISPNode) it.next();
                    _addContainer(factory, doc, sub, container);
                }
            }
        }
    }

    // Return a string to use for the "kind" attribute for the given node
    private String _getNodeKind(ISPNode node) {
        if (node == null) return SpIOTags.USEROBJ;
        if (node instanceof ISPProgram) return SpIOTags.PROGRAM;
        if (node instanceof ISPConflictFolder) return SpIOTags.CONFLICT_FOLDER;
        if (node instanceof ISPTemplateFolder) return SpIOTags.TEMPLATE_FOLDER;
        if (node instanceof ISPTemplateGroup) return SpIOTags.TEMPLATE_GROUP;
        if (node instanceof ISPTemplateParameters) return SpIOTags.TEMPLATE_PARAMETERS;
        if (node instanceof ISPGroup) return SpIOTags.GROUP;
        if (node instanceof ISPNightlyRecord) return SpIOTags.NIGHTLY_PLAN;
        if (node instanceof ISPObservation) return SpIOTags.OBSERVATION;
        if (node instanceof ISPObsQaLog) return SpIOTags.OBS_QA_LOG;
        if (node instanceof ISPObsExecLog) return SpIOTags.OBS_EXEC_LOG;
        if (node instanceof ISPObsComponent) return SpIOTags.OBSCOMP;
        if (node instanceof ISPSeqComponent) return SpIOTags.SEQCOMP;
        throw new RuntimeException("unknown node type: " + node.getClass().getName());
    }
}

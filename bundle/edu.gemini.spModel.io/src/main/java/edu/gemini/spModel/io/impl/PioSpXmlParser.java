package edu.gemini.spModel.io.impl;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.version.JavaVersionMapOps;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.shared.util.VersionVector;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.Encrypted;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.init.ObservationNI;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.phase1.GsaPhase1Data;
import edu.gemini.spModel.io.impl.migration.to2009B.To2009B;
import edu.gemini.spModel.io.impl.migration.to2010B.ToGnirsAtGn;
import edu.gemini.spModel.io.impl.migration.to2014A.AddMissingStaticInstrumentParams;
import edu.gemini.spModel.io.impl.migration.to2014A.To2014A;
import edu.gemini.spModel.io.impl.migration.to2015A.To2015A;
import edu.gemini.spModel.io.impl.migration.to2015B.To2015B;
import edu.gemini.spModel.io.impl.migration.to2016A.To2016A;
import edu.gemini.spModel.io.impl.migration.to2016B.To2016B;
import edu.gemini.spModel.io.impl.migration.to2016B.To2016B2;
import edu.gemini.spModel.io.impl.migration.to2017A.To2017A;
import edu.gemini.spModel.io.impl.migration.to2017B.To2017B;
import edu.gemini.spModel.io.impl.migration.to2018A.To2018A;
import edu.gemini.spModel.io.impl.migration.toPalote.Grillo2Palote;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.pio.*;
import edu.gemini.spModel.pio.xml.PioXmlUtil;
import edu.gemini.spModel.seqcomp.InstrumentSequenceSync;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public final class PioSpXmlParser {

    private static final Logger LOG = Logger.getLogger(PioSpXmlParser.class.getName());

    private interface NodeBuilder<T extends ISPNode> {
        boolean matches(Container c);
        T makeNode(ISPFactory f, ISPProgram p, Container c, List<ISPNode> children) throws SPException;
    }

    private static abstract class SimpleNodeBuilder<T extends ISPNode> implements NodeBuilder<T> {
        private final String tag;
        SimpleNodeBuilder(String tag) { this.tag = tag; }

        @Override public boolean matches(Container c) { return tag.equals(c.getKind()); }

        @Override public T makeNode(ISPFactory f, ISPProgram p, Container c, List<ISPNode> children) throws SPException {
            final T t = createEmpty(f, p, c, key(c));
            if (t instanceof ISPContainerNode) {
                ((ISPContainerNode) t).setChildren(children);
            } else if ((t != null) && (children.size() > 0)) {
                throw new RuntimeException("Cannot add children to a " + t.getClass().getName());
            }
            return t;
        }
        protected abstract T createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException;
    }

    private static final NodeBuilder<ISPGroup> GROUP_BUILDER = new SimpleNodeBuilder<ISPGroup>(SpIOTags.GROUP) {
        @Override protected ISPGroup createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            return f.createGroup(p, k);
        }
    };

    private static final NodeBuilder<ISPConflictFolder> CONFLICT_FOLDER_BUILDER = new SimpleNodeBuilder<ISPConflictFolder>(SpIOTags.CONFLICT_FOLDER) {
        @Override protected ISPConflictFolder createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            return f.createConflictFolder(p, k);
        }
    };

    private static final NodeBuilder<ISPTemplateFolder> TEMPLATE_FOLDER_BUILDER = new SimpleNodeBuilder<ISPTemplateFolder>(SpIOTags.TEMPLATE_FOLDER) {
        @Override protected ISPTemplateFolder createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            return f.createTemplateFolder(p, k);
        }
    };

    private static final NodeBuilder<ISPTemplateGroup> TEMPLATE_GROUP_BUILDER = new SimpleNodeBuilder<ISPTemplateGroup>(SpIOTags.TEMPLATE_GROUP) {
        @Override protected ISPTemplateGroup createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            return f.createTemplateGroup(p, k);
        }
    };

    private static final NodeBuilder<ISPTemplateParameters> TEMPLATE_PARAMETERS_BUILDER = new SimpleNodeBuilder<ISPTemplateParameters>(SpIOTags.TEMPLATE_PARAMETERS) {
        @Override protected ISPTemplateParameters createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            return f.createTemplateParameters(p, k);
        }
    };

    private static final NodeBuilder<ISPObservation> OBSERVATION_BUILDER = new NodeBuilder<ISPObservation>() {
        @Override public boolean matches(Container c) {
            return SpIOTags.OBSERVATION.equals(c.getKind());
        }

        @Override public ISPObservation makeNode(ISPFactory f, ISPProgram p, Container c, List<ISPNode> children) throws SPException {
            final String obsIdStr = c.getName();
            final int index;
            try {
                index = ((obsIdStr != null) && (obsIdStr.length() > 0)) ?
                        (new SPObservationID(obsIdStr)).getObservationNumber() : -1;
            } catch (SPBadIDException e) {
                throw new SPException("Could not parse observation id: " + obsIdStr);
            }

            final ISPObservation obs = f.createObservation(p, index, ObservationNI.NO_CHILDREN_INSTANCE, key(c));
            obs.setChildren(children);
            return obs;
        }
    };

    private static boolean isOldObsLogComponent(Container c) {
        return "ObsLog".equals(c.getType()) && "complete".equals(c.getSubtype());
    }

    private static final NodeBuilder<ISPObsQaLog> OBS_QA_LOG_BUILDER = new SimpleNodeBuilder<ISPObsQaLog>(SpIOTags.OBS_QA_LOG) {
        @Override protected ISPObsQaLog createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            return f.createObsQaLog(p, k);
        }
    };

    private static final NodeBuilder<ISPObsExecLog> OBS_EXEC_LOG_BUILDER = new SimpleNodeBuilder<ISPObsExecLog>(SpIOTags.OBS_EXEC_LOG) {
        @Override protected ISPObsExecLog createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            return f.createObsExecLog(p, k);
        }
    };

    private static final NodeBuilder<ISPObsComponent> OBS_COMPONENT_BUILDER = new SimpleNodeBuilder<ISPObsComponent>(SpIOTags.OBSCOMP) {
        @Override public boolean matches(Container c) {
            if (super.matches(c)) {
                return !isOldObsLogComponent(c);
            } else {
                return false;
            }
        }
        @Override public ISPObsComponent createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            final SPComponentType ct = getType(c);
            return (ct == null) ? null : f.createObsComponent(p, ct, k);
        }
    };

    private static final NodeBuilder<ISPSeqComponent> SEQ_COMPONENT_BUILDER = new SimpleNodeBuilder<ISPSeqComponent>(SpIOTags.SEQCOMP) {
        @Override public ISPSeqComponent createEmpty(ISPFactory f, ISPProgram p, Container c, SPNodeKey k) throws SPException {
            final SPComponentType ct = getType(c);
            return (ct == null) ? null : f.createSeqComponent(p, ct, k);
        }
    };

    private static final List<NodeBuilder<? extends ISPNode>> BUILDERS;

    static {
        List<NodeBuilder<? extends ISPNode>> l = new ArrayList<NodeBuilder<? extends ISPNode>>();
        l.add(CONFLICT_FOLDER_BUILDER);
        l.add(GROUP_BUILDER);
        l.add(OBS_COMPONENT_BUILDER);
        l.add(OBSERVATION_BUILDER);
        l.add(OBS_QA_LOG_BUILDER);
        l.add(OBS_EXEC_LOG_BUILDER);
        l.add(SEQ_COMPONENT_BUILDER);
        l.add(TEMPLATE_FOLDER_BUILDER);
        l.add(TEMPLATE_GROUP_BUILDER);
        l.add(TEMPLATE_PARAMETERS_BUILDER);
        BUILDERS = Collections.unmodifiableList(l);
    }

    private static NodeBuilder<? extends ISPNode> builderFor(Container c) {
        for (NodeBuilder<? extends ISPNode> nb : BUILDERS) {
            if (nb.matches(c)) return nb;
        }
        return null;
    }

    // The factory to use for making science program nodes
    private final ISPFactory _factory;

    /**
     * Constructs the parser with the factory and subject.
     */
    public PioSpXmlParser(ISPFactory factory) {
        _factory = factory;
    }

    public ISPRootNode parseDocument(File file) throws Exception {
        return parseDocument(PioXmlUtil.read(file));
    }

    public ISPRootNode parseDocument(Reader reader) throws Exception {
        return parseDocument(PioXmlUtil.read(reader));
    }

    public ISPRootNode parseDocument(PioNode doc) throws Exception {
        return _parseDocument((Document) doc);
    }

    // There is a difference between a node not appearing in the version map
    // (meaning it is unknown in the program) vs appearing with empty node
    // versions (meaning it simply hasn't been modified since being imported
    // from an older XML version).
    private scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> completeVersions(ISPNode node, scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> versions) {
        final VersionVector<LifespanId, Integer> v = JavaVersionMapOps.getOrNull(versions, node.getNodeKey());
        if (v == null) versions = versions.updated(node.getNodeKey(), JavaVersionMapOps.emptyNodeVersions().incr(node.getLifespanId()));

        if (node instanceof ISPContainerNode) {
            for (ISPNode child : ((ISPContainerNode) node).getChildren()) {
                versions = completeVersions(child, versions);
            }
        }
        return versions;
    }

    // Parse the top level document element
    private ISPRootNode _parseDocument(Document doc) throws Exception {
        // Mutate pre-2015A template folders to be readable.
        To2015A.updateProgram(doc);

        // Update pre-2015B target model
        To2015B.updateProgram(doc);

        // Update pre-2016A programs
        To2016A.updateProgram(doc);

        // Update pre-2016B programs
        To2016B.updateProgram(doc);

        // Update pre-2016B-2 programs
        To2016B2.updateProgram(doc);

        // Update pre-2017A programs
        To2017A.updateProgram(doc);

        // Update pre-2017B programs
        To2017B.updateProgram(doc);

        // Update pre-2018A programs
        To2018A.updateProgram(doc);

        // We will special case the Phase 1 container.
        Container p1Container = null;

        scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> versions = JavaVersionMapOps.emptyVersionMap();

        // A document may contain zero or more containers
        ISPRootNode root = null;
        for (Object o : doc.getContainers()) {
            final Container c = (Container) o;
            final String kind = c.getKind();
            if (SpIOTags.PHASE1.equalsIgnoreCase(kind)) {
                p1Container = c;
            } else if (VersionVectorPio.kind().equalsIgnoreCase(kind)) {
                versions = VersionVectorPio.toVersions(c);
            } else if (SpIOTags.PROGRAM.equalsIgnoreCase(kind)) {
                root = _makeProgramNode(c, c.getName(), key(c));
            } else if (kind.equals(SpIOTags.NIGHTLY_PLAN)) {
                root = _makeNightlyRecordNode(c, c.getName(), key(c));
            } else {
                throw new RuntimeException(String.format("Could not parse top-level container '%s'", kind));
            }
        }

        if (!(root instanceof ISPProgram)) return root;
        final ISPProgram prog = (ISPProgram) root;
        if (p1Container != null) {
            final GsaPhase1Data gsa = getGsaPhase1Data(p1Container);
            if (gsa != null) {
                final SPProgram obj = (SPProgram) root.getDataObject();
                obj.setGsaPhase1Data(gsa);
                root.setDataObject(obj);
            }
        }
        syncIteratorFirstRow(prog);

        prog.setVersions(completeVersions(prog, versions));

        return root;
    }

    // Convert the old Phase 1 model information into the 12B format.
    private GsaPhase1Data.Abstract getP1Abstract(Container c) {
        return new GsaPhase1Data.Abstract(Pio.getValue(c, "phase1Document/common/abstract", ""));
    }

    private GsaPhase1Data.Category getP1Category(Container c) {
        return new GsaPhase1Data.Category(Pio.getValue(c, "phase1Document/common/keywords/keywords-attributes/category", ""));
    }

    private Collection<GsaPhase1Data.Keyword> getP1Keywords(Container c) {
        final ParamSet kps = c.lookupParamSet(new PioPath("phase1Document/common/keywords"));
        if (kps == null) return Collections.emptyList();

        @SuppressWarnings("unchecked") List<Param> keywords = (List<Param>) kps.getParams("keyword");

        // keywords.map(new GsaPhase1Data.Keyword(_))
        final List<GsaPhase1Data.Keyword> lst = new ArrayList<GsaPhase1Data.Keyword>(keywords.size());
        for (Param p : keywords) {
            final String keyword = p.getValue();
            if (keyword != null) lst.add(new GsaPhase1Data.Keyword(keyword));
        }
        return Collections.unmodifiableCollection(lst);
    }

    private GsaPhase1Data.Investigator getInvestigator(ParamSet ps) {
        final String first = Pio.getValue(ps, "name/first", "");
        final String last  = Pio.getValue(ps, "name/last", "");
        final String email = Pio.getValue(ps, "contact/email", "");
        return new GsaPhase1Data.Investigator(first, last, email);
    }

    private GsaPhase1Data.Investigator getP1Pi(Container c) {
        final ParamSet ps = c.lookupParamSet(new PioPath("phase1Document/common/investigators/pi"));
        return ps == null ? new GsaPhase1Data.Investigator("", "", "") : getInvestigator(ps);
    }

    private Collection<GsaPhase1Data.Investigator> getP1Cois(Container c) {
        final ParamSet ps = c.lookupParamSet(new PioPath("phase1Document/common/investigators"));
        if (ps == null) return Collections.emptyList();

        final List<GsaPhase1Data.Investigator> cois = new ArrayList<GsaPhase1Data.Investigator>();
        for (ParamSet cps : ps.getParamSets("coi")) {
            cois.add(getInvestigator(cps));
        }
        return Collections.unmodifiableCollection(cois);
    }

    private GsaPhase1Data getGsaPhase1Data(Container c) {
        return new GsaPhase1Data(
                getP1Abstract(c),
                getP1Category(c),
                getP1Keywords(c),
                getP1Pi(c),
                getP1Cois(c)
        );
    }

    // Awful synchronization of the first step of an iterator with the
    // instrument node itself.  This needs to be done on import and before
    // program versions are stored because it has the potential to modify
    // default values in an instrument node that are not in sync with the
    // sequence.  These modifications are not really changes to the data,
    // just corrections to the incomplete XML information we store so they
    // shouldn't count as modifications.
    private static void syncIteratorFirstRow(ISPProgram prog) {
        for (ISPObservation obs : prog.getAllObservations()) {
            InstrumentSequenceSync.syncFromIterator(obs);
        }
    }

    // Parse the given container element and add nodes for it to the given
    // parent node (parent may be null if at the root of the science program).
    private ISPNode _parseContainer(ISPProgram program, Container container) throws Exception {

        // Handle updating the observation container as necessary to migrate data
        if (SpIOTags.OBSERVATION.equals(container.getKind())) {
            ToGnirsAtGn.instance.update(container);
            AddMissingStaticInstrumentParams.update(container);
        }

        // Map the child containers into ISPNode.
        final List<ISPNode> children = parseChildren(program, container);

        // Now put together the subtree rooted at this node.
        final ISPNode node = _makeContainerNode(program, container, children);
        if (node == null) return null;  // no longer using this node

        // Set the data object, etc.
        addParamSets(node, container);

        // More hacks to handle backwards compatibility.
        if (node instanceof ISPObservation) {
            final ISPObservation o = (ISPObservation) node;
            Grillo2Palote.toPalote(o, container, _factory);
            To2009B.instance.update(o, container);
            To2014A.update(_factory, o, container);
        } else if (node instanceof ISPTemplateFolder) {
            To2014A.update((ISPTemplateFolder) node, container);
        }

        return node;
    }

    private List<ISPNode> parseChildren(ISPProgram p, Container c) throws Exception {
        final List<ISPNode> children = new ArrayList<ISPNode>();
        for (Object o : c.getContainers()) {
            final ISPNode n = _parseContainer(p, (Container) o);
            if (n != null) children.add(n);
        }
        return children;
    }

    private boolean isEncrypted(ParamSet ps, ISPDataObject dataObject) {
        // encryption is new in 2014A so existing XML will be in clear text
        // even if the data object now implements Encrypted
        return (dataObject instanceof Encrypted) && ps.getParam(Encryption.ParamName()) != null;
    }

    private ParamSet decrypt(SPNodeKey key, ParamSet ps) {
        final String encrypted = ps.getParam(Encryption.ParamName()).getValue();
        final String clearText = Encryption.decrypt(key, encrypted);
        try {
            return (ParamSet) PioXmlUtil.read(clearText);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not parse encrypted object", ex);
            throw new RuntimeException(ex);
        }
    }

    private void addParamSets(ISPNode n, Container c) throws Exception {
        final SPNodeKey key = n.getNodeKey();
        for (Object o : c.getParamSets()) {
            final ParamSet ps = (ParamSet) o;
            final String kind = ps.getKind();
            final ISPDataObject dataObj = n.getDataObject();

            if (kind != null) {
                if (kind.equals(ISPDataObject.PARAM_SET_KIND)) {
                    final ParamSet ps0 = isEncrypted(ps, dataObj) ? decrypt(key, ps) : ps;
                    dataObj.setParamSet(ps0);
                    _groupCheck(n, dataObj);
                    n.setDataObject(dataObj);
                } else if (kind.equals(SpIOTags.USEROBJ)) {
                    _addUserObject(n, ps);
                } else if (kind.equals(ConflictPio.conflictsParamSet())) {
                    final ParamSet ps0 = isEncrypted(ps, dataObj) ? decrypt(key, ps) : ps;
                    n.setConflicts(ConflictPio.toConflicts(ps0, n));
                }
            }
        }
    }

    private static SPNodeKey key(Container c) {
        final String s = c.getKey();
        return (s == null) ? null : new SPNodeKey(s);
    }

    private ISPNode _makeContainerNode(ISPProgram p, Container c, List<ISPNode> children) throws Exception {
        final NodeBuilder<? extends ISPNode> nb = builderFor(c);
        if (nb == null) {
            LOG.log(Level.WARNING, "Unexpected value for: " + c.getKind() + "/" + c.getType() + "/" + c.getSubtype());
            return null;
        } else {
            return nb.makeNode(_factory, p, c, children);
        }
    }

    private static SPComponentType getType(Container c) {
        try {
            return SPComponentType.getInstance(c.getType(), c.getSubtype());
        } catch (NoSuchElementException ex) {
            LOG.log(Level.WARNING, "Ignoring container with unknown type: type=" + c.getType() + ", subtype=" + c.getSubtype() + ", name=" + c.getName());
            return null;
        }
    }

    // Create and return a program node with the given id and key
    private ISPProgram _makeProgramNode(Container c, String progIdStr, SPNodeKey nodeKey) throws Exception {
        final SPProgramID progId;
        if (progIdStr != null && progIdStr.length() != 0) {
            progId = SPProgramID.toProgramID(progIdStr);
        } else {
            progId = null;
        }
        final ISPProgram p = _factory.createProgram(nodeKey, progId);
        addParamSets(p, c);
        p.setChildren(parseChildren(p, c));
        return p;
    }

    // Create and return a nightly plan node with the given id and key.
    // Note that the caller has to add it to the database at some point.
    private ISPNightlyRecord _makeNightlyRecordNode(Container c, String progIdStr, SPNodeKey nodeKey) throws Exception {
        final SPProgramID progId;
        if (progIdStr != null && progIdStr.length() != 0) {
            progId = SPProgramID.toProgramID(progIdStr);
        } else {
            progId = null;
        }
        final ISPNightlyRecord record = _factory.createNightlyRecord(nodeKey, progId);
        addParamSets(record, c);
        return record;
    }


    // This code is for backward compatibility, since top level notes and phase1 objects
    // are now in an ISPObsComponent under the program or a group node. They used to be
    // stored as user objects in the top level program or group node.
    private void _addUserObject(ISPNode node, ParamSet paramSet)
            throws Exception {
        PioNode pioNode = paramSet.getParent();
        Container containerElement = (Container) pioNode;
        String type = containerElement.getType();
        String subtype = containerElement.getSubtype();

        if (type.equals(SPNote.SP_TYPE.broadType.toString()) && subtype.equals(SPNote.SP_TYPE.narrowType)) {
            ISPProgram prog = (ISPProgram) node;
            SPNote note = new SPNote();
            note.setParamSet(paramSet);
            ISPObsComponent noteNode = _factory.createObsComponent(prog,
                    SPNote.SP_TYPE, new SPNodeKey());
            prog.addObsComponent(0, noteNode);
            _groupCheck(noteNode, note);
            noteNode.setDataObject(note);
        }
    }

    // This code is for backward compatibility, since groups used to be implemented as
    // just a group name property of an observation or note. Now groups are regular nodes.
    // If the given node, with its data object, belong in a group, create the group if needed
    // and move the node there.
    private void _groupCheck(ISPNode node, ISPDataObject dataObj) throws Exception {
        if (dataObj instanceof SPObservation) {
            SPObservation spObs = (SPObservation) dataObj;
            String group = spObs.getGroup();
            if (group != null) {
                spObs.setGroup(null);
                _moveToGroup(group, node);
            }
        } else if (dataObj instanceof SPNote) {
            SPNote note = (SPNote) dataObj;
            String group = note.getGroup();
            if (group != null) {
                note.setGroup(null);
                _moveToGroup(group, node);
            }
        }
    }

    // This code is for backward compatibility.
    // Add a new node to the given node in a group with the given name, creating the group
    // if it doesn't already exist.
    private void _moveToGroup(String group, ISPNode node) throws Exception {
        // parent must be a program if node was in group
        ISPNode parent = node.getParent();
        if (parent instanceof ISPProgram) {
            ISPProgram prog = (ISPProgram) parent;
            ISPGroup groupNode = _getGroupNode(prog, group);
            if (node instanceof ISPObservation) {
                ISPObservation obs = (ISPObservation) node;
                prog.removeObservation(obs);
                groupNode.addObservation(obs);
            } else if (node instanceof ISPObsComponent) {
                ISPObsComponent obsComp = (ISPObsComponent) node;
                prog.removeObsComponent(obsComp);
                groupNode.addObsComponent(obsComp);
            }
        }
    }

    // This code is for backward compatibility.
    // Find and return a group node with the given name, creating the group
    // if it doesn't already exist.
    private ISPGroup _getGroupNode(ISPProgram prog, String group) throws Exception {
        for (Object o : prog.getGroups()) {
            ISPGroup groupNode = (ISPGroup) o;
            SPGroup spGroup = (SPGroup) groupNode.getDataObject();
            if (group.equals(spGroup.getGroup())) {
                return groupNode;
            }
        }
        // No existing group node found: create one
        ISPGroup groupNode = _factory.createGroup(prog, null);
        SPGroup spGroup = (SPGroup) groupNode.getDataObject();
        spGroup.setGroup(group);
        groupNode.setDataObject(spGroup);
        prog.addGroup(groupNode);
        return groupNode;
    }
}

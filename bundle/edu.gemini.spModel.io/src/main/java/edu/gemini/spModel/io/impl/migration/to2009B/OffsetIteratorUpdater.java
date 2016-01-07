package edu.gemini.spModel.io.impl.migration.to2009B;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.Container;
import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;


import java.util.*;
import java.util.logging.Logger;

/**
 * Updates offset iterators in pre-2009B code to use the new {@link GuideProbe}
 * and {@link GuideOption} classes.
 */
enum OffsetIteratorUpdater {
    instance;

    private static final Logger LOG = Logger.getLogger(OffsetIteratorUpdater.class.getName());

    // Gets a map from the old guide probe key to the new GuideProbe instance.
    private Map<String, GuideProbe> getProbeNameMap(Set<GuideProbe> referencedGuiders) {
        Map<String, GuideProbe> tagMap = new HashMap<>();

        tagMap.put("PWFS1", PwfsGuideProbe.pwfs1);
        tagMap.put("PWFS2", PwfsGuideProbe.pwfs2);

        for (GuideProbe guider : referencedGuiders) {
            GuideProbe.Type type = guider.getType();
            switch (type) {
                case OIWFS:
                    tagMap.put("OIWFS", guider);
                    // if there are multiple OIWFS (and there shouldn't be),
                    // we'll just use the last one
                    break;
                case AOWFS:
                    tagMap.put("AOWFS", guider);
                    // if there are multiple AOWFS (and there shouldn't be),
                    // we'll just use the last one
                    break;
            }
        }

        return tagMap;
    }

    Map<GuideProbe, String> updateOffsetIterators(ISPObservation obs, Container obsContainer, Set<GuideProbe> referencedGuiders)  {
        List<ISPSeqComponent> offsetIters = getOffsetNodes(obs);
        List<Container>       offsetConts = getOffsetContainers(obsContainer);

        if (offsetIters.size() != offsetConts.size()) {
            LOG.info("Skipping offset iterator update for obs " + obs.getObservationID());
            return Collections.emptyMap();
        }

        // Get a mapping of old probe name to GuideProbe instance (e.g.,
        // PWFS2 -> PwfsGuideProbe.pwfs2)
        Map<String, GuideProbe> probeNameMap = getProbeNameMap(referencedGuiders);

        // Create a mapping of GuideProbe to primary target tag.  Used to later
        // set the primary target for each guide probe in the target environment
        Map<GuideProbe, String> primaryTargetMap = new HashMap<>();

        // Update the offset iterator links to the correct values in 2009B.
        for (int i=0; i<offsetIters.size(); ++i) {
            ISPSeqComponent node = offsetIters.get(i);
            Container       cont = offsetConts.get(i);
            updateOffsetIterator(node, cont, referencedGuiders, probeNameMap, primaryTargetMap);
        }

        // return the primary target map for use in the To2009B converter
        return primaryTargetMap;
    }

    private ParamSet getDataObjectPset(Container cont) {
        @SuppressWarnings({"unchecked"}) List<ParamSet> psets = (List<ParamSet>) cont.getParamSets();
        for (ParamSet pset : psets) {
            if ("dataObj".equals(pset.getKind())) return pset;
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private void updateOffsetIterator(ISPSeqComponent node, Container cont,
                                      Set<GuideProbe> referencedGuiders,
                                      Map<String, GuideProbe> probeNameMap,
                                      Map<GuideProbe, String> primaryTargetMap)  {
        SeqRepeatOffsetBase<OffsetPosBase> dataObj;
        dataObj = (SeqRepeatOffsetBase<OffsetPosBase>) node.getDataObject();

        OffsetPosList<OffsetPosBase> opl = dataObj.getPosList();

        ParamSet dobjPset = getDataObjectPset(cont);
        ParamSet listPset = dobjPset.getParamSet("offsets");
        if (listPset == null) return; // no offset positions -- empty iterator

        for (ParamSet posPset : listPset.getParamSets()) {
            OffsetPosBase op = opl.getPositionAt(posPset.getSequence());
            op.removeAllLinks();

            // Set links for all positions explicitly mentioned in the XML.
            for (Param p : (List<Param>) posPset.getParams()) {
                // p and q params implicitly ignored, others should be the
                // names of the old guide probe options.
                GuideProbe guider = probeNameMap.get(p.getName());
                if (guider == null) continue;

                // freeze and park map to the new GuideOptions, any other
                // value should be considered "guide".
                GuideOption opt = guider.getGuideOptions().getDefaultActive();
                try {
                    opt = guider.getGuideOptions().parse(p.getValue());
                } catch (Exception ex) {
                    // was a specific target tag, so remember the first one
                    // for this guider
                    addProbeEntry(primaryTargetMap, guider, p.getValue());
                }

                op.setLink(guider, opt);
            }

            // Set links for guide stars referenced in the target env, but not
            // mentioned in the XML.  This would basically be working around a
            // bug in the old pre-2009B code.
            Set<GuideProbe> missingSet = new HashSet<>(referencedGuiders);
            missingSet.removeAll(op.getGuideProbes());
            for (GuideProbe missingGuider : missingSet) {
                op.setLink(missingGuider, missingGuider.getGuideOptions().getDefaultActive());
            }
        }

        node.setDataObject(dataObj);
    }

    private void addProbeEntry(Map<GuideProbe, String> primaryTargetMap, GuideProbe guider, String targetTag) {
        String val = primaryTargetMap.get(guider);
        if (val != null) return;
        primaryTargetMap.put(guider, targetTag);
    }

    private List<ISPSeqComponent> getOffsetNodes(ISPObservation obs)  {
        List<ISPSeqComponent> res = new ArrayList<>();
        ISPSeqComponent seq = obs.getSeqComponent();
        if (seq != null) addOffsetNodes(seq, res);
        return res;
    }

    private void addOffsetNodes(ISPSeqComponent root, List<ISPSeqComponent> result)  {
        Object dataObj = root.getDataObject();
        if (dataObj instanceof SeqRepeatOffsetBase) {
            result.add(root);
        }
        for (ISPSeqComponent child : root.getSeqComponents()) {
            addOffsetNodes(child, result);
        }
    }

    private List<Container> getOffsetContainers(Container rootContainer) {
        List<Container> res = new ArrayList<>();
        //noinspection unchecked
        for (Container cont : (List<Container>) rootContainer.getContainers()) {
            if (!"seqComp".equals(cont.getKind())) continue;
            addOffsetContainers(cont, res);
        }
        return res;
    }

    private void addOffsetContainers(Container rootContainer, List<Container> result) {
        // Check whether the current container is an offset iterator.  If so,
        // then add it to the results.  This is fairly lame ... but only a
        // one-time data migration thing.
        if ("Iterator".equals(rootContainer.getType())) {
            String subtype = rootContainer.getSubtype();
            if ("offset".equals(subtype) || "nicioffset".equals(subtype)) {
                result.add(rootContainer);
            }
        }

        // Add any child containers.
        //noinspection unchecked
        for (Container cont : (List<Container>) rootContainer.getContainers()) {
            if (!"seqComp".equals(cont.getKind())) continue;
            addOffsetContainers(cont, result);
        }
    }
}

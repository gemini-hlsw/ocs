// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqRepeatSmartGcalObsNI.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatSmartGcalObs;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatSmartGcalObsCB;

import java.util.logging.Logger;
import java.util.logging.Level;



/**
 * Initializes <code>{@link edu.gemini.pot.sp.ISPSeqComponent}</code> nodes.
 */
public abstract class SeqRepeatSmartGcalObsNI implements ISPNodeInitializer {
    private static final Logger LOG = Logger.getLogger(SeqRepeatSmartGcalObsNI.class.getName());

    public static class BaselineDay extends SeqRepeatSmartGcalObsNI {
        protected SPComponentType getMyType() {
            return SeqRepeatSmartGcalObs.BaselineDay.SP_TYPE;
        }
        protected ISPDataObject getNewDataObject() {
            return new SeqRepeatSmartGcalObs.BaselineDay();
        }
        protected SeqRepeatSmartGcalObsCB createConfigBuilder(ISPSeqComponent seqComp)  {
            return new SeqRepeatSmartGcalObsCB.BasecalDay(seqComp);
        }
    }

    public static class BaselineNight extends SeqRepeatSmartGcalObsNI {
        protected SPComponentType getMyType() {
            return SeqRepeatSmartGcalObs.BaselineNight.SP_TYPE;
        }
        protected ISPDataObject getNewDataObject() {
            return new SeqRepeatSmartGcalObs.BaselineNight();
        }
        protected SeqRepeatSmartGcalObsCB createConfigBuilder(ISPSeqComponent seqComp)  {
            return new SeqRepeatSmartGcalObsCB.BasecalNight(seqComp);
        }
    }

    public static class Flat extends SeqRepeatSmartGcalObsNI {
        protected SPComponentType getMyType() {
            return SeqRepeatSmartGcalObs.Flat.SP_TYPE;
        }
        protected ISPDataObject getNewDataObject() {
            return new SeqRepeatSmartGcalObs.Flat();
        }
        protected SeqRepeatSmartGcalObsCB createConfigBuilder(ISPSeqComponent seqComp)  {
            return new SeqRepeatSmartGcalObsCB.Flat(seqComp);
        }
    }

    public static class Arc extends SeqRepeatSmartGcalObsNI {
        protected SPComponentType getMyType() {
            return SeqRepeatSmartGcalObs.Arc.SP_TYPE;
        }
        protected ISPDataObject getNewDataObject() {
            return new SeqRepeatSmartGcalObs.Arc();
        }
        protected SeqRepeatSmartGcalObsCB createConfigBuilder(ISPSeqComponent seqComp)  {
            return new SeqRepeatSmartGcalObsCB.Arc(seqComp);
        }
    }

    protected abstract SPComponentType getMyType();
    protected abstract ISPDataObject getNewDataObject();
    protected abstract SeqRepeatSmartGcalObsCB createConfigBuilder(ISPSeqComponent seqComp) ;

    /**
     * Initializes the given <code>node</code>.
     * Implements <code>{@link edu.gemini.pot.sp.ISPNodeInitializer}</code>
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    public void initNode(ISPFactory factory, ISPNode node)
             {
        //LOG.log(Level.INFO, "Initing SeqRepeatFlat Node");

        ISPSeqComponent castNode = (ISPSeqComponent) node;
        if (!castNode.getType().equals(getMyType())) {
            throw new InternalError();
        }

        // data object of this Seq Component.
        try {
            castNode.setDataObject(getNewDataObject());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set data object of SpRepeatFlatObs node", ex);
        }

        // Set the configuration builder
        updateNode(node);
    }


    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    public void updateNode(ISPNode node)  {
        node.putClientData(IConfigBuilder.USER_OBJ_KEY,
                           createConfigBuilder((ISPSeqComponent) node));
    }
}

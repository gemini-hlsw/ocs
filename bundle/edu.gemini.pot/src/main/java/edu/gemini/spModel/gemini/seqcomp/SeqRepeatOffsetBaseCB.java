// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqRepeatOffsetBaseCB.java 45173 2012-05-10 19:45:13Z swalker $
//

package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.guide.DefaultGuideOptions;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.target.obsComp.GuideSequence;
import edu.gemini.spModel.target.obsComp.GuideSequence.ExplicitGuideSetting;
import edu.gemini.spModel.target.obsComp.GuideSequence.GuideState;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;

import java.util.*;


public abstract class SeqRepeatOffsetBaseCB<P extends OffsetPosBase> extends AbstractSeqComponentCB {
    private static final String GUIDING_PROP = "guiding";

    // The name of the system
    protected static final String SYSTEM_NAME = SeqConfigNames.TELESCOPE_CONFIG_NAME;

    // for serialization
    private static final long serialVersionUID = 1L;

    private transient int _curCount;
    private transient int _max;
    private transient OffsetPosList<P> _posList;

    protected SeqRepeatOffsetBaseCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        //noinspection unchecked
        SeqRepeatOffsetBaseCB<P> result = (SeqRepeatOffsetBaseCB<P>) super.clone();

        result._curCount       = 0;
        result._max            = 0;
        result._posList        = null;

        return result;
    }

    protected void thisReset(Map options) {
        _curCount = 0;
        //noinspection unchecked
        SeqRepeatOffsetBase<P> sro = (SeqRepeatOffsetBase<P>) getDataObject();

        _max = sro.size();
        _posList = sro.getPosList();
    }

    protected boolean thisHasNext() {
        return _curCount < _max;
    }

    protected final void thisApplyNext(IConfig config, IConfig prevFull) {
        // Add the correct offset to the config
        P op = _posList.getPositionAt(_curCount);
        addPosition(config, op);
        ++_curCount;
    }

    protected void addPosition(IConfig config, P op) {
        addPandQ(config, op);
        addGuiding(config, op);
    }

    private void addGuiding(IConfig config, P op) {
        final DefaultGuideOptions.Value defaultOption = op.getDefaultGuideOption();

        final GuideState guideState;
        if (_posList.getAdvancedGuiding().size() == 0) {
            guideState = GuideState.forDefaultOption(defaultOption);
        } else {
            final Set<Map.Entry<GuideProbe, GuideOption>> links = op.getLinks().entrySet();
            final List<ExplicitGuideSetting> res = new ArrayList<ExplicitGuideSetting>(links.size());
            for (Map.Entry<GuideProbe, GuideOption> me : op.getLinks().entrySet()) {
                res.add(new ExplicitGuideSetting(me.getKey(), me.getValue()));
            }
            guideState = new GuideState(defaultOption, DefaultImList.create(res));
        }

        config.putParameter(SYSTEM_NAME,
              DefaultParameter.getInstance(GuideSequence.GUIDE_STATE_PARAM, guideState));
    }

    protected void addPandQ(IConfig config, P op) {
        config.putParameter(SYSTEM_NAME,
                            StringParameter.getInstance("p", op.getXAxisAsString()));
        config.putParameter(SYSTEM_NAME,
                            StringParameter.getInstance("q", op.getYAxisAsString()));
    }
}
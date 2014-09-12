//
// $
//

package edu.gemini.spModel.io.impl.migration.to2010B;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.io.impl.SpIOTags;
import edu.gemini.spModel.pio.*;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.CrossDispersed;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.PixelScale;
import edu.gemini.spModel.gemini.gnirs.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ad-hoc code to update a program from the pre-2010B GNIRS sequence component.
 * As part of the GNIRS commissioning for its transition to GN, the cross
 * dispersed "YES" option becomes "SXD" or "LXD" depending upon the pixel scale
 * value:
 *        (0.15, YES) => SXD
 *        (0.05, YES) => LXD
 */
public enum ToGnirsAtGn {
    instance;

    private static final Version VERSION_2013B = Version.match("2013B-1");
    private static final Version VERSION_2010B = Version.match("2010B-2");

    public void update(Container obsContainer)  {
        // Skip this test in 2013B observations and beyond.
        if (obsContainer.getVersion().compareTo(VERSION_2013B) >= 0) return; // up-to-date

        Option<Container> instOpt = instrumentContainer(obsContainer);
        if (instOpt.isEmpty()) return; // Not GNIRS
        Container inst = instOpt.getValue();

        // Check the version to see if an update is needed.
        Version v = inst.getVersion();
        if (v.compareTo(VERSION_2010B) >= 0) return; // up-to-date

        // ParamSet containing the serialized position list.
        Option<Container> seqOpt = topLevelSequence(obsContainer);
        if (seqOpt.isEmpty()) return; // no sequence anyway
        Container seq = seqOpt.getValue();

        PixelScale ps = extractStaticValues(inst);
        convert(seq, ps);
    }

    @SuppressWarnings({"unchecked"})
    private static Option<Container> instrumentContainer(Container obsContainer) {
        List<Container> cList = (List<Container>) obsContainer.getContainers();
        for (Container c : cList) {
            if (SpIOTags.OBSCOMP.equals(c.getKind()) &&
                InstGNIRS.SP_TYPE.narrowType.equals(c.getSubtype())) {
                return new Some<Container>(c);
            }
        }
        return None.instance();
    }

    @SuppressWarnings({"unchecked"})
    private static Option<Container> topLevelSequence(Container obsContainer) {
        List<Container> cList = (List<Container>) obsContainer.getContainers();
        for (Container c : cList) {
            if (SpIOTags.SEQCOMP.equals(c.getKind())) {
                return new Some<Container>(c);
            }
        }
        return None.instance();
    }

    @SuppressWarnings({"unchecked"})
    private static Option<ParamSet> dataObj(Container obsContainer) {
        List<ParamSet> psList = (List<ParamSet>) obsContainer.getParamSets();
        for (ParamSet ps : psList) {
            if (ISPDataObject.PARAM_SET_KIND.equals(ps.getKind())) {
                return new Some<ParamSet>(ps);
            }
        }
        return None.instance();
    }


    private static final String scaleName = InstGNIRS.PIXEL_SCALE_PROP.getName();
    private static final String xdName    = InstGNIRS.CROSS_DISPERSED_PROP.getName();

    // Get the static pixel scale and XD option.
    private static PixelScale extractStaticValues(Container inst) {
        PixelScale  scale = PixelScale.DEFAULT;

        Option<ParamSet> psetOpt = dataObj(inst);
        if (!psetOpt.isEmpty()) {
            ParamSet pset = psetOpt.getValue();
            String val = Pio.getValue(pset, scaleName);
            if (val != null) scale = PixelScale.getPixelScale(val);
        }

        return scale;
    }

    private static final String itType = SeqConfigGNIRS.SP_TYPE.narrowType;

    // Top-level iterators revert back to the static configuration before
    // descending into children. Non-top-level iterators pick up where the last
    // one left off.
    private static void convert(Container p, PixelScale scale) {
        @SuppressWarnings({"unchecked"})
        List<Container> children = (List<Container>) p.getContainers();
        for (Container c : children) {
            String subType = c.getSubtype();
            if (itType.equals(subType)) {
                convertGnirsSequence(c, scale);
            }
            convert(c, scale);
        }
    }

    private static void convertGnirsSequence(Container c, PixelScale scale) {
        Option<ParamSet> psetOpt = dataObj(c);
        if (psetOpt.isEmpty()) return;
        ParamSet pset = psetOpt.getValue();
        xlat(pset, scale);
    }


    private static void xlat(ParamSet pset, PixelScale scale) {
        Param p = pset.getParam(xdName);
        if (p == null) return;

        List<CrossDispersed> xdList = xlat(scale, p.getValues());
        if (xdList.size() == 0) return;

        List<String> newVals = new ArrayList<String>(xdList.size());
        for (CrossDispersed newXd : xdList) newVals.add(newXd.name());
        p.setValues(newVals);
    }

    private static List<CrossDispersed> xlat(PixelScale scale, List<String> xdVals) {
        if (xdVals == null) return Collections.emptyList();
        List<CrossDispersed> res = new ArrayList<CrossDispersed>(xdVals.size());
        for (String val : xdVals) {
            CrossDispersed xd = CrossDispersed.convertPre2010Xd(val, scale);
            res.add(xd);
        }
        return res;
    }
}
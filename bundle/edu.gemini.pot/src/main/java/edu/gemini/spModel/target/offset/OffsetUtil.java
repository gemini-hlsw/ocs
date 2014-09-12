//
// $
//

package edu.gemini.spModel.target.offset;

import edu.gemini.pot.sp.*;
import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.IOffsetPosListProvider;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;

import java.util.*;

/**
 * A utility for converting between the old {@link OffsetPosList} and
 * {@link OffsetPosBase} classes to the new {@link Offset} positions.
 */
public final class OffsetUtil {

    /**
     * An empty set of {@link Offset}s.
     */
    public static final Set<Offset> NO_OFFSETS = Collections.emptySet();

    /**
     * A single {@ilnk Offset} at the base position.
     */
    public static final Set<Offset> BASE_POS_OFFSET =
            Collections.singleton(new Offset(new Angle(0, ARCSECS), new Angle(0, ARCSECS)));


    /**
     * Gets an unmodifiable Set of {@link Offset}s for all the positions in the
     * given array of offset pos lists.  If there are none, then
     * {@link #NO_OFFSETS} are returned.
     */
    public static Set<Offset> getOffsets(Option<OffsetPosList[]> posListAOpt) {
        if (posListAOpt.isEmpty()) return NO_OFFSETS;
        return getOffsets(posListAOpt.getValue());
    }

    // Get a set of all the offset positions in the given array of position
    // lists.
    private static Collection<OffsetPosBase> extractOffsets(OffsetPosList[] posListA) {
        Collection<OffsetPosBase> res = new ArrayList<OffsetPosBase>();
        for (OffsetPosList opl : posListA) {
            for (Object obj : opl.getAllPositions()) {
                res.add((OffsetPosBase) obj);
            }
        }
        return res;
    }

    // Convert the OffsetPosBase to skycalc offset positions.
    private static Set<Offset> convertOffsets(Collection<OffsetPosBase> col) {
        if (col.size() == 0) return NO_OFFSETS;

        Set<Offset> res = new LinkedHashSet<Offset>();
        for (OffsetPosBase pos : col) {
            double x = pos.getXaxis();
            double y = pos.getYaxis();

            Angle p = new Angle(x, ARCSECS);
            Angle q = new Angle(y, ARCSECS);
            res.add(new Offset(p, q));
        }
        return (res.size() == 0) ? NO_OFFSETS : Collections.unmodifiableSet(res);
    }

    // Filter out positions which have all of their guiders turned off.
    private static Collection<OffsetPosBase> filterNonGuidedPositions(Collection<OffsetPosBase> col) {
        Iterator<OffsetPosBase> it = col.iterator();
        while (it.hasNext()) {
            OffsetPosBase pos = it.next();

            boolean guiding = false;
            if (pos.getDefaultGuideOption().isActive()) {
                // Technically, it could be overridden and turned off for all
                // guiders in use but we'll ignore that pathological case.
                guiding = true;
            } else {
                // Default guiding is off, so check whether there are any
                // overridden guiders with guiding turned on.
                Set<GuideProbe> guiders = pos.getGuideProbes();
                for (GuideProbe guider : guiders) {
                    GuideOption opt = pos.getLink(guider);
                    if ((opt != null) && opt.isActive()) {
                        guiding = true;
                        break;
                    }
                }
            }

            if (!guiding) it.remove();
        }

        return col;
    }

    /**
     * Gets an unmodifiable Set of {@link Offset}s for all the positions in the
     * given array of offset pos lists.
     */
    public static Set<Offset> getOffsets(OffsetPosList[] posListA) {
        if ((posListA == null) || (posListA.length == 0)) return NO_OFFSETS;
        return convertOffsets(extractOffsets(posListA));
    }

    /**
     * Gets all the positions that will be used for obtaining science data.
     * If there are no explicit offset positions, then a single {@link Offset}
     * at the base position is returned.  See {@link #BASE_POS_OFFSET}.
     */
    public static Set<Offset> getSciencePositions(Option<OffsetPosList[]> posListAOpt) {
        if (posListAOpt.isEmpty()) return BASE_POS_OFFSET;
        return getSciencePositions(posListAOpt.getValue());
    }

    /**
     * Gets all the positions that will be used for obtaining science data.
     * If there are no explicit offset positions, then a single {@link Offset}
     * at the base position is returned.  See {@link #BASE_POS_OFFSET}.
     */
    public static Set<Offset> getSciencePositions(OffsetPosList[] posListA) {
        if ((posListA == null) || (posListA.length == 0)) return BASE_POS_OFFSET;

        Set<Offset> res = convertOffsets(filterNonGuidedPositions(extractOffsets(posListA)));
        return res.isEmpty() ? BASE_POS_OFFSET : res;
    }

    public static Set<Offset> getSciencePositions2(OffsetPosList<OffsetPosBase>[] posListA) {
        return getSciencePositions(posListA);
    }


    public static List<OffsetPosList<OffsetPosBase>> allOffsetPosLists(ISPNode node) {
        if (node == null) return Collections.emptyList();
        final List<OffsetPosList<OffsetPosBase>> res = new ArrayList<OffsetPosList<OffsetPosBase>>();
        addOffsetPosLists(node, res);
        return res;
    }

    // Fish out all the offset pos lists
    private static void addOffsetPosLists(ISPNode node, List<OffsetPosList<OffsetPosBase>> posLists) {
        Object dataObj = node.getDataObject();
        if (dataObj instanceof IOffsetPosListProvider) {
            @SuppressWarnings("unchecked") IOffsetPosListProvider<OffsetPosBase> prov = (IOffsetPosListProvider<OffsetPosBase>) dataObj;
            OffsetPosList<OffsetPosBase> posList = prov.getPosList();
            if (posList != null) posLists.add(posList);
        }

        if (!(node instanceof ISPContainerNode)) return;
        ISPContainerNode parent = (ISPContainerNode) node;
        for (ISPNode child : parent.getChildren()) {
            addOffsetPosLists(child, posLists);
        }
    }
}

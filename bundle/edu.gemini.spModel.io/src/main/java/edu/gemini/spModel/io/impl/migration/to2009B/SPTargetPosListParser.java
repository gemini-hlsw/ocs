//
// $
//

package edu.gemini.spModel.io.impl.migration.to2009B;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A singleton used to parse pre-2009B target lists into a TargetEnvironment.
 */
enum SPTargetPosListParser {
    instance;

    private static final Logger LOG = Logger.getLogger(SPTargetPosListParser.class.getName());

    // A small helper that is basically a mutable target enviornment.  It is
    // built up target by target parsed fromt the XML, then converted into a
    // real TargetEnvironment.
    private final static class Targets {
        SPTarget base;
        Map<GuideProbe, GuideProbeTargets> guideMap = new HashMap<GuideProbe, GuideProbeTargets>();
        List<SPTarget> userTargets = new ArrayList<SPTarget>();

        TargetEnvironment toTargetEnv() {
            if (base == null) base = SPTarget.createDefaultBasePosition();
            ImList<GuideProbeTargets> glst = DefaultImList.create(guideMap.values());
            ImList<SPTarget> ulst = DefaultImList.create(userTargets);
            return TargetEnvironment.create(base).setAllPrimaryGuideProbeTargets(glst).setUserTargets(ulst);
        }
    }

    // Generic interface for adding the various types of targets that appear
    // in the position list to a Targets object
    private interface TargetHandler {
        boolean matches(String tag);
        void addTarget(SPTarget target, Targets targets);
    }

    private static final TargetHandler BASE_HANDLER = new TargetHandler() {
        public boolean matches(String tag) {
            return "Base".equals(tag);
        }

        public void addTarget(SPTarget target, Targets targets) {
            targets.base = target;
        }
    };

    private static final class GuideHandler implements TargetHandler {
        private final GuideProbe guider;
        private final Pattern pat;

        GuideHandler(GuideProbe guider, String prefix) {
            this.guider = guider;
            this.pat    = Pattern.compile(prefix + "-\\d+");
        }

        public boolean matches(String tag) {
            Matcher mat = pat.matcher(tag);
            return mat.matches();
        }

        public void addTarget(SPTarget target, Targets targets) {
            GuideProbeTargets gt = targets.guideMap.get(guider);
            if (gt == null) {
                gt = GuideProbeTargets.create(guider, ImCollections.singletonList(target));
            } else {
                gt = gt.setOptions(gt.getOptions().append(target));
            }
            targets.guideMap.put(guider, gt);
        }
    }

    private static final TargetHandler USER_HANDLER = new TargetHandler() {
        private final Pattern pat = Pattern.compile("User\\d+");

        public boolean matches(String tag) {
            Matcher mat = pat.matcher(tag);
            return mat.matches();
        }

        public void addTarget(SPTarget target, Targets targets) {
            targets.userTargets.add(target);
        }
    };

    // Compares two target param sets based upon their tags.  Splits the root
    // part of the tag from the index in order to compare two tags with the
    // same root but different indices correctly.
    private static Comparator<ParamSet> TARGET_COMPARATOR = new Comparator<ParamSet>() {
        class Tag implements Comparable<Tag> {
            private final Pattern[] pats = new Pattern[] {
                    Pattern.compile("(.*)-(\\d+)"),
                    Pattern.compile("(User)(\\d+)"),
            };

            private final String root;
            private final int index;

            Tag(String tag) {
                String root = null;
                int index   = -1;
                for (Pattern pat : pats) {
                    Matcher mat = pat.matcher(tag);
                    if (mat.matches()) {
                        root  = mat.group(1);
                        index = Integer.parseInt(mat.group(2));
                        break;
                    }
                }
                if (root == null) root = tag;

                this.root  = root;
                this.index = index;
            }

            public int compareTo(Tag that) {
                int res = root.compareTo(that.root);
                if (res != 0) return res;

                res = index - that.index;
                return res;
            }
        }

        public int compare(ParamSet ps1, ParamSet ps2) {
            Tag tag1 = new Tag(ps1.getName());
            Tag tag2 = new Tag(ps2.getName());
            return tag1.compareTo(tag2);
        }
    };

    public Tuple2<TargetEnvironment, Map<String, SPTarget>> parse(SPObservationID obsId, ParamSet pset, Set<GuideProbe> availableGuiders) {
        Map<String, SPTarget> tagMap = new HashMap<String, SPTarget>();

        Set<TargetHandler> handlers = setupHandlers(availableGuiders);

        List<ParamSet> targetPsets = pset.getParamSets();
        Collections.sort(targetPsets, TARGET_COMPARATOR);

        Targets targets = new Targets();
        nextTarget: for (ParamSet targetPset : targetPsets) {
            SPTarget target = SPTarget.fromParamSet(targetPset);
            String tag = targetPset.getName();
            tagMap.put(tag, target);

            for (TargetHandler handler : handlers) {
                if (handler.matches(tag)) {
                    handler.addTarget(target, targets);
                    continue nextTarget;
                }
            }

            String obsIdStr = obsId == null ? "unknown" : obsId.toString();
            String msg;
            msg = String.format("Skipping target '%s' in obs '%s'", tag, obsIdStr);
            LOG.info(msg);
        }

        return new Pair<TargetEnvironment, Map<String, SPTarget>>(targets.toTargetEnv(), tagMap);
    }

    private Set<TargetHandler> setupHandlers(Set<GuideProbe> availableGuiders) {
        Set<TargetHandler> handlers = new HashSet<TargetHandler>();

        handlers.add(BASE_HANDLER);
        handlers.add(USER_HANDLER);
        handlers.add(new GuideHandler(PwfsGuideProbe.pwfs1, "PWFS1"));
        handlers.add(new GuideHandler(PwfsGuideProbe.pwfs2, "PWFS2"));

        for (GuideProbe guider : availableGuiders) {
            GuideProbe.Type type = guider.getType();
            switch (type) {
                case OIWFS :
                    handlers.add(new GuideHandler(guider, "OIWFS"));
                    // shouldn't be more than one OIWFS guide probe type, but
                    // if so, then we'll juse use one at random
                    break;
                case AOWFS:
                    handlers.add(new GuideHandler(guider, "AOWFS"));
                    // shouldn't be more than one AOWFS guide probe type, but
                    // if so, then we'll juse use one at random
                    break;
            }
        }
        return handlers;
    }
}

package edu.gemini.spModel.io.impl.migration.to2009B;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.UserTarget;
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
    // built up target by target parsed from the XML, then converted into a
    // real TargetEnvironment.
    private final static class Targets {
        SPTarget base;
        final Map<GuideProbe, GuideProbeTargets> guideMap = new HashMap<>();
        final List<SPTarget> userTargets = new ArrayList<>();

        TargetEnvironment toTargetEnv() {
            if (base == null) base = new SPTarget();
            final ImList<GuideProbeTargets> glst = DefaultImList.create(guideMap.values());
            final ImList<UserTarget>        ulst = DefaultImList.create(userTargets).map(t -> new UserTarget(UserTarget.Type.other, t));
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
        @Override public boolean matches(final String tag) {
            return "Base".equals(tag);
        }

        @Override public void addTarget(final SPTarget target, final Targets targets) {
            targets.base = target;
        }
    };

    private static final class GuideHandler implements TargetHandler {
        private final GuideProbe guider;
        private final Pattern pat;

        GuideHandler(final GuideProbe guider, final String prefix) {
            this.guider = guider;
            this.pat    = Pattern.compile(prefix + "-\\d+");
        }

        @Override public boolean matches(final String tag) {
            return pat.matcher(tag).matches();
        }

        @Override public void addTarget(final SPTarget target, final Targets targets) {
            final GuideProbeTargets gt = targets.guideMap.get(guider);
            final GuideProbeTargets gtNew = gt == null ?
                    GuideProbeTargets.create(guider, ImCollections.singletonList(target)) :
                    gt.setOptions(gt.getOptions().append(target));
            targets.guideMap.put(guider, gtNew);
        }
    }

    private static final TargetHandler USER_HANDLER = new TargetHandler() {
        private final Pattern pat = Pattern.compile("User\\d+");

        @Override public boolean matches(final String tag) {
            return pat.matcher(tag).matches();
        }

        @Override public void addTarget(final SPTarget target, final Targets targets) {
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

            Tag(final String tag) {
                String root = null;
                int index   = -1;
                for (final Pattern pat : pats) {
                    final Matcher mat = pat.matcher(tag);
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

            public int compareTo(final Tag that) {
                int res = root.compareTo(that.root);
                return res != 0 ? res : index - that.index;
            }
        }

        public int compare(final ParamSet ps1, final ParamSet ps2) {
            final Tag tag1 = new Tag(ps1.getName());
            final Tag tag2 = new Tag(ps2.getName());
            return tag1.compareTo(tag2);
        }
    };

    public Tuple2<TargetEnvironment, Map<String, SPTarget>> parse(final SPObservationID obsId, final ParamSet pset,
                                                                  final Set<GuideProbe> availableGuiders) {
        final Map<String, SPTarget> tagMap = new HashMap<>();

        final Set<TargetHandler> handlers = setupHandlers(availableGuiders);

        final List<ParamSet> targetPsets = pset.getParamSets();
        Collections.sort(targetPsets, TARGET_COMPARATOR);

        final Targets targets = new Targets();
        nextTarget: for (final ParamSet targetPset : targetPsets) {
            final SPTarget target = SPTarget.fromParamSet(targetPset);
            final String tag = targetPset.getName();
            tagMap.put(tag, target);

            for (TargetHandler handler : handlers) {
                if (handler.matches(tag)) {
                    handler.addTarget(target, targets);
                    continue nextTarget;
                }
            }

            final String obsIdStr = obsId == null ? "unknown" : obsId.toString();
            final String msg = String.format("Skipping target '%s' in obs '%s'", tag, obsIdStr);
            LOG.info(msg);
        }

        return new Pair<>(targets.toTargetEnv(), tagMap);
    }

    private Set<TargetHandler> setupHandlers(final Set<GuideProbe> availableGuiders) {
        final Set<TargetHandler> handlers = new HashSet<>();

        handlers.add(BASE_HANDLER);
        handlers.add(USER_HANDLER);
        handlers.add(new GuideHandler(PwfsGuideProbe.pwfs1, "PWFS1"));
        handlers.add(new GuideHandler(PwfsGuideProbe.pwfs2, "PWFS2"));

        for (final GuideProbe guider : availableGuiders) {
            final GuideProbe.Type type = guider.getType();
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

package jsky.app.ot.gemini.inst;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.gemini.gnirs.GNIRS_OIWFS_Feature;
import jsky.app.ot.gemini.nifs.NIFS_OIWFS_Feature;
import jsky.app.ot.gemini.niri.NIRI_OIWFS_Feature;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;

import java.awt.*;
import java.util.Collection;


/**
 * Draws the OIWFS.
 * <p>
 * This class is a wrapper for one of the instrument specific classes.
 * The class used depends on the instrument being used.
 */
public class OIWFS_Feature extends TpeImageFeature {

    // The instrument OIWFS feature
    private TpeImageFeature _feat;

    // The instrument specific subclasses
    private TpeImageFeature _niriFeat;
    private TpeImageFeature _gnirsFeat;
    private TpeImageFeature _nifsFeat;


    private static final BasicPropertyList _props = new BasicPropertyList(OIWFS_Feature.class.getName());
    static final String PROP_WITH_VIG = "With Vignetting";
    static final String PROP_FILL_OBSCURED = "Fill Obscured Area";
    static {
        _props.registerBooleanProperty(PROP_WITH_VIG, true);
        _props.registerBooleanProperty(PROP_FILL_OBSCURED, false);
    }

    /**
     * Construct the feature with its name and description.
     */
    public OIWFS_Feature() {
        super("OIWFS", "Show the field of view of the OIWFS (if any).");
    }

    /**
     * Override reinit to start watching properties.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        final TpeContext ctx = iw.getContext();
        if (ctx.instrument().isEmpty()) return;
        final SPInstObsComp inst = ctx.instrument().get();

        // For now, assume instruments have same properties.
        // If this is not the case, the TPE View menu will need to be updated
        // whenever the instrument changes, since getProperties() is called
        // once only the first time the TPE is displayed.
        // Note that BHROS is simply a filter for GMOS-S, so we use GMOS-S for this.
        _feat = null;
        if (inst instanceof InstNIRI) {
            if (_niriFeat == null) _niriFeat = new NIRI_OIWFS_Feature();
            _feat = _niriFeat;
        } else if (inst instanceof InstGmosNorth || inst instanceof InstGmosSouth || inst instanceof InstBHROS) {
            _feat = GmosOiwfsFeature.instance();
        } else if (inst instanceof InstGNIRS) {
            if (_gnirsFeat == null) _gnirsFeat = new GNIRS_OIWFS_Feature();
            _feat = _gnirsFeat;
        } else if (inst instanceof InstNIFS) {
            if (_nifsFeat == null) _nifsFeat = new NIFS_OIWFS_Feature();
            _feat = _nifsFeat;
        } else if (inst instanceof Flamingos2) {
            _feat = Flamingos2OiwfsFeature.instance();
        }

        if (_feat != null)
            _feat.reinit(iw, tii);
    }


    /**
     * Override unloaded to quit watching properties.
     */
    public void unloaded() {
        super.unloaded();
        if (_feat != null)
            _feat.unloaded();
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return _props;
    }

    /** Static version of getProperties() */
    public static BasicPropertyList getProps() {
        return _props;
    }


    /**
     * Draw the feature.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        if (_feat != null)
            _feat.draw(g, tii);
    }

    @Override public boolean isEnabled(TpeContext ctx) {
        if (!super.isEnabled(ctx)) return false;
        if (!ctx.instrument().isDefined()) return false;
        return ctx.instrument().get().hasOIWFS();
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }

    @Override
    public Option<Collection<TpeMessage>> getMessages() {
        if (_feat==null) return None.instance();
        return _feat.getMessages();
    }
}

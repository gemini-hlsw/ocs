//
// $
//

package jsky.app.ot.tpe.feat;

import edu.gemini.spModel.guide.GuideProbe;
import jsky.app.ot.tpe.TpeCreateableItem;

/**
 * Identifies {@link jsky.app.ot.tpe.TpeCreateableItem createable items} that
 * make guide stars.
 */
public interface TpeGuidePosCreateableItem extends TpeCreateableItem {

    /**
     * Gets the type of guide star that will be created.
     *
     * @return guider associated with any guide stars created by this item
     */
    GuideProbe getGuideProbe();
}

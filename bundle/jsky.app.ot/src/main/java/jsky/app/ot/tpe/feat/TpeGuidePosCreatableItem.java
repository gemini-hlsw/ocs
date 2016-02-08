//
// $
//

package jsky.app.ot.tpe.feat;

import edu.gemini.spModel.guide.GuideProbe;
import jsky.app.ot.tpe.TpeCreatableItem;

/**
 * Identifies {@link TpeCreatableItem createable items} that
 * make guide stars.
 */
public interface TpeGuidePosCreatableItem extends TpeCreatableItem {

    /**
     * Gets the type of guide star that will be created.
     *
     * @return guider associated with any guide stars created by this item
     */
    GuideProbe getGuideProbe();
}

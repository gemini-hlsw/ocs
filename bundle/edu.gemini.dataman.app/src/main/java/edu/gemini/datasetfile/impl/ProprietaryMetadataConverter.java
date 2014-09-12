//
// $Id$
//

package edu.gemini.datasetfile.impl;

import edu.gemini.fits.Header;
import edu.gemini.fits.HeaderItem;
import edu.gemini.fits.DefaultHeaderItem;

import java.util.logging.Logger;

/**
 *
 */
final class ProprietaryMetadataConverter {
    private static final Logger LOG = Logger.getLogger(ProprietaryMetadataConverter.class.getName());

    private ProprietaryMetadataConverter() {
    }

    static final String PROP_MD = "PROP_MD";

    static boolean get(Header h) {
        HeaderItem item = h.get(PROP_MD);
        if (item == null) return false;
        return item.getBooleanValue();
    }

    static HeaderItem toItem(boolean propMd) {
        String comment = "Proprietary Metadata";
        return DefaultHeaderItem.create(PROP_MD, propMd, comment);
    }
}

//
// $Id: ReleaseDateCalculator.java 281 2006-02-13 17:52:21Z shane $
//

package edu.gemini.dataman.listener;

import edu.gemini.datasetfile.DatasetFile;
import edu.gemini.spModel.dataflow.GsaAspect;
import edu.gemini.spModel.obsclass.ObsClass;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Calculates the release date for a dataset based upon the program type and
 * observation class.
 */
public class ReleaseDateCalculator {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private ReleaseDateCalculator() {
    }

    public static Date calculate(DatasetFile dsetFile, GsaAspect gsa) {
        // If this is an obs class that isn't charged to program, there is
        // no proprietary period.
        long obsDate = dsetFile.getDataset().getTimestamp();
        ObsClass obsClass = dsetFile.getObsClass();
        if (!obsClass.shouldChargeProgram()) return new Date(obsDate);

        // Calculate when the proprietary months will have expired.
        Calendar cal = Calendar.getInstance(UTC);
        cal.setTimeInMillis(obsDate);
        cal.add(Calendar.MONTH, gsa.getProprietaryMonths());

        return cal.getTime();
    }
}

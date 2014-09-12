//
// $
//

package jsky.app.ot.progadmin;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.timeacct.TimeAcctAllocation;

import java.io.Serializable;


/**
 * Admin properties that apply to time accounting.
 */
class TimeAcctModel implements Serializable {
    private final TimeAcctAllocation allocation;
    private final TimeValue minTime;

    public TimeAcctModel(TimeAcctAllocation allocation) {
        this(allocation, TimeValue.ZERO_HOURS);
    }

    public TimeAcctModel(TimeAcctAllocation allocation, TimeValue minTime) {
        if (allocation == null) {
            allocation = TimeAcctAllocation.EMPTY;
        }
        this.allocation = allocation;
        this.minTime    = minTime;
    }

    public TimeAcctModel(ISPProgram prog)  {
        SPProgram dataObj = (SPProgram) prog.getDataObject();

        TimeAcctAllocation allocation = dataObj.getTimeAcctAllocation();
        if (allocation == null) allocation = TimeAcctAllocation.EMPTY;
        this.allocation = allocation;
        this.minTime    = dataObj.getMinimumTime();
    }

    public void apply(SPProgram dataObj) {
        dataObj.setTimeAcctAllocation(allocation);
        dataObj.setMinimumTime(minTime);
    }

    public TimeAcctAllocation getAllocation() {
        return allocation;
    }

    public TimeValue getMinimumTime() {
        return minTime;
    }
}

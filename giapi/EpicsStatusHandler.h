#ifndef EPICSSTATUSHANDLER_H_
#define EPICSSTATUSHANDLER_H_
#include <tr1/memory>
#include <giapi/EpicsStatusItem.h>

namespace giapi {

/**
 * Interface for Epics Status Handlers.  
 */
class EpicsStatusHandler {
public:

	/**
	 * Callback invoked when the EPICS status item
	 * is updated. Instrument code will use this
	 * method to access the monitored epics 
	 * status item values
	 * 
	 * @param item A smart pointer to the epics status item being 
	 *             monitored
	 */
	virtual void channelChanged(pEpicsStatusItem item) = 0;

	
	virtual ~EpicsStatusHandler() {};
};

typedef std::tr1::shared_ptr<EpicsStatusHandler> pEpicsStatusHandler;

}

#endif /*EPICSSTATUSHANDLER_H_*/

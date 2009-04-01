
#ifndef EPICSCHANNELSMANAGER_H_
#define EPICSCHANNELSMANAGER_H_

#include <tr1/memory>
#include <giapi/giapiexcept.h>
#include <giapi/EpicsStatusHandler.h>

namespace giapi {

/**
 * This is an interface for implementations that will
 * provide support to handle subscriptions to epics status
 * items
 */
class EpicsManager {

public:

	virtual int subscribeEpicsStatus(const std::string &name,
			pEpicsStatusHandler handler) throw (GiapiException) = 0;

	virtual int unsubscribeEpicsStatus(const std::string &name)
			throw (GiapiException) = 0;

	EpicsManager() {}
	virtual ~EpicsManager() {}
};

typedef std::auto_ptr<EpicsManager> pEpicsManager;

}


#endif /* EPICSCHANNELSMANAGER_H_ */

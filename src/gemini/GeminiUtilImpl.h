#ifndef GEMINIUTILIMPL_H_
#define GEMINIUTILIMPL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>
#include <giapi/giapi.h>
#include <giapi/EpicsStatusHandler.h>
#include <gemini/epics/EpicsManager.h>


namespace giapi {

/**
 * This is the delegate class used to implement the behavior
 * described in the Gemini Util class.
 */

class GeminiUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	static GeminiUtilImpl& Instance() throw (GiapiException);

	int subscribeEpicsStatus(const std::string &name, pEpicsStatusHandler handler) throw (GiapiException);

	int unsubscribeEpicsStatus(const std::string &name);

	int postPcsUpdate(double zernikes[], int size);

	int getTcsContext(TcsContext& ctx) const;

	virtual ~GeminiUtilImpl();
private:
	static std::auto_ptr<GeminiUtilImpl> INSTANCE;

	pEpicsManager _epicsMgr;
	GeminiUtilImpl() throw (GiapiException);

};

}

#endif /*GEMINIUTILIMPL_H_*/

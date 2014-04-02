#ifndef GEMINIUTILIMPL_H_
#define GEMINIUTILIMPL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>
#include <giapi/giapi.h>
#include <giapi/EpicsStatusHandler.h>
#include <gemini/epics/EpicsManager.h>
#include <gemini/epics/EpicsFetcher.h>
#include <gemini/pcs/PcsUpdater.h>
#include <gemini/tcs/TcsFetcher.h>

namespace giapi {

/**
 * This is the delegate class used to implement the behavior
 * described in the Gemini Util class.
 */
class GeminiUtilImpl;
typedef std::tr1::shared_ptr<GeminiUtilImpl> pGeminiUtilImpl;

class GeminiUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	static pGeminiUtilImpl Instance() throw (GiapiException);

	int subscribeEpicsStatus(const std::string &name, pEpicsStatusHandler handler) throw (GiapiException);

	int unsubscribeEpicsStatus(const std::string &name);

	int postPcsUpdate(double zernikes[], int size);

	int getTcsContext(TcsContext& ctx, long timeout) const throw (GiapiException);

	pEpicsStatusItem getChannel(const std::string &name, long timeout) throw (GiapiException);

	virtual ~GeminiUtilImpl();
private:
	static pGeminiUtilImpl INSTANCE;

	/**
	 * Manager of Epics subscriptions
	 */
	pEpicsManager _epicsMgr;

	/**
	 * The PCS updater object
	 */
	gemini::pcs::pPcsUpdater _pcsUpdater;

	/**
	 * The TCS fetcher object
	 */
	gemini::tcs::pTcsFetcher _tcsFetcher;

	/**
	 * The EPICS fetcher object
	 */
	gemini::epics::pEpicsFetcher _epicsFetcher;

	GeminiUtilImpl() throw (GiapiException);

};

}

#endif /*GEMINIUTILIMPL_H_*/

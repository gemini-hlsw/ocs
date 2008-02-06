#ifndef GEMINIUTILIMPL_H_
#define GEMINIUTILIMPL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>
#include <giapi/giapi.h>
#include <giapi/EpicsStatusHandler.h>

namespace giapi {

class GeminiUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	static GeminiUtilImpl& Instance();

	int subscribeEPICSStatus(const char *name, pEPICSStatusHandler handler);

	int unsubscribeEPISCSStatus(const char *name);

	int postPCSUpdate(double zernikes[], int size);

	int getTCSContext(TCSContext& ctx) const;

	virtual ~GeminiUtilImpl();
private:
	static std::auto_ptr<GeminiUtilImpl> INSTANCE;
	GeminiUtilImpl();

};

}

#endif /*GEMINIUTILIMPL_H_*/

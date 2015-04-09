#include "GeminiUtilImpl.h"
#include <cstdio>
#include <gemini/epics/jms/JmsEpicsManager.h>
#include <gemini/pcs/jms/JmsPcsUpdater.h>
#include <gemini/tcs/jms/JmsTcsFetcher.h>
#include <gemini/epics/jms/JmsEpicsFetcher.h>

namespace giapi {

log4cxx::LoggerPtr GeminiUtilImpl::logger(log4cxx::Logger::getLogger("giapi.GeminiUtilImpl"));

pGeminiUtilImpl GeminiUtilImpl::INSTANCE(static_cast<GeminiUtilImpl *>(0));

GeminiUtilImpl::GeminiUtilImpl() throw (GiapiException) {
	_epicsMgr = JmsEpicsManager::create();
	_pcsUpdater = gemini::pcs::jms::JmsPcsUpdater::create();
	_tcsFetcher = gemini::tcs::jms::JmsTcsFetcher::create();
	_epicsFetcher = gemini::epics::JmsEpicsFetcher::create();
}

GeminiUtilImpl::~GeminiUtilImpl() {
	LOG4CXX_DEBUG(logger,  "Destroying Gemini Util Service");
	_epicsMgr.reset();
	_pcsUpdater.reset();
	_tcsFetcher.reset();
	_epicsFetcher.reset();
}

pGeminiUtilImpl GeminiUtilImpl::Instance() throw (GiapiException) {
	if (INSTANCE.get() == 0) {
		INSTANCE.reset(new GeminiUtilImpl());
	}
	return INSTANCE;
}

int GeminiUtilImpl::subscribeEpicsStatus(const std::string &name,
		pEpicsStatusHandler handler) throw (GiapiException) {
	LOG4CXX_INFO(logger, "Subscribe epics status " << name);
	return _epicsMgr->subscribeEpicsStatus(name, handler);
}

int GeminiUtilImpl::unsubscribeEpicsStatus(const std::string &name) {
	LOG4CXX_INFO(logger, "Unsubscribe epics status " << name);
	return _epicsMgr->unsubscribeEpicsStatus(name);
}

int GeminiUtilImpl::postPcsUpdate(double zernikes[], int size) {

	std::string str;
	for (int i = 0; i < size; i++) {
		char z[25];
		sprintf(z, "%.2f ", zernikes[i]);
		str.append(z);
	}

	LOG4CXX_INFO(logger, "postPCSUpdate: " << str);
	return _pcsUpdater->postPcsUpdate(zernikes, size);
}

int GeminiUtilImpl::getTcsContext(TcsContext& ctx, long timeout) const throw (GiapiException) {
	return _tcsFetcher->fetch(ctx, timeout);
}

pEpicsStatusItem GeminiUtilImpl::getChannel(const std::string &name, long timeout) throw (GiapiException)  {
	std::cout << "Destroying " << std::endl;
	return _epicsFetcher->getChannel(name, timeout);
}

}

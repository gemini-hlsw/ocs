#include "GeminiUtilImpl.h"
#include "JmsEpicsManager.h"
namespace giapi {

log4cxx::LoggerPtr GeminiUtilImpl::logger(log4cxx::Logger::getLogger("giapi.GeminiUtilImpl"));

std::auto_ptr<GeminiUtilImpl> GeminiUtilImpl::INSTANCE(0);

GeminiUtilImpl::GeminiUtilImpl() throw (GiapiException) {
	_epicsMgr = JmsEpicsManager::create();
}

GeminiUtilImpl::~GeminiUtilImpl() {
	_epicsMgr.reset();
}

GeminiUtilImpl& GeminiUtilImpl::Instance() throw (GiapiException) {
	if (INSTANCE.get() == 0) {
		INSTANCE.reset(new GeminiUtilImpl());
	}
	return *INSTANCE;
}

int GeminiUtilImpl::subscribeEpicsStatus(const std::string &name,
		pEpicsStatusHandler handler) throw (GiapiException) {
	LOG4CXX_INFO(logger, "Subscribe epics status " << name);
	return _epicsMgr->subscribeEpicsStatus(name, handler);
}

int GeminiUtilImpl::unsubscribeEpicsStatus(const std::string &name) {
	LOG4CXX_INFO(logger, "Unsubscribe epics status " << name);
	return status::OK;
}

int GeminiUtilImpl::postPcsUpdate(double zernikes[], int size) {

	std::string str;
	for (int i = 0; i < size; i++) {
		char z[25];
		sprintf(z, "%.2f ", zernikes[i]);
		str.append(z);
	}

	LOG4CXX_INFO(logger, "postPCSUpdate: " << str);
	return status::OK;

}

int GeminiUtilImpl::getTcsContext(TcsContext& ctx) const {
	LOG4CXX_INFO(logger, "Get TCSContext ");
	ctx.tel.fl = 0.4334;
	return status::OK;
}

}

#include "GeminiUtilImpl.h"

namespace giapi {

log4cxx::LoggerPtr GeminiUtilImpl::logger(log4cxx::Logger::getLogger("giapi.GeminiUtilImpl"));

std::auto_ptr<GeminiUtilImpl> GeminiUtilImpl::INSTANCE(new GeminiUtilImpl());

GeminiUtilImpl::GeminiUtilImpl() {
}

GeminiUtilImpl::~GeminiUtilImpl() {
}

GeminiUtilImpl& GeminiUtilImpl::Instance() {
	return *INSTANCE;
}

int GeminiUtilImpl::subscribeEPICSStatus(const char *name,
		pEPICSStatusHandler handler) {
	LOG4CXX_INFO(logger, "Subscribe epics status " << name);
	return status::OK;

}

int GeminiUtilImpl::unsubscribeEPISCSStatus(const char *name) {
	LOG4CXX_INFO(logger, "Unsubscribe epics status " << name);
	return status::OK;
}

int GeminiUtilImpl::postPCSUpdate(double zernikes[], int size) {
	
	std::string str;
	for (int i = 0; i < size; i++) {
		char z[25];
		sprintf(z, "%.2f ", zernikes[i]);
		str.append(z);
	}
	
	LOG4CXX_INFO(logger, "postPCSUpdate: " << str);
	return status::OK;

}

int GeminiUtilImpl::getTCSContext(TCSContext& ctx) const {
	LOG4CXX_INFO(logger, "Get TCSContext ");
	return status::OK;
}

}

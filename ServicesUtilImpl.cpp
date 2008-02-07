#include "ServicesUtilImpl.h"
#include <time.h>
namespace giapi {

log4cxx::LoggerPtr ServicesUtilImpl::logger(log4cxx::Logger::getLogger("giapi.ServicesUtilImpl"));

std::auto_ptr<ServicesUtilImpl>
		ServicesUtilImpl::INSTANCE(new ServicesUtilImpl());

ServicesUtilImpl::ServicesUtilImpl() {
}

ServicesUtilImpl::~ServicesUtilImpl() {
}

ServicesUtilImpl& ServicesUtilImpl::Instance() {
	return *INSTANCE;
}

void ServicesUtilImpl::systemLog(log::Level level, const char *msg) {
	switch (level) {
	case log::INFO:LOG4CXX_INFO(logger, msg)
		break;
	case log::WARNING:LOG4CXX_WARN(logger, msg)
		break;
	case log::SEVERE:LOG4CXX_ERROR(logger, msg)
		break;
	case log::ALL:LOG4CXX_INFO(logger, msg)
		break;
	}
}

long64 ServicesUtilImpl::getObservatoryTime() {
	long64 time;
	struct timeval tv;
	if (gettimeofday(&tv, NULL) == 0) {
		//convert the structure to milliseconds
		time = ((long long)tv.tv_sec)*1000 + (long long)tv.tv_usec/1000;
		LOG4CXX_INFO(logger, "getObservatoryTime():  " << time);

	} else {
		LOG4CXX_ERROR(logger, "Can't get observatory time");
	}
	return time;
}

const char * ServicesUtilImpl::getProperty(const char *key) {
	LOG4CXX_INFO(logger, "Property requested for key " << key << " UNKNOWN Value");
	return "UNKNOWN";
}

}

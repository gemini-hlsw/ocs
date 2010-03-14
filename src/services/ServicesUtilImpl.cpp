#include "ServicesUtilImpl.h"
#include <sys/time.h>
namespace giapi {

log4cxx::LoggerPtr ServicesUtilImpl::logger(log4cxx::Logger::getLogger("giapi.ServicesUtilImpl"));

pServicesUtilImpl ServicesUtilImpl::INSTANCE(static_cast<ServicesUtilImpl *>(0));

ServicesUtilImpl::ServicesUtilImpl() throw (CommunicationException) {
	_producer = RequestProducer::create();
	_logProducer = JmsLogProducer::create();
}

ServicesUtilImpl::~ServicesUtilImpl() {
	LOG4CXX_DEBUG(logger, "Destroying Services Util");
}

pServicesUtilImpl ServicesUtilImpl::Instance() throw (CommunicationException) {
	if (INSTANCE.get() == 0) {
		INSTANCE.reset(new ServicesUtilImpl());
	}
	return INSTANCE;
}

void ServicesUtilImpl::systemLog(log::Level level, const std::string &msg)
	throw (CommunicationException) {

	_logProducer->postLog(level, msg);

	switch (level) {
	case log::INFO:LOG4CXX_INFO(logger, msg)
		break;
	case log::WARNING:LOG4CXX_WARN(logger, msg)
		break;
	case log::SEVERE:LOG4CXX_ERROR(logger, msg)
		break;
	default:
		return; //do nothing.
	}
}

long64 ServicesUtilImpl::getObservatoryTime() {
	long64 time = 0;
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

const std::string ServicesUtilImpl::getProperty(const std::string &key, long timeout)
	throw (CommunicationException, TimeoutException) {
	LOG4CXX_INFO(logger, "Property requested for key: " << key);

	return _producer->getProperty(key, timeout);

}

}

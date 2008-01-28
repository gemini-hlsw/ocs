#include "LogStatusSender.h"
#include "StatusItem.h"
#include "StatusDatabase.h"
#include <giapi/giapi.h>

namespace giapi {

log4cxx::LoggerPtr LogStatusSender::logger(log4cxx::Logger::getLogger("giapi.LogStatusSender"));

LogStatusSender::LogStatusSender() {
}

LogStatusSender::~LogStatusSender() {
}

int LogStatusSender::postStatus(const char* name) const throw (PostException) {
	
	StatusItem * statusItem = StatusDatabase::Instance().getStatusItem(name);

	if (statusItem == 0) {
		LOG4CXX_WARN(logger, "No status item found for " << name << ". Not posting");
		return status::ERROR;
	}
	
	if (!statusItem->isChanged()) {
		LOG4CXX_WARN(logger, "Status Item " << name << " hasn't changed since last post. Not posting");
		return status::WARNING;;
	}
	
	const std::type_info& typeInfo = statusItem->getType();
		
	statusItem->clearChanged(); //mark clean, so it can be posted again
	//Post It. Basically, log 
	if (typeInfo == typeid(int)) {
		LOG4CXX_INFO(logger, "Post Status Item " << statusItem->getName() 
				<< " Value : " << statusItem->getValueAsInt());
	}

	if (typeInfo == typeid(const char *)) {
		LOG4CXX_INFO(logger, "Post Status Item " << statusItem->getName() 
						<< " Value : " << statusItem->getValueAsString());
	}
	return status::OK;
}

int LogStatusSender::postStatus() const throw (PostException) {
	return status::ERROR;
}

}

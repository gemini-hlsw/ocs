#include "LogStatusSender.h"
#include "StatusItem.h"
#include "StatusDatabase.h"
#include <giapi/giapi.h>

#include <vector>

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

	return postStatus(statusItem);
}

int LogStatusSender::postStatus() const throw (PostException) {
	//get the status items

	const vector<StatusItem *>& items = StatusDatabase::Instance().getStatusItems();
	//and post the ones that haven't changed. Clear their status 
	for (vector<StatusItem *>::const_iterator it = items.begin(); it
			!=items.end(); ++it) {
		StatusItem * item = *it;
		postStatus(item);
	}
	return status::OK;
}

//******AUXILIARY METHODS

int LogStatusSender::postStatus(StatusItem *statusItem) const
		throw (PostException) {

	if (statusItem == 0)
		return giapi::status::ERROR;

	//value hasn't changed since last post, return immediately. 
	if (!statusItem->isChanged()) {
		return status::OK;
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
	if (typeInfo == typeid(double)) {
		LOG4CXX_INFO(logger, "Post Status Item " << statusItem->getName()
				<< " Value : " << statusItem->getValueAsDouble());
	}

	return status::OK;
}

}

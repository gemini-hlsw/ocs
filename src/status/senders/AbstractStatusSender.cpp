/*
 * AbstractStatusSender.cpp
 *
 *  Created on: Dec 18, 2008
 *      Author: anunez
 */

#include <status/senders/AbstractStatusSender.h>
#include <status/StatusDatabase.h>

namespace giapi {

log4cxx::LoggerPtr AbstractStatusSender::logger(log4cxx::Logger::getLogger(
		"giapi.AbstractStatusSender"));

AbstractStatusSender::AbstractStatusSender() {
}

AbstractStatusSender::~AbstractStatusSender() {
}

int AbstractStatusSender::postStatus(const std::string &name) const
		throw (PostException) {

	StatusItem * statusItem = StatusDatabase::Instance().getStatusItem(name);

	if (statusItem == 0) {
		LOG4CXX_WARN(logger, "No status item found for " << name << ". Not posting");
		return status::ERROR;
	}

	return doPost(statusItem);
}

int AbstractStatusSender::postStatus() const throw (PostException) {
	//get the status items
	const vector<StatusItem *>& items =
			StatusDatabase::Instance().getStatusItems();
	//and post the ones that haven't changed. Clear their status
	for (vector<StatusItem *>::const_iterator it = items.begin(); it
			!= items.end(); ++it) {
		StatusItem * item = *it;
		doPost(item);
	}
	return status::OK;
}

int AbstractStatusSender::doPost(StatusItem * statusItem) const throw (PostException) {
	if (statusItem == 0)
		return giapi::status::ERROR;

	//value hasn't changed since last post, return immediately.
	if (!statusItem->isChanged()) {
		return status::ERROR;
	}

	statusItem->clearChanged(); //mark clean, so it can be posted again
	//Post It. Invoke an specific post mechanism delegated to implementors
	return postStatus(statusItem);
}

}

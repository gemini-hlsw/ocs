#include "StatusDatabase.h"

#include <giapi/StatusUtil.h>
#include <status/senders/StatusSender.h>

#include <status/senders/StatusSenderFactory.h>

namespace giapi {

StatusUtil::StatusUtil() {
}
StatusUtil::~StatusUtil() {
}

int StatusUtil::postStatus(const std::string &name) throw (GiapiException) {
	pStatusSender sender = StatusSenderFactory::Instance()->getStatusSender();
	return sender->postStatus(name);
}

int StatusUtil::postStatus() throw (GiapiException) {
	pStatusSender sender = StatusSenderFactory::Instance()->getStatusSender();
	return sender->postStatus();
}

int StatusUtil::setValueAsInt(const std::string &name, int value) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->setStatusValueAsInt(name, value);
}

int StatusUtil::setValueAsString(const std::string &name, const std::string &value) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->setStatusValueAsString(name, value);
}

int StatusUtil::setValueAsDouble(const std::string &name, double value) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->setStatusValueAsDouble(name, value);
}

int StatusUtil::setValueAsFloat(const std::string &name, float value) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->setStatusValueAsFloat(name, value);
}

int StatusUtil::createStatusItem(const std::string &name, const type::Type type) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->createStatusItem(name, type);
}

int StatusUtil::createAlarmStatusItem(const std::string &name, const type::Type type) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->createAlarmStatusItem(name, type);
}

int StatusUtil::createHealthStatusItem(const std::string &name) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->createHealthStatusItem(name);
}

int StatusUtil::setHealth(const std::string &name, const health::Health health) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->setHealth(name, health);

}

int StatusUtil::setAlarm(const std::string &name,
		const alarm::Severity severity, const alarm::Cause cause,
		const std::string &message) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->setAlarm(name, severity, cause, message);
}

int StatusUtil::clearAlarm(const std::string & name) {
	pStatusDatabase database = StatusDatabase::Instance();
	return database->clearAlarm(name);
}


}

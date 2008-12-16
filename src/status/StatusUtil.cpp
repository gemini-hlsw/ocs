#include "StatusDatabase.h"

#include <giapi/StatusUtil.h>
#include <status/senders/StatusSender.h>

#include "StatusFactory.h"

namespace giapi {

StatusUtil::StatusUtil() {
}
StatusUtil::~StatusUtil() {
}

int StatusUtil::postStatus(const char* name) throw (GiapiException) {
	StatusSender & sender = StatusFactory::Instance().getStatusSender();
	return sender.postStatus(name);
}

int StatusUtil::postStatus() throw (GiapiException) {
	StatusSender & sender = StatusFactory::Instance().getStatusSender();
	return sender.postStatus();
}

int StatusUtil::setValueAsInt(const char* name, int value) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.setStatusValueAsInt(name, value);
}

int StatusUtil::setValueAsString(const char* name, const char *value) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.setStatusValueAsString(name, value);
}

int StatusUtil::setValueAsDouble(const char* name, double value) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.setStatusValueAsDouble(name, value);
}

int StatusUtil::createStatusItem(const char* name, const type::Type type) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.createStatusItem(name, type);
}

int StatusUtil::createAlarmStatusItem(const char* name, const type::Type type) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.createAlarmStatusItem(name, type);
}

int StatusUtil::createHealthStatusItem(const char* name) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.createHealthStatusItem(name);
}

int StatusUtil::setHealth(const char *name, const health::Health health) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.setHealth(name, health);

}

int StatusUtil::setAlarm(const char *name,
		const alarm::Severity severity, const alarm::Cause cause,
		const char *message) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.setAlarm(name, severity, cause, message);
}

int StatusUtil::clearAlarm(const char *name) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.clearAlarm(name);
}


}

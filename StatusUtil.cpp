#include "StatusDatabase.h"

#include <giapi/StatusUtil.h>
#include <giapi/StatusFactory.h>
#include <giapi/StatusSender.h>


namespace giapi {

StatusUtil::StatusUtil() {
}
StatusUtil::~StatusUtil() {
}

int StatusUtil::postStatus(const char* name) throw (PostException) {
	StatusSender & sender = StatusFactory::Instance().getStatusSender();
	return sender.postStatus(name);
}

int StatusUtil::postStatus() throw (PostException) {
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

int StatusUtil::createStatusItem(const char* name, const type::Type type) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.createStatusItem(name, type);
}

int StatusUtil::createAlarmStatusItem(const char* name, const type::Type type) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.createAlarmStatusItem(name, type);
}

int StatusUtil::setAlarm(const char *name,
		alarm::Severity severity, alarm::Cause cause, 
		const char *message) {
	StatusDatabase& database = StatusDatabase::Instance(); 
	return database.setAlarm(name, severity, cause, message);
}

}

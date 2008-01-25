#include "giapi/StatusUtil.h"
#include "giapi/StatusFactory.h"
#include "giapi/StatusSender.h"
#include "StatusDatabase.h"

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

int StatusUtil::createStatusItem(const char* name) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.createStatusItem(name);
}

int StatusUtil::createAlarmStatusItem(const char* name) {
	StatusDatabase& database = StatusDatabase::Instance();
	return database.createAlarmStatusItem(name);
}

}

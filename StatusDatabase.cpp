#include "StatusDatabase.h"

#include <giapi/giapi.h>
#include "AlarmStatusItem.h"

namespace giapi {

log4cxx::LoggerPtr StatusDatabase::logger(log4cxx::Logger::getLogger("giapi.StatusDatabase"));

StatusDatabase* StatusDatabase::INSTANCE = 0;

StatusDatabase::StatusDatabase() {
}

StatusDatabase::~StatusDatabase() {
	//TODO: Remove all the objects
}

StatusDatabase& StatusDatabase::Instance() {
	if (INSTANCE == 0) {
		INSTANCE = new StatusDatabase();
	}
	return *INSTANCE;
}

int StatusDatabase::createStatusItem(const char* name) {

	if (getStatusItem(name) != 0) {
		//status item already present. 
		LOG4CXX_WARN(logger, "A status item with the name " << name << " already created. No action");
		return status::GIAPI_NOK;
	}
	LOG4CXX_INFO(logger, "Creating a Status Item for " << name);
	//make a new status item and store it in the map. 
	StatusItem *item = new StatusItem(name);
	_map[name] = item;
	return status::GIAPI_OK;

}

int StatusDatabase::createAlarmStatusItem(const char* name) {

	if (getStatusItem(name) != 0) {
		//status item already present. 
		LOG4CXX_DEBUG(logger, "StatusDatabase::createAlarmStatusItem. A status item "
				" with the name " << name << " already created. No action");
		return status::GIAPI_NOK;
	}

	//make a new status item and store it in the map. 
	StatusItem *item = new AlarmStatusItem(name);
	_map[name] = item;
	return status::GIAPI_OK;

}

int StatusDatabase::setStatusValueAsInt(const char* name, int value) {
	StatusItem* statusItem = getStatusItem(name);
	if (statusItem == 0) {
		return status::GIAPI_NOK;
	}
	return statusItem->setValueAsInt(value);
}

int StatusDatabase::setStatusValueAsString(const char* name, const char * value) {
	StatusItem* statusItem = getStatusItem(name);
	if (statusItem == 0) {
		return status::GIAPI_NOK;
	}
	return statusItem->setValueAsString(value);
}

StatusItem * StatusDatabase::getStatusItem(const char* name) {
	if (name == 0) {
		return (StatusItem *)0; //NULL
	}
	return _map[name];
}

}
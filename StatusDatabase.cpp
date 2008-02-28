#include "StatusDatabase.h"

#include <giapi/giapi.h>
#include "AlarmStatusItem.h"
#include "HealthStatusItem.h"

namespace giapi {

log4cxx::LoggerPtr StatusDatabase::logger(log4cxx::Logger::getLogger("giapi.StatusDatabase"));

std::auto_ptr<StatusDatabase> StatusDatabase::INSTANCE(new StatusDatabase());

StatusDatabase::StatusDatabase() {
}

StatusDatabase::~StatusDatabase() {
	//TODO: Remove all the objects
	LOG4CXX_DEBUG(logger, "Cleaning database...");
}

StatusDatabase& StatusDatabase::Instance() {
	return *INSTANCE;
}

int StatusDatabase::createStatusItem(const char* name, const type::Type type) {

	if (getStatusItem(name) != 0) {
		//status item already present. 
		LOG4CXX_DEBUG(logger, "StatusDatabase::createStatusItem. A status item "
				" with the name " << name << " already created. No action");
		return status::ERROR;
	}
	LOG4CXX_DEBUG(logger, "Creating a Status Item for " << name);
	//make a new status item and store it in the map. 
	StatusItem *item = new StatusItem(name, type);
	_map[name] = item;
	_statusItemList.push_back(item);
	return status::OK;

}

int StatusDatabase::createAlarmStatusItem(const char* name,
		const type::Type type) {

	if (getStatusItem(name) != 0) {
		//status item already present. 
		LOG4CXX_DEBUG(logger, "StatusDatabase::createAlarmStatusItem. A status item "
				" with the name " << name << " already created. No action");
		return status::ERROR;
	}
	LOG4CXX_DEBUG(logger, "Creating an Alarm Status Item for " << name);
	//make a new status item and store it in the map. 
	StatusItem *item = new AlarmStatusItem(name, type);
	_map[name] = item;
	_statusItemList.push_back(item);
	return status::OK;
}

int StatusDatabase::createHealthStatusItem(const char* name) {

	if (getStatusItem(name) != 0) {
		//status item already present. 
		LOG4CXX_DEBUG(logger, "StatusDatabase::createHealthStatusItem. A status item "
				" with the name " << name << " already created. No action");
		return status::ERROR;
	}

	LOG4CXX_DEBUG(logger, "Creating a Health Status Item for " << name);
	//make a new status item and store it in the map. 
	StatusItem *item = new HealthStatusItem(name);
	_map[name] = item;
	_statusItemList.push_back(item);
	return status::OK;
}

int StatusDatabase::setHealth(const char *name, const health::Health health) {
	StatusItem * statusItem = getStatusItem(name);
	if (statusItem == 0) {
		return status::ERROR;
	}

	HealthStatusItem* healthStatusItem =
			dynamic_cast<HealthStatusItem*>(statusItem);

	if (healthStatusItem == 0) {
		LOG4CXX_WARN(logger, name << " is not a health status item, aborting operation");
		return status::ERROR;
	}

	//set the values in the alarm item
	return healthStatusItem->setHealth(health);
}

int StatusDatabase::setStatusValueAsInt(const char* name, int value) {
	StatusItem* statusItem = getStatusItem(name);
	if (statusItem == 0) {
		return status::ERROR;
	}
	return statusItem->setValueAsInt(value);
}

int StatusDatabase::setStatusValueAsString(const char* name, const char * value) {
	StatusItem* statusItem = getStatusItem(name);
	if (statusItem == 0) {
		return status::ERROR;
	}
	return statusItem->setValueAsString(value);
}

StatusItem * StatusDatabase::getStatusItem(const char* name) {
	if (name == 0) {
		return (StatusItem *)0; //NULL
	}
	return _map[name];
}

int StatusDatabase::setAlarm(const char *name, const alarm::Severity severity,
		const alarm::Cause cause, const char* message) {

	StatusItem * statusItem = getStatusItem(name);
	if (statusItem == 0) {
		return status::ERROR;
	}

	AlarmStatusItem* alarmStatusItem =
			dynamic_cast<AlarmStatusItem*>(statusItem);

	if (alarmStatusItem == 0) {
		LOG4CXX_WARN(logger, name << " is not an alarm status item, aborting operation");
		return status::ERROR;
	}

	//set the values in the alarm item
	return alarmStatusItem->setAlarmState(severity, cause, message);
}

int StatusDatabase::clearAlarm(const char *name) {
	StatusItem * statusItem = getStatusItem(name);
	if (statusItem == 0) {
		return status::ERROR;
	}

	AlarmStatusItem* alarmStatusItem =
			dynamic_cast<AlarmStatusItem*>(statusItem);

	if (alarmStatusItem == 0) {
		LOG4CXX_WARN(logger, name << " is not an alarm status item, aborting operation");
		return status::ERROR;
	}

	//set the values in the alarm item
	alarmStatusItem->clearAlarmState();
	return status::OK;
}

const vector<StatusItem *>& StatusDatabase::getStatusItems() {
	return _statusItemList;
}



}

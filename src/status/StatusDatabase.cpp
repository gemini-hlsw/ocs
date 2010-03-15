#include "StatusDatabase.h"

#include <giapi/giapi.h>
#include "AlarmStatusItem.h"
#include "HealthStatusItem.h"

namespace giapi {

log4cxx::LoggerPtr StatusDatabase::logger(log4cxx::Logger::getLogger("giapi.StatusDatabase"));

pStatusDatabase StatusDatabase::INSTANCE(new StatusDatabase());

StatusDatabase::StatusDatabase() {
}

StatusDatabase::~StatusDatabase() {
	LOG4CXX_DEBUG(logger, "Cleaning database...");	
	_statusItemList.clear();
	_map.clear();
}

pStatusDatabase StatusDatabase::Instance() {
	return INSTANCE;
}

int StatusDatabase::createStatusItem(const std::string & name, const type::Type type) {

	if (getStatusItem(name).get() != 0) {
		//status item already present.
		LOG4CXX_DEBUG(logger, "StatusDatabase::createStatusItem. A status item "
				" with the name " << name << " already created. No action");
		return status::ERROR;
	}
	LOG4CXX_DEBUG(logger, "Creating a Status Item for " << name);
	//make a new status item and store it in the map.
	pStatusItem item(new StatusItem(name, type));
	_map[name] = item;
	_statusItemList.push_back(item);
	return status::OK;

}

int StatusDatabase::createAlarmStatusItem(const std::string &name,
		const type::Type type) {

	if (getStatusItem(name).get() != 0) {
		//status item already present.
		LOG4CXX_DEBUG(logger, "StatusDatabase::createAlarmStatusItem. A status item "
				" with the name " << name << " already created. No action");
		return status::ERROR;
	}
	LOG4CXX_DEBUG(logger, "Creating an Alarm Status Item for " << name);
	//make a new status item and store it in the map.
	pStatusItem item(new AlarmStatusItem(name, type));
	_map[name] = item;
	_statusItemList.push_back(item);
	return status::OK;
}

int StatusDatabase::createHealthStatusItem(const std::string & name) {

	if (getStatusItem(name).get() != 0) {
		//status item already present.
		LOG4CXX_DEBUG(logger, "StatusDatabase::createHealthStatusItem. A status item "
				" with the name " << name << " already created. No action");
		return status::ERROR;
	}

	LOG4CXX_DEBUG(logger, "Creating a Health Status Item for " << name);
	//make a new status item and store it in the map.
	pStatusItem item(new HealthStatusItem(name));
	_map[name] = item;
	_statusItemList.push_back(item);
	return status::OK;
}

int StatusDatabase::setHealth(const std::string &name, const health::Health health) {
	pStatusItem statusItem = getStatusItem(name);
	if (statusItem.get() == 0) {
		return status::ERROR;
	}

	HealthStatusItem* healthStatusItem =
			dynamic_cast<HealthStatusItem*>(statusItem.get());

	if (healthStatusItem == 0) {
		LOG4CXX_WARN(logger, name << " is not a health status item, aborting operation");
		return status::ERROR;
	}

	//set the values in the alarm item
	return healthStatusItem->setHealth(health);
}

int StatusDatabase::setStatusValueAsInt(const std::string & name, int value) {
	pStatusItem statusItem = getStatusItem(name);
	if (statusItem.get() == 0) {
		return status::ERROR;
	}
	return statusItem->setValueAsInt(value);
}

int StatusDatabase::setStatusValueAsString(const std::string & name, const std::string & value) {
	pStatusItem statusItem = getStatusItem(name);
	if (statusItem.get() == 0) {
		return status::ERROR;
	}
	return statusItem->setValueAsString(value);
}

int StatusDatabase::setStatusValueAsDouble(const std::string & name, double value) {
	pStatusItem statusItem = getStatusItem(name);
	if (statusItem.get() == 0) {
		return status::ERROR;
	}
	return statusItem->setValueAsDouble(value);
}

int StatusDatabase::setStatusValueAsFloat(const std::string & name, float value) {
	pStatusItem statusItem = getStatusItem(name);
	if (statusItem.get() == 0) {
		return status::ERROR;
	}
	return statusItem->setValueAsFloat(value);
}


pStatusItem StatusDatabase::getStatusItem(const std::string & name) {
	if (name.empty()) {
		return pStatusItem((StatusItem *)0); //NULL
	}
	return _map[name];
}

int StatusDatabase::setAlarm(const std::string &name, const alarm::Severity severity,
		const alarm::Cause cause, const std::string & message) {

	pStatusItem statusItem = getStatusItem(name);
	if (statusItem.get() == 0) {
		return status::ERROR;
	}

	AlarmStatusItem* alarmStatusItem =
			dynamic_cast<AlarmStatusItem*>(statusItem.get());

	if (alarmStatusItem == 0) {
		LOG4CXX_WARN(logger, name << " is not an alarm status item, aborting operation");
		return status::ERROR;
	}

	//set the values in the alarm item
	return alarmStatusItem->setAlarmState(severity, cause, message);
}

int StatusDatabase::clearAlarm(const std::string &name) {
	pStatusItem statusItem = getStatusItem(name);
	if (statusItem.get() == 0) {
		return status::ERROR;
	}

	AlarmStatusItem* alarmStatusItem =
			dynamic_cast<AlarmStatusItem*>(statusItem.get());

	if (alarmStatusItem == 0) {
		LOG4CXX_WARN(logger, name << " is not an alarm status item, aborting operation");
		return status::ERROR;
	}

	//set the values in the alarm item
	alarmStatusItem->clearAlarmState();
	return status::OK;
}

const vector<pStatusItem>& StatusDatabase::getStatusItems() {
	return _statusItemList;
}



}

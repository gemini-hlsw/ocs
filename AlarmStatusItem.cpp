#include "AlarmStatusItem.h"
#include <string.h>

namespace giapi {
AlarmStatusItem::AlarmStatusItem(const char *name, const type::Type type) :
	StatusItem(name, type) {
	_message = 0;
	_severity = alarm::ALARM_OK;
	_cause = alarm::ALARM_CAUSE_OK;
	_initialized = false;
}

AlarmStatusItem::~AlarmStatusItem() {
}


int AlarmStatusItem::setAlarmState(alarm::Severity severity, 
		alarm::Cause cause, const char * message) {
	
	//Message is required with the ALARM_CAUSE_OTHER alarm cause
	if (message == 0 && cause == alarm::ALARM_CAUSE_OTHER) {
		return giapi::status::ERROR;
	}
	
	//If the values haven't changed since last time, we issue a 
	//warning. Exception is if this is the first time we call this method
	//on this alarm status item
	if (_initialized) {
		if (severity == _severity 
				&& cause == _cause
				&& ((message == 0 && _message == 0) 
						|| (strcmp(message, _message) == 0)) ) {
			return giapi::status::WARNING; 
		}
	} else {
		_initialized = true;
	}
	
	_severity = severity;
	//if the severity is NO_ALARM, the other arguments aren't considered
	if (_severity == alarm::ALARM_OK) {
		_cause = alarm::ALARM_CAUSE_OK;
		_message = 0;
	} else {
		_cause = cause;
		_message = message;
	}
	_mark(); //mark the status item as dirty and set the timestamp
	return giapi::status::OK;
}

int AlarmStatusItem::clearAlarmState() {
	//use the set alarm state method to clear. This will mark the internal
	//state accordingly if necessary, and also will return the appropriate 
	//value to the caller
	return setAlarmState(alarm::ALARM_OK, alarm::ALARM_CAUSE_OK, (const char *)0);
}

const char * AlarmStatusItem::getMessage() const {
	return _message;
}

alarm::Severity AlarmStatusItem::getSeverity() const {
	return _severity;
}

alarm::Cause AlarmStatusItem::getCause() const {
	return _cause;
}

}

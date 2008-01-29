#include "AlarmStatusItem.h"

namespace giapi {
AlarmStatusItem::AlarmStatusItem(const char *name, const type::Type type) :
	StatusItem(name, type) {
	_message = 0;
	_severity = alarm::ALARM_OK;
	_cause = alarm::ALARM_CAUSE_OK;
}

AlarmStatusItem::~AlarmStatusItem() {
}


int AlarmStatusItem::setAlarmState(alarm::Severity severity, 
		alarm::Cause cause, const char * message) {
	
	//Message is required wiht the ALARM_CAUSE_OTHER alarm cause
	if (message == 0 && cause == alarm::ALARM_CAUSE_OTHER) {
		return giapi::status::ERROR;
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

void AlarmStatusItem::clearAlarmState() {
	_message = 0;
	_severity = alarm::ALARM_OK;
	_cause = alarm::ALARM_CAUSE_OK;
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

#include "AlarmStatusItem.h"

namespace giapi {
AlarmStatusItem::AlarmStatusItem(const char *name, const type::Type type) :
	StatusItem(name, type) {
	_message = 0;
	_severity = alarm::NO_ALARM;
	_cause = alarm::NO_CAUSE;
}

AlarmStatusItem::~AlarmStatusItem() {
}


void AlarmStatusItem::setAlarmState(alarm::Severity severity, 
		alarm::Cause cause, const char * message) {
	_severity = severity;
	//if the severity is NO_ALARM, the other arguments aren't considered
	if (_severity == alarm::NO_ALARM) {
		_cause = alarm::NO_CAUSE;
		_message = 0;
	} else {
		_cause = cause;
		_message = message;
	}
	_mark(); //mark the status item as dirty and set the timestamp
}

void AlarmStatusItem::clearAlarmState() {
	_message = 0;
	_severity = alarm::NO_ALARM;
	_cause = alarm::NO_CAUSE;
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

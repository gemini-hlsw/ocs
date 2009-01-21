#include "AlarmStatusItem.h"
#include <string.h>
#include <giapi/giapiexcept.h>

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

	//If the values haven't changed since last time, we don't mark
	// the status as dirty. Return immediately
	if (_initialized) {
		if (severity == _severity && cause == _cause && ((message == 0
				&& _message == 0) || (strcmp(message, _message) == 0))) {
			return giapi::status::OK;
		}
	} else {
		_initialized = true;
	}

	//original pointer to the message stored here
	char * oldMsg = _message;

	_severity = severity;
	//if the severity is NO_ALARM, the other arguments aren't considered
	if (_severity == alarm::ALARM_OK) {
		_cause = alarm::ALARM_CAUSE_OK;
		_message = 0;
	} else {
		_cause = cause;
		if (message != NULL) {
			_message = new char[strlen(message) + 1];
			strcpy(_message, message);
		} else {
			_message = 0;
		}
	}
	_mark(); //mark the status item as dirty and set the timestamp

	if (oldMsg != NULL) {
		delete[] oldMsg;
	}

	return giapi::status::OK;
}

void AlarmStatusItem::clearAlarmState() {
	//use the set alarm state method to clear. This will mark the internal
	//state accordingly if necessary. This can't return error.
	if (setAlarmState(alarm::ALARM_OK, alarm::ALARM_CAUSE_OK, (const char *)0)
			== status::ERROR) {
		//This shouldn't happen.
		throw InvalidOperation("AlarmStatusItem::clearAlarmState - Couldn't clear alarm item");
	}
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

void AlarmStatusItem::accept(StatusVisitor & visitor) {
	visitor.visitAlarmItem(this);
}

}

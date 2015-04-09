#include "AlarmStatusItem.h"
#include <cstdarg>
#include <string.h>
#include <giapi/giapiexcept.h>

namespace giapi {
AlarmStatusItem::AlarmStatusItem(const std::string &name, const type::Type type) :
	StatusItem(name, type) {
	_severity = alarm::ALARM_OK;
	_cause = alarm::ALARM_CAUSE_OK;
	_initialized = false;
}

AlarmStatusItem::~AlarmStatusItem() {
}

int AlarmStatusItem::setAlarmState(alarm::Severity severity,
		alarm::Cause cause, const std::string & message) {

	//Message is required with the ALARM_CAUSE_OTHER alarm cause
	if (message.empty() && cause == alarm::ALARM_CAUSE_OTHER) {
		return giapi::status::ERROR;
	}

	//If the values haven't changed since last time, we don't mark
	// the status as dirty. Return immediately
	if (_initialized) {
		if (severity == _severity && cause == _cause && (message == _message)) {
			return giapi::status::OK;
		}
	} else {
		_initialized = true;
	}

	_severity = severity;
	//if the severity is NO_ALARM, the other arguments aren't considered
	if (_severity == alarm::ALARM_OK) {
		_cause = alarm::ALARM_CAUSE_OK;
		_message.clear();
	} else {
		_cause = cause;
		_message = message;
	}
	_mark(); //mark the status item as dirty and set the timestamp

	return giapi::status::OK;
}

void AlarmStatusItem::clearAlarmState() {
	//use the set alarm state method to clear. This will mark the internal
	//state accordingly if necessary. This can't return error.
	if (setAlarmState(alarm::ALARM_OK, alarm::ALARM_CAUSE_OK)
			== status::ERROR) {
		//This shouldn't happen.
		throw InvalidOperation("AlarmStatusItem::clearAlarmState - Couldn't clear alarm item");
	}
}

const std::string & AlarmStatusItem::getMessage() const {
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

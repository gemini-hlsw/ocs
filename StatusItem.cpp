#include "StatusItem.h"

#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>

#include <time.h>

namespace giapi {

log4cxx::LoggerPtr StatusItem::logger(log4cxx::Logger::getLogger("giapi.StatusItem"));

StatusItem::StatusItem(const char *name, const type::Type type) :
	KvPair(name) {
	_time = 0;
	_changedFlag = false;
	_type = type;
}

StatusItem::~StatusItem() {
}

int StatusItem::setValueAsInt(int value) {

	//If the type is not integer, return error. 
	if (_type != type::INT) {
		LOG4CXX_WARN(logger, "Can't set an int value in the status item : " << getName());
		return status::ERROR;
	}
	//Figure out if this is a new value
	if (!_value.empty() && value == getValueAsInt()) {
		LOG4CXX_DEBUG(logger, "Values match. Won't mark as dirty. Item " << getName());
		return status::WARNING;
	}
	//set the value
	_mark();
	return KvPair::setValueAsInt(value);
}

int StatusItem::setValueAsString(const char* value) {

	//If the type is not an string, return error. 
	if (_type != type::STRING) {
		LOG4CXX_WARN(logger, "Can't set a string value in the status item : " << getName());
		return status::ERROR;
	}
	//Figure out if this is a new value
	if (!_value.empty() && strcmp(value, getValueAsString()) == 0) {
		LOG4CXX_DEBUG(logger, "Values match. Won't mark as dirty. Item " << getName());
		return status::WARNING;
	}
	_mark();
	return KvPair::setValueAsString(value);
}

bool StatusItem::isChanged() const {
	return _changedFlag;
}

void StatusItem::clearChanged() {
	_changedFlag = false;
}

/////PROTECTED METHODS

void StatusItem::_mark() {

	_changedFlag = true;
	struct timeval tv;
	if (gettimeofday(&tv, NULL) == 0) {
		//convert the structure to milliseconds
		_time = ((long long)tv.tv_sec)*1000 + (long long)tv.tv_usec/1000;
		LOG4CXX_DEBUG(logger, "Marking dirty status item " << getName() << " at " << _time);
	} else {
		LOG4CXX_WARN(logger, "Can't set timestamp on status item " << getName());
	}
}

}


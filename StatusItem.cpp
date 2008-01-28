#include "StatusItem.h"
#include "giapi/giapi.h"
#include <time.h>

namespace giapi {

log4cxx::LoggerPtr StatusItem::logger(log4cxx::Logger::getLogger("giapi.StatusItem"));

StatusItem::StatusItem(const char *name) :
	KvPair(name) {
	_time = 0;
	_changedFlag = false;
}

StatusItem::~StatusItem() {
}

int StatusItem::setValueAsInt(int value) {

	//find out if the value is different from what we 
	//already have
	if (!_value.empty() && _value.type() == typeid(int)) {
		LOG4CXX_DEBUG(logger, "Types match. Analyze equality");
		if (value == getValueAsInt()) {
			LOG4CXX_DEBUG(logger, "Values match. Won't mark as dirty");
			return status::GIAPI_WARNING;
		}
	}
	_mark();
	return KvPair::setValueAsInt(value);
}

int StatusItem::setValueAsString(const char* value) {

	//find out if the value is different from what we 
	//already have

	if (!_value.empty() && _value.type() == typeid(const char *)) {
		LOG4CXX_DEBUG(logger, "Types match. Analyze equality");
		if (strcmp(value, getValueAsString()) == 0) {
			LOG4CXX_DEBUG(logger, "Values match. Won't mark as dirty");
			return status::GIAPI_WARNING;
		}
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

	LOG4CXX_DEBUG(logger, "Marking dirty status item " << getName());
	_changedFlag = true;
	time_t currentTime = time(0);
	_time = (currentTime == (time_t)-1) ? 0 : (unsigned long)currentTime;
	LOG4CXX_DEBUG(logger, "Timestamp set to " << _time);

}

}


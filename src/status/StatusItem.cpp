#include "StatusItem.h"

#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>

#include <sys/time.h>

namespace giapi {

log4cxx::LoggerPtr StatusItem::logger(log4cxx::Logger::getLogger("giapi.StatusItem"));

StatusItem::StatusItem(const std::string &name, const type::Type type) :
	KvPair(name) {
	_mark(); //initially, the items are dirty, and the timestamp is now.
	_type = type;
	//initial values for each type:
	switch (_type) {
	case type::BOOLEAN:
		_value = false;
		break;
	case type::DOUBLE:
		_value = 0.0;
		break;
	case type::FLOAT:
		_value = (float)0.0;
		break;
	case type::INT:
		_value = 0;
		break;
	case type::BYTE:
		_value = (unsigned char)0;
		break;
	case type::SHORT:
		_value = (unsigned short)0;
		break;
	case type::STRING:
		_value = std::string(" ");
		break;
	}
}

StatusItem::~StatusItem() {
}

int StatusItem::setValueAsInt(int value) {

	//If the type is not integer, return error.
	if (_type != type::INT) {
		LOG4CXX_WARN(logger, "Can't set an int value in the status item : " << *this);
		return status::ERROR;
	}
	//Figure out if this is a new value. If they are the same, return immediately
	if (!_value.empty() && value == getValueAsInt()) {
		LOG4CXX_DEBUG(logger, "Values match. Won't mark as dirty. Item " << *this);
		return status::OK;
	}
	//set the value
	_mark();
	return KvPair::setValueAsInt(value);
}

int StatusItem::setValueAsString(const std::string &value) {

	//If the type is not an string, return error.
	if (_type != type::STRING) {
		LOG4CXX_WARN(logger, "Can't set a string value in the status item : " << *this);
		return status::ERROR;
	}
	//Figure out if this is a new value. If they are the same, return immediately
	if (!_value.empty() && (value == getValueAsString())) {
		LOG4CXX_DEBUG(logger, "Values match. Won't mark as dirty. Item " << *this);
		return status::OK;
	}

	_mark();
	return KvPair::setValueAsString(value);
}

int StatusItem::setValueAsDouble(double value) {

	//If the type is not double, return error.
	if (_type != type::DOUBLE) {
		LOG4CXX_WARN(logger, "Can't set a double value in the status item : " << *this);
		return status::ERROR;
	}
	//Figure out if this is a new value. If they are the same, return immediately
	if (!_value.empty() && value == getValueAsDouble()) {
		LOG4CXX_DEBUG(logger, "Values match. Won't mark as dirty. Item " << *this);
		return status::OK;
	}
	//set the value
	_mark();
	return KvPair::setValueAsDouble(value);
}

int StatusItem::setValueAsFloat(float value) {
	//If the type is not float, return an error
	if (_type != type::FLOAT) {
		LOG4CXX_WARN(logger, "Can't set a float value in the status item : " << *this);
		return status::ERROR;
	}

	//Figure out if this is a new value. If they are the same, return immediately
	if (!_value.empty() && value == getValueAsFloat()) {
		LOG4CXX_DEBUG(logger, "Values match. Won't mark as dirty. Item " << *this);
		return status::OK;
	}
	//set the value
	_mark();
	return KvPair::setValueAsFloat(value);

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
		LOG4CXX_DEBUG(logger, "Marking dirty status item " << *this << " at " << _time);
	} else {
		LOG4CXX_WARN(logger, "Can't set timestamp on status item " << *this);
	}
}


void StatusItem::accept(StatusVisitor &visitor) {
	visitor.visitStatusItem(this);
}

const type::Type StatusItem::getStatusType() const {
	return _type;
}

const long64 StatusItem::getTimestamp() const {
	return _time;
}

}


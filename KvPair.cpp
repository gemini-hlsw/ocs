#include "KvPair.h"
#include <giapi/giapi.h>

namespace giapi {

KvPair::KvPair(const char * name) {
	_name = name;
}

KvPair::~KvPair() {
}

int KvPair::setValueAsInt(int value) {
	_value = value;
	return status::OK;
}

int KvPair::setValueAsString(const char * value) {
	_value = value;
	return status::OK;
}

int KvPair::getValueAsInt() const {
	return boost::any_cast<int>(_value);
}

const char * KvPair::getValueAsString() const {
	return boost::any_cast<const char *>(_value);
}

const char * KvPair::getName() const {
	return _name;
}

const std::type_info& KvPair::getType() const {
	return _value.type();
}

}

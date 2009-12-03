#include "KvPair.h"
#include <giapi/giapi.h>

namespace giapi {

KvPair::KvPair(const std::string & name) {
	_name = name;
}

KvPair::~KvPair() {
}

int KvPair::setValueAsInt(int value) {
	_value = value;
	return status::OK;
}

int KvPair::setValueAsString(const std::string &value) {
	_value = value;
	return status::OK;
}

int KvPair::setValueAsDouble(double value) {
	_value = value;
	return status::OK;
}

int KvPair::setValueAsFloat(float value) {
	_value = value;
	return status::OK;
}


int KvPair::getValueAsInt() const {
	return boost::any_cast<int>(_value);
}

const std::string KvPair::getValueAsString() const {
	return boost::any_cast<std::string>(_value);
}

double KvPair::getValueAsDouble() const {
	return boost::any_cast<double>(_value);
}

float KvPair::getValueAsFloat() const {
	return boost::any_cast<float>(_value);
}


const std::string & KvPair::getName() const {
	return _name;
}

const std::type_info& KvPair::getType() const {
	return _value.type();
}

std::ostream& operator<< (std::ostream& os, const KvPair& pair) {

	os << "[name = " << pair.getName() << ", value = ";
	const std::type_info& typeInfo = pair.getType();

	if (typeInfo == typeid(int)) {
		os << pair.getValueAsInt();
	}
	if (typeInfo == typeid(std::string)) {
		os << pair.getValueAsString();
	}
	if (typeInfo == typeid(double)) {
		os << pair.getValueAsDouble();
	}
	if (typeInfo == typeid(float)) {
		os << pair.getValueAsFloat();
	}
	if (typeInfo == typeid(void)) {
		os << "void";
	}

	os << "]";

	return os;
}

}

#include "ConfigurationFactory.h"

namespace giapi {

ConfigurationImpl::ConfigurationImpl() {

}

ConfigurationImpl::~ConfigurationImpl() {

}

vector<std::string> ConfigurationImpl::getKeys() const {
	std::vector<std::string> keys(_configMap.size());

	StringStringMap::const_iterator iter;
	iter = _configMap.begin();
	for (int i = 0; iter != _configMap.end(); ++iter, ++i) {
		keys[i] = iter->first;
	}
	return keys;
}

int ConfigurationImpl::getSize() const {
	return _configMap.size();
}

const std::string ConfigurationImpl::getValue(const std::string &key) {
	return _configMap[key];
}

void ConfigurationImpl::setValue(const std::string &key, const std::string &value) {
	_configMap[key] = value;
}



ConfigurationFactory::ConfigurationFactory() {
}

ConfigurationFactory::~ConfigurationFactory() {
}

pConfiguration ConfigurationFactory::getConfiguration() {

	pConfiguration config(new ConfigurationImpl());

	return config;
}

}

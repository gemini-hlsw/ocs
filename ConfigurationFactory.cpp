#include "ConfigurationFactory.h"

namespace giapi {

ConfigurationImpl::ConfigurationImpl() {

}

ConfigurationImpl::~ConfigurationImpl() {

}

vector<const char *> ConfigurationImpl::getKeys() const {
	std::vector<const char *> keys(_configMap.size());

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

const char * ConfigurationImpl::getValue(const char * key) {
	return _configMap[key];
}

void ConfigurationImpl::setValue(const char * key, const char * value) {
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

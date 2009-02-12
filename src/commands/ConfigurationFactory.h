#ifndef CONFIGURATIONFACTORY_H_
#define CONFIGURATIONFACTORY_H_

#include <giapi/Configuration.h>
#include <util/giapiMaps.h>

#include <ext/hash_map>

//hash_map is an extension of STL widely available on gnu compilers, fortunately
//Will make its namespace visible here.
using namespace __gnu_cxx;


namespace giapi {

class ConfigurationImpl : public Configuration {
public:

	/**
	 * Return the value associated to the given key. The key is the name
	 * of the parameter associated to the value, like inst:filterA.
	 *
	 * @param key the name of the parameter whose value we need to retrieve
	 *
	 * @return the value associated to the given key or <code>NULL</code>
	 *         if there is no value associated for the key in the current
	 *         configuration.
	 */
	virtual const std::string getValue(const std::string & key);

	/**
	 * Return the keys contained in the configuration. If the configuration
	 * does not contain any key, a reference to an empty vector is returned.
	 *
	 * @return reference to a vector of strings representing the keys
	 *         contained in the configuration. An empty list is returned
	 *         if no keys are present.
	 */
	virtual vector<std::string> getKeys() const;

	/**
	 * Return the number of parameters contained in this configuration.
	 *
	 * @return number of parameters contained in this configuration.
	 */
	virtual int getSize() const;

	/**
	 * Set the value for the specified key
	 * @param key The target key
	 * @param value The value to be set
	 */
	virtual void setValue(const std::string & key, const std::string & value);

	ConfigurationImpl();
	virtual ~ConfigurationImpl();

private:
	/**
	 * Type definition for the hash_table that will map strings to
	 * values in the configuration
	 */
	typedef hash_map<std::string, std::string, hash<std::string>, util::eqstr>
			StringStringMap;

	StringStringMap _configMap;

};

class ConfigurationFactory {

public:
	static pConfiguration getConfiguration();

	virtual ~ConfigurationFactory();
private:
	ConfigurationFactory();

};

}

#endif /*CONFIGURATIONFACTORY_H_*/

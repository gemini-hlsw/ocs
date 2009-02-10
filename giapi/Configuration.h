#ifndef CONFIGURATION_H_
#define CONFIGURATION_H_
#include <tr1/memory>

#include <vector>
#include <string>

using namespace std;

namespace giapi {

/**
 * Configuration interface.
 * <p/>
 * A configuration is made up of parameters. Each parameter has a key and a
 * value. The OCS commands an instrument by demanding that the instrument
 * match a specified configuration.
 * <p/>
 * This interface provides mechanisms to:
 * a) get the value associated to a given parameter (identified by key)
 * b) get all the keys present in the configuration.
 */
class Configuration {

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
	virtual const std::string  getValue(const std::string & key) = 0;

	/**
	 * Return the keys contained in the configuration. If the configuration
	 * does not contain any key, an empty vector is returned.
	 *
	 * @return a vector of strings representing the keys
	 *         contained in the configuration. An empty list is returned
	 *         if no keys are present.
	 */
	virtual vector<std::string> getKeys() const = 0;

	/**
	 * Return the number of parameters contained in this configuration.
	 *
	 * @return number of parameters contained in this configuration.
	 */
	virtual int getSize() const = 0;

	/**
	 * Set the value for the specified key
	 * @param key The target key
	 * @param value The value to be set
	 */
	virtual void setValue(const std::string & key, const std::string & value) = 0;

	/**
	 * Destructor.
	 */
	virtual ~Configuration();
protected:
	/**
	 * Protected constructor. Configurations are built internally by the
	 * GIAPI and passed to a <code>SequenceCommandHandler</code> for its use
	 * in the form of smart pointers.
	 */
	Configuration();
public:
	/**
	 * Keyword for the Argument in the OBSERVE sequence command
	 */
	static const char * DATA_LABEL;
	/**
	 * Keyword for the Argument in the REBOOT sequence command
	 */
	static const char * REBOOT_OPT;

	/**
	 * GMP Reboot argument in the REBOOT sequence command
	 */
	static const char * GMP;
	/**
	 * Reboot argument in the REBOOT sequence command
	 */
	static const char * REBOOT;
	/**
	 * No argument in the REBOOT sequence command
	 */
	static const char * NONE;

};

/**
 * A smart pointer to Configurations. GIAPI return configurations in the
 * form of smart pointers to configurations, to help keep track of object
 * creation/destruction.
 */
typedef std::tr1::shared_ptr<Configuration> pConfiguration;
}

#endif /*CONFIGURATION_H_*/

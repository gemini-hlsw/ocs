#ifndef CONFIGURATION_H_
#define CONFIGURATION_H_
#include <tr1/memory>

#include <vector>

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
	virtual const char * getValue(const char * key) = 0;
	
	/**
	 * Return the keys contained in the configuration. If the configuration 
	 * does not contain any key, a reference to an empty vector is returned.
	 * 
	 * @return reference to a vector of strings representing the keys 
	 *         contained in the configuration. An empty list is returned
	 *         if no keys are present. 
	 */
	virtual const vector<const char *> & getKeys() = 0;
	
	/**
	 * Destructor. 
	 */
	virtual ~Configuration();
private:
	/**
	 * Private constructor. Configurations are built internally by the
	 * GIAPI and passed to a <code>SequenceCommandHandler</code> for its use
	 * in the form of smart pointers. 
	 */
	Configuration();
};

/**
 * A smart pointer to Configurations. GIAPI return configurations in the 
 * form of smart pointers to configurations, to help keep track of object
 * creation/destruction. 
 */
typedef std::tr1::shared_ptr<Configuration> pConfiguration;
}

#endif /*CONFIGURATION_H_*/

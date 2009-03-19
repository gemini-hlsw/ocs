#ifndef EPICSCONFIGURATION_H_
#define EPICSCONFIGURATION_H_

#include <string>
#include <giapi/giapiexcept.h>

namespace giapi {

/**
 * An Epics configuration object allows to retrieve and query the
 * information about valid epics channels that are available
 * in the GMP
 */
class EpicsConfiguration {
public:

	/**
	 * Return true if the given channel name is an available channel
	 * in the configuration
	 */
	virtual bool hasChannel(const std::string &channel) = 0;

	/**
	 * Initialize the configuration. This requires access to the
	 * GMP to get the values.
	 */
	virtual void init() throw (GiapiException) = 0;

	/**
	 * Return true if this epics configuration was correctly initialized
	 * and contains information from the valid epics channels from the GMP
	 */
	virtual bool isInitialized() const = 0;

	EpicsConfiguration() {};
	virtual ~EpicsConfiguration() {};
};

typedef std::auto_ptr<EpicsConfiguration> pEpicsConfiguration;

}

#endif /*EPICSCONFIGURATION_H_*/

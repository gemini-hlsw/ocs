#ifndef SERVICESUTILIMPL_H_
#define SERVICESUTILIMPL_H_

#include <cstdarg>

#include <tr1/memory>
#include <log4cxx/logger.h>

#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>

#include <services/RequestProducer.h>
#include <services/JmsLogProducer.h>

namespace giapi {

class ServicesUtilImpl;

typedef std::tr1::shared_ptr<ServicesUtilImpl> pServicesUtilImpl;

class ServicesUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:

	/**
	 * Get the singleton instance of the ServiceUtil implementation
	 */
	static pServicesUtilImpl Instance() throw (CommunicationException) ;

	/**
	 * Sends the logging information to the GMP
	 */
	void systemLog(log::Level level, const std::string &msg)
		throw (CommunicationException);

	long64 getObservatoryTime();

	/**
	 * Returns the property value for the given key. If there
	 * are no value associated to that key, an empty string
	 * is returned.
	 */
	const std::string getProperty(const std::string &key, long timeout)
			throw (CommunicationException, TimeoutException);

	/**
	 * Destructor
	 */
	virtual ~ServicesUtilImpl();

private:
	static pServicesUtilImpl INSTANCE;

	/**
	 * Private constructor
	 */
	ServicesUtilImpl() throw (CommunicationException) ;

	/**
	 * Smart pointer to the RequestProducer object
	 */
	pRequestProducer _producer;

	/**
	 * Smart pointer to the Log Producer object
	 */
	pJmsLogProducer _logProducer;


};

}

#endif /*SERVICESUTILIMPL_H_*/

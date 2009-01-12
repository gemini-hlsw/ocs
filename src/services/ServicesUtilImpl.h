#ifndef SERVICESUTILIMPL_H_
#define SERVICESUTILIMPL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>

#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>

#include <services/RequestProducer.h>

namespace giapi {

class ServicesUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:

	/**
	 * Get the singleton instance of the ServiceUtil implementation
	 */
	static ServicesUtilImpl& Instance();

	void systemLog(log::Level level, const char *msg);

	long64 getObservatoryTime();

	/**
	 * Returns the property value for the given key. If there
	 * are no value associated to that key, an empty string
	 * is returned.
	 */
	const char * getProperty(const char *key) throw (CommunicationException);

	/**
	 * Destructor
	 */
	virtual ~ServicesUtilImpl();

private:
	static std::auto_ptr<ServicesUtilImpl> INSTANCE;

	/**
	 * Private constructor
	 */
	ServicesUtilImpl();

	/**
	 * Smart pointer to the RequestProducer object
	 */
	pRequestProducer _producer;
	
	/**
	 * Accessor to the request producer which is 
	 * lazily initialized
	 */
	pRequestProducer getRequestProducer() throw (CommunicationException);
};

}

#endif /*SERVICESUTILIMPL_H_*/
